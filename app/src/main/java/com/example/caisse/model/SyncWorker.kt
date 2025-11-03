package com.example.caisse.model

import android.content.Context
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


        val cloud = FirebaseFirestore.getInstance()

        // 1) PUSH : envoyer ce qui est dirty (Produit, Vente, VenteLigne)
        pushDirtyProduits(cloud, uid, produitDao)
        pushDirtyVentes(cloud, uid, venteDao)
        pushDirtyVenteLignes(cloud, uid, venteDao)
        pushDirtyCategories(cloud, uid, categorieDao)


        // 2) PULL : récupérer ce qui a changé depuis lastSyncAt
        val prefs = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
        val since = prefs.getLong("lastSyncAt", 0L)

        pullProduitsSince(cloud, uid, since, produitDao)
        pullVentesSince(cloud, uid, since, venteDao)
        pullVenteLignesSince(cloud, uid, since, venteDao)

        // 3) MAJ horodatage de sync
        prefs.edit().putLong("lastSyncAt", System.currentTimeMillis()).apply()

        return Result.success()
    }

    // ---------------- PUSH ----------------

    private suspend fun pushDirtyProduits(
        cloud: FirebaseFirestore,
        uid: String,
        produitDao: com.example.caisse.model.ProduitDao
    ) {
        val list = produitDao.getAllProduitsOnce().filter { it.isDirty && !it.isDeleted }
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
            venteDao.insertVente(v.copy(isDirty = false))
        }
    }

    private suspend fun pushDirtyVenteLignes(
        cloud: FirebaseFirestore,
        uid: String,
        venteDao: com.example.caisse.model.VenteDao
    ) {
        val list = venteDao.getAllVenteLignesOnce().filter { it.isDirty && !it.isDeleted }
        for (vl in list) {
            cloud.collection("users").document(uid)
                .collection("venteLignes").document(vl.id.toString())
                .set(venteLigneToMap(vl.copy(isDirty = false)))
                .await()
            venteDao.insertLigne(vl.copy(isDirty = false))
        }
    }

    private suspend fun pushDirtyCategories(
        cloud: FirebaseFirestore,
        uid: String,
        categorieDao: com.example.caisse.model.CategorieDao
    ){
        val list = categorieDao.getAllCategoryOnce().filter { it.isDirty && !it.isDeleted }
        for (c in list){
            cloud.collection("users").document(uid)
                .collection("categories").document(c.id.toString())
                .set(categorieToMap(c.copy(isDirty = false)))
                .await()
            categorieDao.addCategory(c.copy(isDirty = false))

        }
    }


    // ---------------- PULL ----------------

    private suspend fun pullProduitsSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        produitDao: com.example.caisse.model.ProduitDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("produits")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToProduit(data)
            val local = produitDao.getProduitById(remote.id)
            // "dernier gagnant"
            if (local == null || remote.updatedAt >= local.updatedAt) {
                produitDao.updateProduit(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pullCategoriesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        categorieDao: com.example.caisse.model.CategorieDao
    ){
        val snap = cloud.collection("users").document(uid)
            .collection("categories")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

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
        venteDao: com.example.caisse.model.VenteDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("ventes")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToVente(data)
            val local = venteDao.getVenteById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                // insertVente est en REPLACE -> upsert
                venteDao.insertVente(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pullVenteLignesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        venteDao: com.example.caisse.model.VenteDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("venteLignes")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToVenteLigne(data)
            val local = venteDao.getVenteLigneById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                venteDao.insertLigne(remote.copy(isDirty = false))
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
        "updatedAt" to v.updatedAt,
        "isDirty" to v.isDirty,
        "isDeleted" to v.isDeleted
    )

    private fun mapToVente(m: Map<String, Any?>): Vente {
        return Vente(
            id = UUID.fromString(m["id"] as String),
            date = (m["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            vendeurId = (m["vendeurId"] as? Number)?.toInt(),
            total = (m["total"] as? Number)?.toDouble() ?: 0.0,
            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            isDirty = m["isDirty"] as? Boolean ?: false,
            isDeleted = m["isDeleted"] as? Boolean ?: false
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
}
