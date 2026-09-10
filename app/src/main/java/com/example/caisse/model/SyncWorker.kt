package com.example.caisse.model

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.caisse.R
import com.example.caisse.data.CaisseDataBase
import com.example.caisse.data.LegacyIds
import com.example.caisse.data.Produit
import com.example.caisse.data.TypeEvenement
import com.example.caisse.data.Vente
import com.example.caisse.data.VenteLigne
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // Plus grand horodatage SERVEUR (FieldValue.serverTimestamp) réellement vu pendant le PULL.
    // Base de temps partagée par TOUS les appareils -> insensible aux décalages d'horloge locale.
    private var maxSeenServerMillis = 0L
    private fun trackServer(doc: DocumentSnapshot) {
        val ms = doc.getTimestamp("serverUpdatedAt")?.toDate()?.time ?: return
        if (ms > maxSeenServerMillis) maxSeenServerMillis = ms
    }

    // Champ d'horodatage serveur ajouté à chaque écriture (push). Sert de base au curseur de PULL.
    private fun serverStamp(): Map<String, Any> =
        mapOf("serverUpdatedAt" to FieldValue.serverTimestamp())

    override suspend fun doWork(): Result {
        val user = com.google.firebase.Firebase.auth.currentUser ?: return Result.success()
        val uid = user.uid
        Log.w("SyncWorker", "Démarrage sync uid=$uid email=${user.email}")

        // DB locale
        val dbLocal = CaisseDataBase.getDatabase(applicationContext)
        val produitDao = dbLocal.produitDao()
        val venteDao = dbLocal.venteDao()
        val venteLigneDao = dbLocal.venteDao()
        val categorieDao = dbLocal.categorieDao()
        val vendeurDao = dbLocal.vendeurDao()
        val infosDao = dbLocal.infosDao()
        val logDao = dbLocal.logDao()
        val clotureDao = dbLocal.clotureDao()
        val prefs = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)

        // Curseur basé sur l'horodatage SERVEUR (millis). Clé distincte de l'ancien "lastSyncAt"
        // (qui était basé sur l'horloge locale) pour forcer une migration propre.
        val lastServerSyncMillis = prefs.getLong("lastServerSyncAt", 0L)

        // Migration : la v2 introduit l'horodatage serveur ; la v3 force une nouvelle sync complète
        // pour réparer les tablettes restées "coincées" avec des données partielles (un document
        // produit malformé interrompait l'import avant le correctif par-document). Au premier run
        // d'une nouvelle version de schéma, on force une sync complète (full push + full pull).
        // v6 : migration des ids Vendeur/LogTechnique vers UUID -> full resync pour republier
        // toutes les données sous leurs nouveaux identifiants (les anciens docs à id numérique
        // restent lisibles grâce au mapping déterministe LegacyIds).
        val SYNC_SCHEMA_VERSION = 6
        val syncSchemaVersion = prefs.getInt("syncSchemaVersion", 1)
        val needsBackfill = syncSchemaVersion < SYNC_SCHEMA_VERSION

        // Garde-fou anti-incohérence : si la base locale est vide alors que le curseur est "avancé",
        // c'est que la base Room a été vidée (réinstallation, migration destructive, clear data...)
        // sans réinitialiser le curseur. Une sync incrémentale ne récupérerait alors RIEN.
        val localEmpty = produitDao.getAllProduitsOnce().isEmpty() &&
                venteDao.getAllVentesOnce().isEmpty()

        // Sync complète si : premier run, base locale vide, ou migration vers l'horodatage serveur.
        val isInitialSync = lastServerSyncMillis == 0L || localEmpty || needsBackfill
        if (isInitialSync) {
            Log.w("SyncWorker", "Sync complète forcée (lastServer=$lastServerSyncMillis, vide=$localEmpty, backfill=$needsBackfill)")
        }

        // En sync incrémentale, on ne récupère que les docs estampillés APRÈS le curseur serveur.
        // whereGreaterThanOrEqualTo (et pas strict) : on ré-importe le doc à la frontière, c'est
        // idempotent (upsert + comparaison updatedAt), au cas où plusieurs écritures partagent la
        // même milliseconde serveur.
        val sinceTs: Timestamp? = if (isInitialSync) null else Timestamp(Date(lastServerSyncMillis))

        val cloud = FirebaseFirestore.getInstance()

        // Exécute une étape en isolant les erreurs : une collection qui échoue (réseau, règles
        // Firestore, doc malformé...) ne doit pas empêcher les autres de se synchroniser.
        var hadError = false
        suspend fun step(name: String, block: suspend () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                hadError = true
                Log.e("SyncWorker", "Échec étape '$name' : ${e.javaClass.simpleName} ${e.message}", e)
            }
        }

        // 0) Clôture automatique des caisses vendeur oubliées ouvertes, avant le push pour que
        // la fermeture soit propagée aux autres appareils dans le même passage.
        step("clôture auto caisses") { autoCloseCaissesOubliees(dbLocal.sessionCaisseDao(), infosDao, logDao) }

        // 1) PUSH : envoyer ce qui est dirty (Produit, Vente, VenteLigne)
        step("push produits") { pushDirtyProduits(cloud, uid, produitDao, isInitialSync) }
        step("push categories") { pushDirtyCategories(cloud, uid, categorieDao, isInitialSync) }
        step("push ventes") { pushDirtyVentes(cloud, uid, venteDao, isInitialSync) }
        step("push venteLignes") { pushDirtyVenteLignes(cloud, uid, venteDao, isInitialSync) }
        step("push vendeurs") { pushDirtyVendeurs(cloud, uid, vendeurDao) }
        step("push sessions") { pushDirtySessions(cloud, uid, dbLocal.sessionCaisseDao()) }
        step("push infos") { pushInfos(cloud, uid, infosDao) }
        step("push tables") { pushDirtyTables(cloud, uid, dbLocal.tableDao(), isInitialSync) }
        step("push table_items") { pushDirtyTableItems(cloud, uid, dbLocal.tableDao(), isInitialSync) }
        step("push clotures") { pushDirtyClotures(cloud, uid, clotureDao, isInitialSync) }
        step("push logs") { pushDirtyLogs(cloud, uid, logDao, isInitialSync) }

        // 2) PULL : récupérer ce qui a changé depuis le curseur serveur
        step("pull categories") { pullCategoriesSince(cloud, uid, sinceTs, categorieDao) }
        step("pull produits") { pullProduitsSince(cloud, uid, sinceTs, produitDao) }
        step("pull tables") { pullTablesSince(cloud, uid, sinceTs, dbLocal.tableDao()) }
        step("pull table_items") { pullTableItemsSince(cloud, uid, sinceTs, dbLocal.tableDao(), produitDao) }
        step("pull vendeurs") { pullVendeursSince(cloud, uid, sinceTs, vendeurDao) }
        step("pull sessions") { pullSessionsSince(cloud, uid, sinceTs, dbLocal.sessionCaisseDao()) }
        step("pull ventes") { pullVentesSince(cloud, uid, sinceTs, venteDao) }
        step("pull venteLignes") { pullVenteLignesSince(cloud, uid, sinceTs, venteDao, produitDao) }
        step("pull infos") { pullInfos(cloud, uid, infosDao) }
        step("pull clotures") { pullCloturesSince(cloud, uid, sinceTs, clotureDao) }
        step("pull logs") { pullLogsSince(cloud, uid, sinceTs, logDao) }

        val nbProduits = produitDao.getAllProduitsOnce().size
        val nbVentes = venteDao.getAllVentesOnce().size
        Log.w("SyncWorker", "Sync terminé : produits=$nbProduits ventes=$nbVentes erreur=$hadError")

        // 3) MAJ du curseur de sync — UNIQUEMENT si AUCUNE étape n'a échoué.
        // Sinon on garde l'ancien curseur : les documents non récupérés (réseau, etc.) seront
        // re-balayés au prochain run au lieu d'être sautés définitivement (cause de divergence
        // entre appareils). La version de schéma n'est validée qu'après une sync complète réussie.
        if (!hadError) {
            val edit = prefs.edit()
            if (maxSeenServerMillis > 0L) {
                // Marge de recouvrement : on re-balaye les dernières minutes pour rattraper les
                // écritures dont la visibilité serveur a pu arriver juste après notre requête
                // (ré-import idempotent grâce aux upserts + résolution par isDirty).
                val overlapMs = 5 * 60 * 1000L
                edit.putLong("lastServerSyncAt", (maxSeenServerMillis - overlapMs).coerceAtLeast(0L))
            }
            edit.putInt("syncSchemaVersion", SYNC_SCHEMA_VERSION)
            edit.apply()
        } else {
            Log.w("SyncWorker", "Curseur NON avancé (une étape a échoué) -> nouvel essai au prochain run")
        }

        // Statut renvoyé à l'UI (spinner + toast du bouton "Synchroniser").
        val output = androidx.work.workDataOf(
            "ok" to !hadError,
            "produits" to nbProduits,
            "ventes" to nbVentes
        )
        return Result.success(output)
    }

    // ---------------- PUSH ----------------
    private suspend fun pushDirtyTables(
        cloud: FirebaseFirestore,
        uid: String,
        tableDao: TableDao,
        isInitialSync: Boolean
    ) {
        val all = tableDao.getAllTablesOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }
        for (t in list) {
            cloud.collection("users").document(uid)
                .collection("tables").document(t.id.toString())
                .set(tableToMap(t.copy(isDirty = false)) + serverStamp()).await()
            // Baisse conditionnelle du flag (et pas réécriture de l'objet lu) : ne pas écraser
            // une modification locale survenue pendant le push.
            tableDao.clearTableDirty(t.id, t.updatedAt)
        }
    }
    private suspend fun pushDirtyTableItems(
        cloud: FirebaseFirestore,
        uid: String,
        tableDao: TableDao,
        isInitialSync: Boolean
    ) {
        val all = tableDao.getAllTableItemsOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }
        for (ti in list) {
            cloud.collection("users").document(uid)
                .collection("table_items").document(ti.id.toString())
                .set(tableItemToMap(ti.copy(isDirty = false)) + serverStamp()).await()
            tableDao.clearTableItemDirty(ti.id, ti.updatedAt)
        }
    }
    private suspend fun pushDirtyProduits(
        cloud: FirebaseFirestore,
        uid: String,
        produitDao: com.example.caisse.model.ProduitDao,
        isInitialSync: Boolean
    ) {
        // Inclure les produits soft-deletés pour propager les suppressions UNITAIRES (softDeleteProduit
        // met isDirty=1). La suppression de MASSE ("Effacer toutes les données") ne met PAS isDirty,
        // donc elle ne remonte pas : aucun risque de vider les autres appareils.
        // ⚠️ Au backfill, on pousse les actifs + les dirty, mais JAMAIS un soft-delete de masse
        // (isDeleted=1 && isDirty=0), sinon le backfill re-propagerait un wipe.
        val all = produitDao.getAllProduitsForSync()
        val list = if (isInitialSync) all.filter { !it.isDeleted || it.isDirty } else all.filter { it.isDirty }
        for (p in list) {
            cloud.collection("users").document(uid)
                .collection("produits").document(p.id.toString())
                .set(produitToMap(p.copy(isDirty = false)) + serverStamp())
                .await()
            produitDao.clearDirty(p.id, p.updatedAt)
        }
    }

    private suspend fun pushDirtyVentes(
        cloud: FirebaseFirestore,
        uid: String,
        venteDao: VenteDao,
        isInitialSync: Boolean
    ) {
        // Au backfill, pousser TOUTES les ventes (comme les venteLignes) : sinon une vente locale
        // absente du cloud mais non-dirty laisse ses lignes orphelines côté cloud, et les autres
        // appareils les sautent définitivement ("Skip VL sans parent").
        val all = venteDao.getAllVentesOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }
        for (v in list) {
            cloud.collection("users").document(uid)
                .collection("ventes").document(v.id.toString())
                .set(venteToMap(v.copy(isDirty = false)) + serverStamp())
                .await()
            venteDao.clearVenteDirty(v.id, v.updatedAt)
        }
    }

    private suspend fun pushDirtyVenteLignes(
        cloud: FirebaseFirestore,
        uid: String,
        venteDao: com.example.caisse.model.VenteDao,
        isInitialSync: Boolean
    ) {
        val all = venteDao.getAllVenteLignesOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }
        for (vl in list) {
            cloud.collection("users").document(uid)
                .collection("venteLignes").document(vl.id.toString())
                .set(venteLigneToMap(vl.copy(isDirty = false)) + serverStamp())
                .await()
            venteDao.clearLigneDirty(vl.id, vl.updatedAt)
        }
    }

    private suspend fun pushDirtyCategories(
        cloud: FirebaseFirestore,
        uid: String,
        categorieDao: com.example.caisse.model.CategorieDao,
        isInitialSync: Boolean
    ){
        // Comme les produits : on inclut les catégories soft-deletées pour propager les suppressions
        // unitaires ; la suppression de masse (isDirty=0) ne remonte pas (même garde-fou au backfill).
        val all = categorieDao.getAllCategoriesForSync()
        val list = if (isInitialSync) all.filter { !it.isDeleted || it.isDirty } else all.filter { it.isDirty }
        for (c in list){
            cloud.collection("users").document(uid)
                .collection("categories").document(c.id.toString())
                .set(categorieToMap(c.copy(isDirty = false)) + serverStamp())
                .await()
            categorieDao.clearDirty(c.id, c.updatedAt)
        }
    }


    private suspend fun pushDirtyVendeurs(
        cloud: FirebaseFirestore,
        uid: String,
        vendeurDao: VendeurDao
    ) {
        // inclure les vendeurs supprimés (soft-delete) pour propager la suppression aux autres appareils
        val list = vendeurDao.getAllVendeursForSync().filter { it.isDirty }
        for (v in list) {
            cloud.collection("users").document(uid)
                .collection("vendeurs").document(v.id.toString())
                .set(vendeurToMap(v.copy(isDirty = false)) + serverStamp())
                .await()
            vendeurDao.clearDirty(v.id, v.updatedAt)
        }
    }

    /**
     * Clôture automatique : une caisse vendeur oubliée ouverte (jamais fermée manuellement)
     * empoisonne le rattachement des ventes au jour métier (voir bucketVentesParJourMetier côté
     * dashboard) — toute vente ultérieure de ce vendeur retombe sur ce très ancien jour métier.
     * Dès que l'heure de clôture configurée (5h00 par défaut, réglable par le gérant) est passée,
     * on force la fermeture de toute session encore ouverte depuis avant cette heure aujourd'hui.
     */
    private suspend fun autoCloseCaissesOubliees(
        sessionCaisseDao: SessionCaisseDao,
        infosDao: com.example.caisse.model.InfosDao,
        logDao: LogDao
    ) {
        val heureCloture = infosDao.getInfos()?.heureClotureAuto ?: 5
        val zone = java.time.ZoneId.systemDefault()
        val cutoffToday = java.time.LocalDate.now(zone)
            .atTime(heureCloture.coerceIn(0, 23), 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        if (System.currentTimeMillis() < cutoffToday) return

        val ouvertes = sessionCaisseDao.getOpenSessionsOnce()
        for (session in ouvertes) {
            if (session.dateOuverture >= cutoffToday) continue // ouverte après l'heure de clôture d'aujourd'hui : encore valide

            sessionCaisseDao.closeSessionAt(session.id, cutoffToday)

            val date = java.time.LocalDateTime.now().toString()
            val description = "Fermeture automatique de la caisse (heure de clôture ${heureCloture}h00) | vendeur=${session.idVendeurOuverture}"
            val contenu = "$date|${TypeEvenement.FERMETURE_SESSION.name}|$description|${session.idVendeurOuverture ?: ""}"
            val empreinte = com.example.caisse.util.FiscalHashUtils.sha256(contenu)
            logDao.insertLog(
                com.example.caisse.data.LogTechnique(
                    date = date,
                    typeEvenement = TypeEvenement.FERMETURE_SESSION.name,
                    description = description,
                    idVendeur = session.idVendeurOuverture,
                    empreinte = empreinte
                )
            )
        }
    }

    private suspend fun pushDirtySessions(
        cloud: FirebaseFirestore,
        uid: String,
        sessionCaisseDao: SessionCaisseDao
    ) {
        val list = sessionCaisseDao.getDirtySessions()
        for (s in list) {
            cloud.collection("users").document(uid)
                .collection("sessions_caisse").document(s.id.toString())
                .set(sessionToMap(s.copy(isDirty = false)) + serverStamp())
                .await()
            sessionCaisseDao.clearDirty(s.id, s.updatedAt)
        }
    }

    private suspend fun  pushInfos(
        cloud: FirebaseFirestore,
        uid: String,
        infosDao: com.example.caisse.model.InfosDao
    ){
        val infos = infosDao.getInfos()
        Log.w("SyncWorker", "Push infos : local=${infos != null} dirty=${infos?.isDirty}")
        // Ne pousser que si une modification locale est en attente (évite d'écraser le cloud à
        // chaque sync et de clobber une édition faite sur un autre appareil).
        if (infos != null && infos.isDirty) {
            cloud.collection("users").document(uid)
                .collection("infos").document("1")
                .set(infosToMap(infos) + serverStamp())
                .await()
            infosDao.clearDirty(infos.updatedAt)
        }


    }

    private suspend fun pushDirtyClotures(
        cloud: FirebaseFirestore,
        uid: String,
        clotureDao: ClotureDao,
        isInitialSync: Boolean
    ) {
        val all = clotureDao.getAllCloturesOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }

        for (c in list) {
            cloud.collection("users").document(uid)
                .collection("clotures").document(c.idCloture.toString())
                .set(clotureToMap(c.copy(isDirty = false)) + serverStamp())
                .await()

            clotureDao.markSynced(c.idCloture)
        }
    }


    private suspend fun pushDirtyLogs(
        cloud: FirebaseFirestore,
        uid: String,
        logDao: LogDao,
        isInitialSync: Boolean
    ) {
        val all = logDao.getAllLogsOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }

        for (log in list) {
            cloud.collection("users").document(uid)
                .collection("logs_techniques").document(log.id.toString())
                .set(logToMap(log.copy(isDirty = false)) + serverStamp())
                .await()

            logDao.markSynced(log.id)
        }
    }



    // ---------------- PULL ----------------
    // Helper commun : sélectionne tout (sinceTs == null) ou ce qui a changé depuis le curseur
    // serveur. Paginé (500 docs par page) pour qu'une sync initiale sur un gros historique ne
    // charge pas toute la collection en mémoire et ne dépasse pas la limite de temps du worker.
    private suspend fun pullSnapshot(
        cloud: FirebaseFirestore,
        uid: String,
        collection: String,
        sinceTs: Timestamp?
    ): List<DocumentSnapshot> {
        val base = cloud.collection("users").document(uid).collection(collection)
        val pageSize = 500L
        val docs = mutableListOf<DocumentSnapshot>()
        var last: DocumentSnapshot? = null
        while (true) {
            var q = if (sinceTs == null)
                base.orderBy(FieldPath.documentId()).limit(pageSize)
            else
                base.whereGreaterThanOrEqualTo("serverUpdatedAt", sinceTs)
                    .orderBy("serverUpdatedAt").limit(pageSize)
            last?.let { q = q.startAfter(it) }
            val page = q.get().await()
            docs += page.documents
            if (page.size() < pageSize) break
            last = page.documents.last()
        }
        return docs
    }

    private suspend fun pullTablesSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        tableDao: TableDao
    ) {
        val snap = pullSnapshot(cloud, uid, "tables", sinceTs)
        for (doc in snap) {
            trackServer(doc)
            try {
                val m = doc.data ?: continue
                val remote = mapToTable(m)
                val local = tableDao.getTableById(remote.id)
                if (local == null) {
                    tableDao.upsertTable(remote.copy(isDirty = false))   // INSERT (pas de conflit)
                } else if (!local.isDirty) {
                    // Conflit résolu par isDirty (indépendant de l'horloge locale).
                    // @Update et pas upsertTable (REPLACE) : éviter le CASCADE sur les table_items.
                    tableDao.updateTable(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Table invalide doc=${doc.id}: ${e.javaClass.simpleName} ${e.message}")
            }
        }
    }
    private suspend fun pullTableItemsSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        tableDao: TableDao,
        produitDao: ProduitDao
    ) {
        val snap = pullSnapshot(cloud, uid, "table_items", sinceTs)
        for (doc in snap) {
            trackServer(doc)
            try {
                val m = doc.data ?: continue
                val ti = mapToTableItem(m)

                // Conflit résolu par isDirty : ne pas écraser une ligne de commande locale non encore
                // poussée (ex. un article ajouté à la table dont le push a échoué).
                val localTi = tableDao.getTableItemById(ti.id)
                if (localTi != null && localTi.isDirty) continue

                // s'assurer que le produit parent existe localement
                if (produitDao.getProduitById(ti.productId) == null) {
                    val prodDoc = cloud.collection("users").document(uid)
                        .collection("produits").document(ti.productId.toString()).get().await()
                    if (prodDoc.exists()) {
                        val p = mapToProduit(prodDoc.data!!)
                        produitDao.insertProduit(p.copy(isDirty = false))
                    } else {
                        continue
                    }
                }

                tableDao.upsertTableItem(ti.copy(isDirty = false))
            } catch (e: Exception) {
                Log.e("SyncWorker", "TableItem invalide doc=${doc.id}: ${e.javaClass.simpleName} ${e.message}")
            }
        }
    }
    private suspend fun pullProduitsSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        produitDao: com.example.caisse.model.ProduitDao
    ) {
        val snap = pullSnapshot(cloud, uid, "produits", sinceTs)
        var imported = 0
        for (doc in snap) {
            trackServer(doc)
            try {
                val data = doc.data ?: continue
                val remote = mapToProduit(data)
                val local = produitDao.getProduitById(remote.id)
                if (local == null) {
                    produitDao.insertProduit(remote.copy(isDirty = false))   // INSERT
                } else if (!local.isDirty) {
                    // Conflit résolu par isDirty (PAS par updatedAt, qui dépend de l'horloge locale
                    // de chaque appareil) : si aucune modif locale n'est en attente, le cloud fait foi.
                    // @Update (jamais REPLACE) : applique aussi isDeleted=true le cas échéant -> propage
                    // la suppression SANS supprimer physiquement la ligne (pas de CASCADE sur les enfants).
                    produitDao.updateProduit(remote.copy(isDirty = false))
                }
                imported++
            } catch (e: Exception) {
                // Un document produit malformé ne doit pas interrompre l'import des autres produits.
                Log.e("SyncWorker", "Produit invalide doc=${doc.id}: ${e.javaClass.simpleName} ${e.message}")
            }
        }
        Log.w("SyncWorker", "Pull produits : reçus=${snap.size} importés=$imported")
    }

    private suspend fun pullInfos(
        cloud: FirebaseFirestore,
        uid: String,
        infosDao: com.example.caisse.model.InfosDao
    ) {
        val doc = cloud.collection("users").document(uid)
            .collection("infos").document("1")
            .get().await()
        if (!doc.exists()) {
            // Cas typique "nouvel appareil sans fiche" : le doc n'a jamais été poussé depuis
            // l'appareil source (fiche jamais ré-enregistrée depuis le correctif isDirty).
            Log.w("SyncWorker", "Pull infos : aucun document infos/1 dans le cloud pour ce compte")
            return
        }
        trackServer(doc)
        val data = doc.data ?: return
        val remote = mapToInfos(data).copy(id = 1)  // sécurité : force id=1
        val local = infosDao.getInfos()

        if (local == null) {
            infosDao.insertInfos(remote)   // insert
            Log.w("SyncWorker", "Pull infos : fiche magasin importée (${remote.name})")
        } else if (!local.isDirty) {
            // Conflit résolu par isDirty : ne pas écraser une édition locale en attente.
            infosDao.updateInfos(remote)
            Log.w("SyncWorker", "Pull infos : fiche magasin mise à jour (${remote.name})")
        } else {
            Log.w("SyncWorker", "Pull infos : édition locale en attente, cloud ignoré")
        }
    }


    private suspend fun pullCategoriesSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        categorieDao: com.example.caisse.model.CategorieDao
    ){
        val snap = pullSnapshot(cloud, uid, "categories", sinceTs)
        var imported = 0
        for (doc in snap) {
            trackServer(doc)
            try {
                val data = doc.data ?: continue
                val remote = mapToCategorie(data)
                val local = categorieDao.get(remote.id)
                if (local == null) {
                    // INSERT pur (pas de conflit -> pas de REPLACE -> pas de CASCADE)
                    categorieDao.addCategory(remote.copy(isDirty = false))
                } else if (!local.isDirty) {
                    // Conflit résolu par isDirty (indépendant de l'horloge locale).
                    // ⚠️ @Update (modification en place) et SURTOUT PAS addCategory (REPLACE) : un REPLACE
                    // supprimerait la ligne catégorie et CASCADE supprimerait tous ses produits enfants.
                    // L'@Update applique aussi isDeleted=true -> propage la suppression sans cascade.
                    categorieDao.updateCategory(remote.copy(isDirty = false))
                }
                imported++
            } catch (e: Exception) {
                Log.e("SyncWorker", "Categorie invalide doc=${doc.id}: ${e.javaClass.simpleName} ${e.message}")
            }
        }
        Log.w("SyncWorker", "Pull categories : reçus=${snap.size} importés=$imported")
    }


    private suspend fun pullVentesSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        venteDao: com.example.caisse.model.VenteDao
    ) {
        val snap = pullSnapshot(cloud, uid, "ventes", sinceTs)
        Log.d("SyncWorker", "nombre de vente  : ${snap.size}")

        for (doc in snap) {
            trackServer(doc)
            val data = doc.data ?: continue

            try {
                val remote = mapToVente(data)
                val local = venteDao.getVenteById(remote.id)

                if (local == null) {
                    venteDao.insertVente(remote.copy(isDirty = false))   // INSERT
                } else if (!local.isDirty) {
                    // Conflit résolu par isDirty (indépendant de l'horloge locale).
                    venteDao.updateVente(remote.copy(isDirty = false))
                }

            } catch (e: Exception) {
                Log.d("SyncWorker", "Erreur catch  : ${e.message} Vente invalide doc=${doc.id} ")
                continue
            }
        }
    }

    private suspend fun pullVenteLignesSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        venteDao: VenteDao,
        produitDao: ProduitDao
    ) {
        val snap = pullSnapshot(cloud, uid, "venteLignes", sinceTs)

        for (doc in snap) {
            trackServer(doc)
            val data = doc.data ?: continue
            try {
                val remote = mapToVenteLigne(data)

                // 1) s'assurer que la Vente parente existe
                if (venteDao.getVenteById(remote.venteId) == null) {
                    val venteDoc = cloud.collection("users").document(uid)
                        .collection("ventes").document(remote.venteId.toString())
                        .get().await()
                    if (venteDoc.exists()) {
                        val vente = mapToVente(venteDoc.data!!)
                        venteDao.insertVente(vente.copy(isDirty = false)) // upsert
                    } else {
                        // parent introuvable => on ne peut pas insérer cette ligne
                        Log.w("SyncWorker", "Skip VL sans parent Vente=${remote.venteId}")
                        continue
                    }
                }

                // 2) s'assurer que le Produit parent existe
                if (produitDao.getProduitById(remote.produitId) == null) {
                    val prodDoc = cloud.collection("users").document(uid)
                        .collection("produits").document(remote.produitId.toString())
                        .get().await()
                    if (prodDoc.exists()) {
                        val prod = mapToProduit(prodDoc.data!!)
                        produitDao.insertProduit(prod.copy(isDirty = false)) // upsert
                    } else {
                        Log.w("SyncWorker", "Skip VL sans parent Produit=${remote.produitId}")
                        continue
                    }
                }

                // 3) enfin, upsert de la ligne (conflit résolu par isDirty, indépendant de l'horloge).
                // insertLigne est en ABORT : sur une ligne déjà présente (ré-import de la fenêtre
                // de recouvrement), il faut passer par @Update sinon l'insert lève une exception.
                val local = venteDao.getVenteLigneById(remote.id)
                if (local == null) {
                    venteDao.insertLigne(remote.copy(isDirty = false))
                } else if (!local.isDirty) {
                    venteDao.updateLigne(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "VL invalide doc=${doc.id}: ${e.message}")
                continue
            }
        }
    }

    private suspend fun pullVendeursSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        vendeurDao: com.example.caisse.model.VendeurDao
    ) {
        val snap = pullSnapshot(cloud, uid, "vendeurs", sinceTs)

        for (doc in snap) {
            trackServer(doc)
            try {
                val data = doc.data ?: continue
                val remote = mapToVendeur(data)
                val local = vendeurDao.getVendeurById(remote.id)
                if (local == null) {
                    vendeurDao.insertVendeur(remote.copy(isDirty = false))   // INSERT (sinon @Update = no-op sur nouvel appareil)
                } else if (!local.isDirty) {
                    // Conflit résolu par isDirty (indépendant de l'horloge locale).
                    vendeurDao.updateVendeur(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Vendeur invalide doc=${doc.id}: ${e.javaClass.simpleName} ${e.message}")
            }
        }
    }

    private suspend fun pullSessionsSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        sessionCaisseDao: SessionCaisseDao
    ) {
        val snap = pullSnapshot(cloud, uid, "sessions_caisse", sinceTs)

        for (doc in snap) {
            trackServer(doc)
            try {
                val data = doc.data ?: continue
                val remote = mapToSession(data)
                val local = sessionCaisseDao.getSessionById(remote.id)
                if (local == null || !local.isDirty) {
                    // upsert : une session distante fermée (dateFermeture) doit remplacer la
                    // version locale encore ouverte, et inversement si la locale est déjà dirty.
                    sessionCaisseDao.upsert(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Session caisse invalide doc=${doc.id}: ${e.javaClass.simpleName} ${e.message}")
            }
        }
    }

    private suspend fun pullCloturesSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        clotureDao: ClotureDao
    ) {
        val snap = pullSnapshot(cloud, uid, "clotures", sinceTs)

        for (doc in snap) {
            trackServer(doc)
            val data = doc.data ?: continue
            try {
                val remote = mapToCloture(data)
                val local = clotureDao.getClotureById(remote.idCloture)

                // NF525 Axe A : une clôture locale n'est jamais écrasée par une version distante
                if (local == null) {
                    clotureDao.insertCloture(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Cloture invalide doc=${doc.id}: ${e.message}")
            }
        }
    }


    private suspend fun pullLogsSince(
        cloud: FirebaseFirestore,
        uid: String,
        sinceTs: Timestamp?,
        logDao: LogDao
    ) {
        val snap = pullSnapshot(cloud, uid, "logs_techniques", sinceTs)

        for (doc in snap) {
            trackServer(doc)
            val data = doc.data ?: continue
            try {
                val remote = mapToLog(data)
                val local = logDao.getLogById(remote.id)

                // NF525 Axe A : un log local (JET) n'est jamais écrasé par une version distante
                if (local == null) {
                    logDao.insertLog(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Log invalide doc=${doc.id}: ${e.message}")
            }
        }
    }


    // --------------- MAPPERS (copiés depuis repo) ---------------

    private fun tableToMap(t: com.example.caisse.data.AppTable) = mapOf(
        "id" to t.id.toString(),
        "name" to t.name,
        "active" to t.active,
        "updatedAt" to t.updatedAt,
        "isDeleted" to t.isDeleted
    )
    private fun tableItemToMap(ti: com.example.caisse.data.TableItem) = mapOf(
        "id" to ti.id.toString(),
        "tableId" to ti.tableId.toString(),
        "productId" to ti.productId.toString(),
        "quantity" to ti.quantity,
        "updatedAt" to ti.updatedAt,
        "isDirty" to ti.isDirty,
        "isDeleted" to ti.isDeleted
    )
    private fun mapToTable(m: Map<String, Any?>) = com.example.caisse.data.AppTable(
        id = UUID.fromString(m["id"] as String),
        name = m["name"] as String,
        active = (m["active"] as? Boolean) ?: true,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = (m["isDeleted"] as? Boolean) ?: false
    )

    private fun mapToTableItem(m: Map<String, Any?>) = com.example.caisse.data.TableItem(
        id = UUID.fromString(m["id"] as String),
        tableId = UUID.fromString(m["tableId"] as String),
        productId = UUID.fromString(m["productId"] as String),
        quantity = (m["quantity"] as? Number)?.toInt() ?: 0,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = (m["isDeleted"] as? Boolean) ?: false
    )
    private fun produitToMap(p: Produit) = mapOf(
        "id" to p.id.toString(),
        "nom" to p.nom,
        "prix" to p.prix,
        "image" to p.image,
        "categoryId" to p.categoryId.toString(),
        "stock" to p.stock,
        "description" to p.description,
        "isActive" to p.isActive,
        "updatedAt" to p.updatedAt,
        "isDeleted" to p.isDeleted
    )

    private fun categorieToMap(c: com.example.caisse.data.Category) = mapOf(
        "id" to c.id.toString(),
        "name" to c.name,
        "description" to c.description,
        "icon" to c.icon,
        "updatedAt" to c.updatedAt,
        "isDeleted" to c.isDeleted
    )

    private fun infosToMap(i: com.example.caisse.data.ShopInfos) = mapOf(
        "id" to i.id,
        "name" to i.name,
        "address" to i.address,
        "phone" to i.phone,
        "email" to i.email,
        "siret" to i.siret,
        "logo" to i.logo,
        "passwordHash" to i.passwordHash,
        "passwordSalt" to i.passwordSalt,
        "devise" to i.devise,
        "heureClotureAuto" to i.heureClotureAuto,
        "updatedAt" to i.updatedAt,
        "isDeleted" to i.isDeleted
        )

    // Casts tolérants : un document écrit par une ancienne version de l'app (champ manquant,
    // ex. siret) ne doit pas faire échouer TOUT le pull infos — c'était une cause de fiche
    // magasin jamais importée sur les nouveaux appareils.
    private fun mapToInfos(m: Map<String, Any?>) = com.example.caisse.data.ShopInfos(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        name = m["name"] as? String ?: "",
        address = m["address"] as? String ?: "",
        phone = m["phone"] as? String ?: "",
        email = m["email"] as? String ?: "",
        siret = m["siret"] as? String ?: "",
        logo = (m["logo"] as? Number)?.toInt(),
        passwordHash =
            (m["passwordHash"] as? String)
                ?: (m["password"] as? String)      // ancien format cloud
                ?: "",

        passwordSalt =
            (m["passwordSalt"] as? String)
                ?: "",

        devise = m["devise"] as? String ?: "FCFA",
        heureClotureAuto = (m["heureClotureAuto"] as? Number)?.toInt() ?: 5,

        // si tu as ces champs dans ShopInfos
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = (m["isDeleted"] as? Boolean) ?: false
    )




    private fun mapToCategorie(m: Map<String, Any?>) = com.example.caisse.data.Category(
        id = UUID.fromString(m["id"] as String),
        name = m["name"] as String,
        description = m["description"] as String?,
        icon = m["icon"] as? Int,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false,
    )




    private fun mapToProduit(m: Map<String, Any?>): Produit {
        val image: String? = when (val img = m["image"]) {
            is String -> img              // ✅ nouveau format (URI)
            is Number -> null
            else -> null
        }

        return Produit(
            id = UUID.fromString(m["id"] as String),
            nom = m["nom"] as String,
            prix = (m["prix"] as Number).toDouble(),
            image = image,               // 🔥 on met l’image nettoyée !
            categoryId = UUID.fromString(m["categoryId"] as String),
            stock = (m["stock"] as Number).toInt(),
            description = m["description"] as String?,
            isActive = m["isActive"] as? Boolean ?: true,
            updatedAt = (m["updatedAt"] as Number?)?.toLong() ?: System.currentTimeMillis(),
            isDirty = false,
            isDeleted = m["isDeleted"] as? Boolean ?: false
        )
    }

    private fun venteToMap(v: Vente) = mapOf(
        "id" to v.id.toString(),
        "date" to v.date,
        "vendeurId" to v.vendeurId?.toString(),
        "total" to v.total,
        "tableId" to v.tableId?.toString(),
        // NF525 : numéro de séquence synchronisé pour être visible sur tous les appareils
        "sequenceNumber" to v.sequenceNumber,
        "updatedAt" to v.updatedAt,
        "isDirty" to v.isDirty,
        "isDeleted" to v.isDeleted,
        "hash" to v.hash,
        "previousHash" to v.previousHash,
    )

    // Ancien format cloud : vendeurId était un entier local -> converti via le même mapping
    // déterministe que la migration Room (LegacyIds), pour pointer sur le bon vendeur migré.
    private fun parseVendeurId(raw: Any?): UUID? = when (raw) {
        is Number -> LegacyIds.vendeurUuid(raw.toInt())
        is String -> parseUuidOrNull(raw)
            ?: raw.toIntOrNull()?.let { LegacyIds.vendeurUuid(it) }
        else -> null
    }

    private fun mapToVente(m: Map<String, Any?>): Vente {

        val id = parseUuidOrNull(getString(m, "id"))
            ?: throw IllegalArgumentException("vente.id invalide")
        val date = getNumberAsLong(m, "date") ?: System.currentTimeMillis()
        val vendeurId = parseVendeurId(m["vendeurId"])
        val total = getNumberAsDouble(m, "total") ?: 0.0
        val updatedAt = getNumberAsLong(m, "updatedAt") ?: System.currentTimeMillis()
        val tableId = parseUuidOrNull(getString(m, "tableId"))

        return Vente(
            id = id,
            date = date,
            vendeurId = vendeurId,
            total = total,
            sequenceNumber = getNumberAsLong(m, "sequenceNumber") ?: 0L,
            updatedAt = updatedAt,
            tableId = tableId,
            isDirty = (m["isDirty"] as? Boolean) ?: false,
            isDeleted = (m["isDeleted"] as? Boolean) ?: false,
            hash = getString(m, "hash") ?: "",
            previousHash = getString(m, "previousHash") ?: ""
        )
    }

    private fun venteLigneToMap(vl: VenteLigne) = mapOf(
        "id" to vl.id.toString(),
        "venteId" to vl.venteId.toString(),
        "produitId" to vl.produitId.toString(),
        "quantity" to vl.quantity,
        "prixUnitaire" to vl.prixUnitaire,
        "sousTotal" to vl.sousTotal,
        "updatedAt" to vl.updatedAt,
        "isDirty" to vl.isDirty,
        "isDeleted" to vl.isDeleted
    )

    private fun mapToVenteLigne(m: Map<String, Any?>): VenteLigne {
        return VenteLigne(
            id = UUID.fromString(m["id"] as String),
            venteId = UUID.fromString(m["venteId"] as String),
            produitId = UUID.fromString(m["produitId"] as String),
            quantity = (m["quantity"] as? Number)?.toInt() ?: 0,
            prixUnitaire = (m["prixUnitaire"] as? Number)?.toDouble() ?: 0.0,
            sousTotal = (m["sousTotal"] as? Number)?.toDouble() ?: 0.0,
            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            isDirty = false,
            isDeleted = m["isDeleted"] as? Boolean ?: false
        )
    }
    private fun vendeurToMap(v: com.example.caisse.data.Vendeur) = mapOf(
        "id" to v.id.toString(),
        "nom" to v.nom,
        "prenom" to v.prenom,
        "pinHash" to v.pinHash,
        "pinSalt" to v.pinSalt,
        "updatedAt" to v.updatedAt,
        "isDeleted" to v.isDeleted
    )

    private fun mapToVendeur(m: Map<String, Any?>) = com.example.caisse.data.Vendeur(
        // Ancien format cloud : id numérique -> même mapping déterministe que la migration Room.
        id = when (val raw = m["id"]) {
            is Number -> LegacyIds.vendeurUuid(raw.toInt())
            is String -> parseUuidOrNull(raw)
                ?: raw.toIntOrNull()?.let { LegacyIds.vendeurUuid(it) }
                ?: throw IllegalArgumentException("vendeur.id invalide")
            else -> throw IllegalArgumentException("vendeur.id manquant")
        },
        nom = m["nom"] as String,
        prenom = m["prenom"] as String,
        pinHash = m["pinHash"] as? String ?: "",
        pinSalt = m["pinSalt"] as? String ?: "",
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false
    )

    private fun sessionToMap(s: com.example.caisse.data.SessionCaisse) = mapOf(
        "id" to s.id.toString(),
        "dateOuverture" to s.dateOuverture,
        "dateFermeture" to s.dateFermeture,
        "jourMetier" to s.jourMetier,
        "idVendeurOuverture" to s.idVendeurOuverture?.toString(),
        "updatedAt" to s.updatedAt
    )

    private fun mapToSession(m: Map<String, Any?>) = com.example.caisse.data.SessionCaisse(
        id = parseUuidOrNull(m["id"] as? String) ?: throw IllegalArgumentException("session.id invalide"),
        dateOuverture = (m["dateOuverture"] as? Number)?.toLong() ?: 0L,
        dateFermeture = (m["dateFermeture"] as? Number)?.toLong(),
        jourMetier = m["jourMetier"] as? String ?: "",
        idVendeurOuverture = parseUuidOrNull(m["idVendeurOuverture"] as? String),
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false
    )

    private fun clotureToMap(c: com.example.caisse.data.Cloture) = mapOf(
        "idCloture" to c.idCloture.toString(),
        "dateCloture" to c.dateCloture,
        "type" to c.type,
        "chiffreAffaireBrut" to c.chiffreAffaireBrut,
        "totalTVA" to c.totalTVA,
        "compteurVentes" to c.compteurVentes,
        "grandTotalCumule" to c.grandTotalCumule,
        "hash" to c.hash,
        "updatedAt" to c.updatedAt,
        "isDirty" to c.isDirty,
        "isDeleted" to c.isDeleted
    )

    private fun mapToCloture(m: Map<String, Any?>) = com.example.caisse.data.Cloture(
        idCloture = UUID.fromString(m["idCloture"] as String),
        dateCloture = m["dateCloture"] as String,
        type = m["type"] as String,
        chiffreAffaireBrut = (m["chiffreAffaireBrut"] as? Number)?.toDouble() ?: 0.0,
        totalTVA = (m["totalTVA"] as? Number)?.toDouble() ?: 0.0,
        compteurVentes = (m["compteurVentes"] as? Number)?.toInt() ?: 0,
        grandTotalCumule = (m["grandTotalCumule"] as? Number)?.toDouble() ?: 0.0,
        hash = m["hash"] as? String ?: "",
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = (m["isDeleted"] as? Boolean) ?: false
    )

    private fun logToMap(log: com.example.caisse.data.LogTechnique) = mapOf(
        "id" to log.id.toString(),
        "date" to log.date,
        "typeEvenement" to log.typeEvenement,
        "description" to log.description,
        "idVendeur" to log.idVendeur?.toString(),
        "empreinte" to log.empreinte,
        "updatedAt" to log.updatedAt,
        "isDirty" to log.isDirty,
        "isDeleted" to log.isDeleted
    )

    private fun mapToLog(m: Map<String, Any?>): com.example.caisse.data.LogTechnique {
        val date = m["date"] as? String ?: ""
        val type = m["typeEvenement"] as? String ?: ""
        val empreinte = m["empreinte"] as? String ?: ""
        // Ancien format cloud : id numérique local (collisions entre appareils) -> UUID dérivé
        // du CONTENU, identique à celui produit par la migration Room sur les autres appareils.
        val id = (m["id"] as? String)?.let { parseUuidOrNull(it) }
            ?: LegacyIds.logUuid(date, type, empreinte)
        return com.example.caisse.data.LogTechnique(
        id = id,
        date = date,
        typeEvenement = type,
        description = m["description"] as? String ?: "",
        idVendeur = parseUuidOrNull(m["idVendeur"] as? String),
        empreinte = empreinte,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = (m["isDeleted"] as? Boolean) ?: false
        )
    }

    private fun getString(map: Map<String, Any?>, key: String): String? =
        (map[key] as? String)?.takeIf { it.isNotBlank() }

    private fun getNumberAsLong(map: Map<String, Any?>, key: String): Long? =
        when (val v = map[key]) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }

    private fun getNumberAsDouble(map: Map<String, Any?>, key: String): Double? =
        when (val v = map[key]) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }

    private fun parseUuidOrNull(s: String?): UUID? =
        try { s?.let(UUID::fromString) } catch (_: Exception) { null }



}
