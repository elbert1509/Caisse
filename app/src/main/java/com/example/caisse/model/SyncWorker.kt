package com.example.caisse.model

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.caisse.R
import com.example.caisse.data.CaisseDataBase
import com.example.caisse.data.Produit
import com.example.caisse.data.Vente
import com.example.caisse.data.VenteLigne
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val user = com.google.firebase.Firebase.auth.currentUser ?: return Result.success()
        val uid = user.uid

        // DB locale
        val dbLocal = CaisseDataBase.getDatabase(applicationContext)
        val produitDao = dbLocal.produitDao()
        val venteDao = dbLocal.venteDao()
        val venteLigneDao = dbLocal.venteDao()
        val categorieDao = dbLocal.categorieDao()
        val vendeurDao = dbLocal.vendeurDao()
        val infosDao = dbLocal.infosDao()
        val logDao = dbLocal.logDao()
        val prefs = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
        val since = prefs.getLong("lastSyncAt", 0L)
        val isInitialSync = since == 0L


        val cloud = FirebaseFirestore.getInstance()

        // 1) PUSH : envoyer ce qui est dirty (Produit, Vente, VenteLigne)
        pushDirtyProduits(cloud, uid, produitDao, isInitialSync)
        pushDirtyCategories(cloud, uid, categorieDao, isInitialSync)
        pushDirtyVentes(cloud, uid, venteDao)
        pushDirtyVenteLignes(cloud, uid, venteDao,isInitialSync)
        pushDirtyVendeurs(cloud, uid, vendeurDao)
        pushInfos(cloud, uid, infosDao)
        pushDirtyTables(cloud, uid, dbLocal.tableDao(), isInitialSync)
        pushDirtyTableItems(cloud, uid, dbLocal.tableDao(), isInitialSync)
        pushDirtyClotures(cloud, uid, venteDao, isInitialSync)
        pushDirtyLogs(cloud, uid, logDao, isInitialSync)

        // 2) PULL : récupérer ce qui a changé depuis lastSyncAt


        pullCategoriesSince(cloud, uid, since, categorieDao, isInitialSync)
        pullProduitsSince(cloud, uid, since, produitDao, isInitialSync)
        pullTablesSince(cloud, uid, since, dbLocal.tableDao(), isInitialSync)
        pullTableItemsSince(cloud, uid, since, dbLocal.tableDao(), produitDao, isInitialSync)
        pullVendeursSince(cloud, uid, since, vendeurDao)
        pullVentesSince(cloud, uid, since, venteDao, isInitialSync)
        pullVenteLignesSince(cloud, uid, since, venteDao, produitDao, isInitialSync)
        pullInfos(cloud, uid, infosDao)
        pullCloturesSince(cloud, uid, since, venteDao, isInitialSync)
        pullLogsSince(cloud, uid, since, logDao, isInitialSync)


        Log.d("SyncWorker", "Sync terminé")
        Log.d("SyncWorker", "venteDao : ${venteDao.getAllVentesOnce()}")


        // 3) MAJ horodatage de sync
        prefs.edit().putLong("lastSyncAt", System.currentTimeMillis()).apply()

        return Result.success()
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
                .set(tableToMap(t.copy(isDirty = false))).await()
            tableDao.upsertTable(t.copy(isDirty = false))
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
                .set(tableItemToMap(ti.copy(isDirty = false))).await()
            tableDao.upsertTableItem(ti.copy(isDirty = false))
        }
    }
    private suspend fun pushDirtyProduits(
        cloud: FirebaseFirestore,
        uid: String,
        produitDao: com.example.caisse.model.ProduitDao,
        isInitialSync: Boolean
    ) {
        val all = produitDao.getAllProduitsOnce() // existe déjà :contentReference[oaicite:2]{index=2}
        val list = if (isInitialSync) all else all.filter { it.isDirty  }
        for (p in list) {
            cloud.collection("users").document(uid)
                .collection("produits").document(p.id.toString())
                .set(produitToMap(p.copy(isDirty = false)))
                .await()
            produitDao.updateProduit(p.copy(isDirty = false))
        }
    }

    private suspend fun pushDirtyVentes(
        cloud: FirebaseFirestore,
        uid: String,
        venteDao: VenteDao
    ) {
        val list = venteDao.getAllVentesOnce().filter { it.isDirty  }
        for (v in list) {
            cloud.collection("users").document(uid)
                .collection("ventes").document(v.id.toString())
                .set(venteToMap(v.copy(isDirty = false)))
                .await()
            // insert (REPLACE) sert d'upsert local
            venteDao.updateVente(v.copy(isDirty = false))
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
                .set(venteLigneToMap(vl.copy(isDirty = false)))
                .await()
            venteDao.updateLigne(vl.copy(isDirty = false))
        }
    }

    private suspend fun pushDirtyCategories(
        cloud: FirebaseFirestore,
        uid: String,
        categorieDao: com.example.caisse.model.CategorieDao,
        isInitialSync: Boolean
    ){
        val all = categorieDao.getAllCategoryOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty}
        for (c in list){
            cloud.collection("users").document(uid)
                .collection("categories").document(c.id.toString())
                .set(categorieToMap(c.copy(isDirty = false)))
                .await()
            categorieDao.addCategory(c.copy(isDirty = false))

        }
    }


    private suspend fun pushDirtyVendeurs(
        cloud: FirebaseFirestore,
        uid: String,
        vendeurDao: VendeurDao
    ) {
        val list = vendeurDao.getAllVendeursOnce().filter { it.isDirty && !it.isDeleted }
        for (v in list) {
            cloud.collection("users").document(uid)
                .collection("vendeurs").document(v.id.toString())
                .set(vendeurToMap(v.copy(isDirty = false)))
                .await()
            vendeurDao.updateVendeur(v.copy(isDirty = false))
        }
    }

    private suspend fun  pushInfos(
        cloud: FirebaseFirestore,
        uid: String,
        infosDao: com.example.caisse.model.InfosDao
    ){
        val infos = infosDao.getInfos()
        if (infos != null) {
            cloud.collection("users").document(uid)
                .collection("infos").document("1")
                .set(infosToMap(infos))
                .await()
            infosDao.updateInfos(infos.copy(isDirty = false))
        }


    }

    private suspend fun pushDirtyClotures(
        cloud: FirebaseFirestore,
        uid: String,
        venteDao: VenteDao,
        isInitialSync: Boolean
    ) {
        val all = venteDao.getAllCloturesOnce()
        val list = if (isInitialSync) all else all.filter { it.isDirty }

        for (c in list) {
            cloud.collection("users").document(uid)
                .collection("clotures").document(c.idCloture.toString())
                .set(clotureToMap(c.copy(isDirty = false)))
                .await()

            venteDao.updateCloture(c.copy(isDirty = false))
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
                .set(logToMap(log.copy(isDirty = false)))
                .await()

            logDao.updateLog(log.copy(isDirty = false))
        }
    }



    // ---------------- PULL ----------------
    private suspend fun pullTablesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        tableDao: TableDao,
        isInitialSync: Boolean
    ) {
        val base = cloud.collection("users").document(uid).collection("tables")
        val snap = if (isInitialSync) base.get().await()
        else base.whereGreaterThanOrEqualTo("updatedAt", since).get().await()
        for (doc in snap.documents) {
            val m = doc.data ?: continue
            val remote = mapToTable(m)
            val local = tableDao.getTableById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                tableDao.upsertTable(remote.copy(isDirty = false))
            }
        }
    }
    private suspend fun pullTableItemsSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        tableDao: TableDao,
        produitDao: ProduitDao,
        isInitialSync: Boolean
    ) {
        val base = cloud.collection("users").document(uid).collection("table_items")
        val snap = if (isInitialSync) base.get().await()
        else base.whereGreaterThanOrEqualTo("updatedAt", since).get().await()
        for (doc in snap.documents) {
            val m = doc.data ?: continue
            val ti = mapToTableItem(m)

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
        }
    }
    private suspend fun pullProduitsSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        produitDao: com.example.caisse.model.ProduitDao,
        isInitialSync: Boolean
    ) {
        val query = cloud.collection("users").document(uid)
            .collection("produits")


        val snap = if (isInitialSync) {
            query.get().await()               // TOUT
        } else {
            query.whereGreaterThanOrEqualTo("updatedAt", since).get().await()
        }

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToProduit(data)
            val local = produitDao.getProduitById(remote.id)
            if (local == null) {
                produitDao.insertProduit(remote.copy(isDirty = false))   // INSERT
            } else if (remote.updatedAt >= local.updatedAt) {
                produitDao.updateProduit(remote.copy(isDirty = false))   // UPDATE (surtout pas REPLACE)
            }
        }
    }

    private suspend fun pullInfos(
        cloud: FirebaseFirestore,
        uid: String,
        infosDao: com.example.caisse.model.InfosDao
    ) {
        val doc = cloud.collection("users").document(uid)
            .collection("infos").document("1")
            .get().await()
        if (!doc.exists()) return
        val data = doc.data ?: return
        val remote = mapToInfos(data).copy(id = 1)  // sécurité : force id=1
        val local = infosDao.getInfos()

        if (local == null ) {
            infosDao.insertInfos(remote)   // insert
        } else {
            infosDao.updateInfos(remote)   // update (écrase avec la version cloud)
        }
    }


    private suspend fun pullCategoriesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        categorieDao: com.example.caisse.model.CategorieDao,
        isInitialSync: Boolean
    ){
        val query = cloud.collection("users").document(uid)
            .collection("categories")

        val snap = if (isInitialSync) {
            query.get().await()
        }else{
            query.whereGreaterThanOrEqualTo("updatedAt", since).get().await()
        }

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToCategorie(data)
            val local = categorieDao.get(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                categorieDao.addCategory(remote.copy(isDirty = false))
            }

        }
    }


    private suspend fun pullVentesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        venteDao: com.example.caisse.model.VenteDao,
        isInitialSync: Boolean
    ) {
        val base = cloud.collection("users").document(uid).collection("ventes")
        val snap = if (isInitialSync) base.get().await()
        else base.whereGreaterThanOrEqualTo("updatedAt", since).get().await()
        Log.d("SyncWorker", "nombre de vente  : ${snap.size()}")




        for (doc in snap.documents) {
            val data = doc.data ?: continue

            try {
                val remote = mapToVente(data)
                val local = venteDao.getVenteById(remote.id)
                Log.d("SyncWorker", "remote : ${remote.total}, ${remote.id},${remote.tableId} ")
                Log.d("SyncWorker", "local : ${local?.total}")

                if (local == null) {
                    venteDao.insertVente(remote.copy(isDirty = false))   // INSERT
                } else if (remote.updatedAt >= local.updatedAt) {
                    venteDao.updateVente(remote.copy(isDirty = false))   // UPDATE (ajoute @Update dans VenteDao si absent)
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
        since: Long,
        venteDao: VenteDao,
        produitDao: ProduitDao,
        isInitialSync: Boolean
    ) {
        val base = cloud.collection("users").document(uid).collection("venteLignes")
        val snap = if (isInitialSync) base.get().await()
        else base.whereGreaterThanOrEqualTo("updatedAt", since).get().await()

        for (doc in snap.documents) {
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
                        // (option) s'assurer du vendeur parent si vendeurId != null (même logique qu'on a ajouté)
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

                // 3) enfin, upsert de la ligne
                val local = venteDao.getVenteLigneById(remote.id)
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    venteDao.insertLigne(remote.copy(isDirty = false))
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
        since: Long,
        vendeurDao: com.example.caisse.model.VendeurDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("vendeurs")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToVendeur(data)
            val local = vendeurDao.getVendeurById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                vendeurDao.updateVendeur(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pullCloturesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        venteDao: VenteDao,
        isInitialSync: Boolean
    ) {
        val base = cloud.collection("users").document(uid).collection("clotures")
        val snap = if (isInitialSync) base.get().await()
        else base.whereGreaterThanOrEqualTo("updatedAt", since).get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            try {
                val remote = mapToCloture(data)
                val local = venteDao.getClotureById(remote.idCloture)

                if (local == null) {
                    venteDao.insertCloture(remote.copy(isDirty = false))
                } else if (remote.updatedAt >= local.updatedAt) {
                    venteDao.updateCloture(remote.copy(isDirty = false))
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Cloture invalide doc=${doc.id}: ${e.message}")
            }
        }
    }


    private suspend fun pullLogsSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        logDao: LogDao,
        isInitialSync: Boolean
    ) {
        val base = cloud.collection("users").document(uid).collection("logs_techniques")
        val snap = if (isInitialSync) base.get().await()
        else base.whereGreaterThanOrEqualTo("updatedAt", since).get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            try {
                val remote = mapToLog(data)
                val local = logDao.getLogById(remote.id)

                if (local == null) {
                    logDao.insertLog(remote.copy(isDirty = false))
                } else if (remote.updatedAt >= local.updatedAt) {
                    logDao.updateLog(remote.copy(isDirty = false))
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
        "logo" to i.logo,
        "passwordHash" to i.passwordHash,
        "passwordSalt" to i.passwordSalt,
        "devise" to i.devise,
        "updatedAt" to i.updatedAt,
        "isDeleted" to i.isDeleted
        )

    private fun mapToInfos(m: Map<String, Any?>) = com.example.caisse.data.ShopInfos(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        name = m["name"] as String,
        address = m["address"] as String,
        phone = m["phone"] as String,
        email = m["email"] as String,
        logo = m["logo"] as? Int,
        passwordHash =
            (m["passwordHash"] as? String)
                ?: (m["password"] as? String)      // ancien format cloud
                ?: "",

        passwordSalt =
            (m["passwordSalt"] as? String)
                ?: "",

        devise = m["devise"] as? String ?: "FCFA",

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
        "vendeurId" to v.vendeurId,
        "total" to v.total,
        "tableId" to v.tableId?.toString(),
        "updatedAt" to v.updatedAt,
        "isDirty" to v.isDirty,
        "isDeleted" to v.isDeleted,
        "hash" to v.hash,
        "previousHash" to v.previousHash,
    )

    private fun mapToVente(m: Map<String, Any?>): Vente {

        val id = parseUuidOrNull(getString(m, "id"))
            ?: throw IllegalArgumentException("vente.id invalide")
        val date = getNumberAsLong(m, "date") ?: System.currentTimeMillis()
        val vendeurId = (m["vendeurId"] as? Number)?.toInt()
            ?: (m["vendeurId"] as? String)?.toIntOrNull()
        val total = getNumberAsDouble(m, "total") ?: 0.0
        val updatedAt = getNumberAsLong(m, "updatedAt") ?: System.currentTimeMillis()
        val tableId = parseUuidOrNull(getString(m, "tableId"))

        return Vente(
            id = id,
            date = date,
            vendeurId = vendeurId,
            total = total,
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
        "updatedAt" to v.updatedAt,
        "isDeleted" to v.isDeleted
    )

    private fun mapToVendeur(m: Map<String, Any?>) = com.example.caisse.data.Vendeur(
        id = (m["id"] as? Number)?.toInt() ?: 0,
        nom = m["nom"] as String,
        prenom = m["prenom"] as String,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
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
        "id" to log.id,
        "date" to log.date,
        "typeEvenement" to log.typeEvenement,
        "description" to log.description,
        "idVendeur" to log.idVendeur?.toString(),
        "empreinte" to log.empreinte,
        "updatedAt" to log.updatedAt,
        "isDirty" to log.isDirty,
        "isDeleted" to log.isDeleted
    )

    private fun mapToLog(m: Map<String, Any?>) = com.example.caisse.data.LogTechnique(
        id = (m["id"] as? Number)?.toLong() ?: 0L,
        date = m["date"] as? String ?: "",
        typeEvenement = m["typeEvenement"] as? String ?: "",
        description = m["description"] as? String ?: "",
        idVendeur = parseUuidOrNull(m["idVendeur"] as? String),
        empreinte = m["empreinte"] as? String ?: "",
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = (m["isDeleted"] as? Boolean) ?: false
    )

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
