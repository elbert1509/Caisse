package com.example.caisse.model

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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




        // 2) PULL : récupérer ce qui a changé depuis lastSyncAt


        pullCategoriesSince(cloud, uid, since, categorieDao, isInitialSync)                 // 1️⃣
        pullProduitsSince(cloud, uid, since, produitDao, isInitialSync)                     // 2️⃣
        pullVendeursSince(cloud, uid, since, vendeurDao)                                    // 3️⃣ (Vente.vendeurId nullable, mais mieux avant)
        pullVentesSince(cloud, uid, since, venteDao, isInitialSync)                         // 4️⃣
        pullVenteLignesSince(cloud, uid, since, venteDao,produitDao, isInitialSync)                    // 5️⃣


        Log.d("SyncWorker", "Sync terminé")
        Log.d("SyncWorker", "venteDao : ${venteDao.getAllVentesOnce()}")


        // 3) MAJ horodatage de sync
        prefs.edit().putLong("lastSyncAt", System.currentTimeMillis()).apply()

        return Result.success()
    }

    // ---------------- PUSH ----------------

    private suspend fun pushDirtyProduits(
        cloud: FirebaseFirestore,
        uid: String,
        produitDao: com.example.caisse.model.ProduitDao,
        isInitialSync: Boolean
    ) {
        val all = produitDao.getAllProduitsOnce() // existe déjà :contentReference[oaicite:2]{index=2}
        val list = if (isInitialSync) all else all.filter { it.isDirty && !it.isDeleted }
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
        venteDao: com.example.caisse.model.VenteDao
    ) {
        val list = venteDao.getAllVentesOnce().filter { it.isDirty && !it.isDeleted }
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
        val list = if (isInitialSync) all else all.filter { it.isDirty && !it.isDeleted }
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
        val list = if (isInitialSync) all else all.filter { it.isDirty && !it.isDeleted }
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
        vendeurDao: com.example.caisse.model.VendeurDao
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


    // ---------------- PULL ----------------

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
    // --------------- MAPPERS (copiés depuis repo) ---------------

    private fun produitToMap(p: Produit) = mapOf(
        "id" to p.id.toString(),
        "nom" to p.nom,
        "prix" to p.prix,
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

    private fun mapToCategorie(m: Map<String, Any?>) = com.example.caisse.data.Category(
        id = UUID.fromString(m["id"] as String),
        name = m["name"] as String,
        description = m["description"] as String?,
        icon = m["icon"] as? Int,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false,
    )





    private fun mapToProduit(m: Map<String, Any?>) = Produit(
        id = UUID.fromString(m["id"] as String),
        nom = m["nom"] as String,
        prix = (m["prix"] as Number).toDouble(),
        categoryId = UUID.fromString(m["categoryId"] as String),
        stock = (m["stock"] as Number).toInt(),
        description = m["description"] as String?,
        isActive = m["isActive"] as? Boolean ?: true,
        updatedAt = (m["updatedAt"] as Number?)?.toLong() ?: System.currentTimeMillis(),
        isDirty = false,
        isDeleted = m["isDeleted"] as? Boolean ?: false
    )

    private fun venteToMap(v: Vente) = mapOf(
        "id" to v.id.toString(),
        "date" to v.date,
        "vendeurId" to v.vendeurId,
        "total" to v.total,
        "tableId" to v.tableId?.toString(),
        "updatedAt" to v.updatedAt,
        "isDirty" to v.isDirty,
        "isDeleted" to v.isDeleted
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
            isDeleted = (m["isDeleted"] as? Boolean) ?: false
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
