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
        val vendeurDao = dbLocal.vendeurDao()
        val tableDao = dbLocal.tableDao()
        val invoiceDao = dbLocal.invoiceDao()


        val cloud = FirebaseFirestore.getInstance()

        // 1) PUSH : envoyer ce qui est dirty (Produit, Vente, VenteLigne)
        pushDirtyProduits(cloud, uid, produitDao)
        pushDirtyVentes(cloud, uid, venteDao)
        pushDirtyVenteLignes(cloud, uid, venteDao)
        pushDirtyCategories(cloud, uid, categorieDao)
        pushDirtyVendeurs(cloud, uid, vendeurDao)
        pushDirtyAppTables(cloud, uid, tableDao)
        pushDirtyTableItems(cloud, uid, tableDao)
        pushDirtyInvoices(cloud, uid, invoiceDao)
        pushDirtyInvoiceItems(cloud, uid, invoiceDao)


        // 2) PULL : récupérer ce qui a changé depuis lastSyncAt
        val prefs = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
        val since = prefs.getLong("lastSyncAt", 0L)

        pullProduitsSince(cloud, uid, since, produitDao)
        pullVentesSince(cloud, uid, since, venteDao)
        pullVenteLignesSince(cloud, uid, since, venteDao)
        pullVendeursSince(cloud, uid, since, vendeurDao)
        pullAppTablesSince(cloud, uid, since, tableDao)
        pullTableItemsSince(cloud, uid, since, tableDao)
        pullInvoicesSince(cloud, uid, since, invoiceDao)
        pullInvoiceItemsSince(cloud, uid, since, invoiceDao)

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

    private suspend fun pullInvoiceItemsSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        invoiceDao: com.example.caisse.model.InvoiceDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("invoiceItems")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToInvoiceItem(data)
            val local = invoiceDao.getInvoiceItemById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                invoiceDao.updateInvoiceItem(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pushDirtyInvoiceItems(
        cloud: FirebaseFirestore,
        uid: String,
        invoiceDao: com.example.caisse.model.InvoiceDao
    ) {
        val list = invoiceDao.getAllInvoiceItemsOnce().filter { it.isDirty && !it.isDeleted }
        for (ii in list) {
            cloud.collection("users").document(uid)
                .collection("invoiceItems").document(ii.id.toString())
                .set(invoiceItemToMap(ii.copy(isDirty = false)))
                .await()
            invoiceDao.updateInvoiceItem(ii.copy(isDirty = false))
        }
    }

    private suspend fun pullInvoicesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        invoiceDao: com.example.caisse.model.InvoiceDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("invoices")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToInvoice(data)
            val local = invoiceDao.getInvoiceById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                invoiceDao.updateInvoice(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pushDirtyInvoices(
        cloud: FirebaseFirestore,
        uid: String,
        invoiceDao: com.example.caisse.model.InvoiceDao
    ) {
        val list = invoiceDao.getAllInvoicesOnce().filter { it.isDirty && !it.isDeleted }
        for (i in list) {
            cloud.collection("users").document(uid)
                .collection("invoices").document(i.id.toString())
                .set(invoiceToMap(i.copy(isDirty = false)))
                .await()
            invoiceDao.updateInvoice(i.copy(isDirty = false))
        }
    }

    private suspend fun pullTableItemsSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        tableDao: com.example.caisse.model.TableDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("tableItems")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToTableItem(data)
            val local = tableDao.getTableItemById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                tableDao.updateTableItem(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pushDirtyTableItems(
        cloud: FirebaseFirestore,
        uid: String,
        tableDao: com.example.caisse.model.TableDao
    ) {
        val list = tableDao.getAllTableItemsOnce().filter { it.isDirty && !it.isDeleted }
        for (ti in list) {
            cloud.collection("users").document(uid)
                .collection("tableItems").document(ti.id.toString())
                .set(tableItemToMap(ti.copy(isDirty = false)))
                .await()
            tableDao.updateTableItem(ti.copy(isDirty = false))
        }
    }

    private suspend fun pullAppTablesSince(
        cloud: FirebaseFirestore,
        uid: String,
        since: Long,
        tableDao: com.example.caisse.model.TableDao
    ) {
        val snap = cloud.collection("users").document(uid)
            .collection("appTables")
            .whereGreaterThanOrEqualTo("updatedAt", since)
            .get().await()

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val remote = mapToAppTable(data)
            val local = tableDao.getTableById(remote.id)
            if (local == null || remote.updatedAt >= local.updatedAt) {
                tableDao.updateTable(remote.copy(isDirty = false))
            }
        }
    }

    private suspend fun pushDirtyAppTables(
        cloud: FirebaseFirestore,
        uid: String,
        tableDao: com.example.caisse.model.TableDao
    ) {
        val list = tableDao.getAllTablesOnce().filter { it.isDirty && !it.isDeleted }
        for (t in list) {
            cloud.collection("users").document(uid)
                .collection("appTables").document(t.id.toString())
                .set(appTableToMap(t.copy(isDirty = false)))
                .await()
            tableDao.updateTable(t.copy(isDirty = false))
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

    private fun vendeurToMap(v: com.example.caisse.data.Vendeur) = mapOf(
        "id" to v.id.toString(),
        "nom" to v.nom,
        "prenom" to v.prenom,
        "updatedAt" to v.updatedAt,
        "isDeleted" to v.isDeleted
    )

    private fun mapToVendeur(m: Map<String, Any?>) = com.example.caisse.data.Vendeur(
        id = UUID.fromString(m["id"] as String),
        nom = m["nom"] as String,
        prenom = m["prenom"] as String,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false
    )

    private fun appTableToMap(t: com.example.caisse.data.AppTable) = mapOf(
        "id" to t.id.toString(),
        "name" to t.name,
        "active" to t.active,
        "updatedAt" to t.updatedAt,
        "isDeleted" to t.isDeleted
    )

    private fun mapToAppTable(m: Map<String, Any?>) = com.example.caisse.data.AppTable(
        id = UUID.fromString(m["id"] as String),
        name = m["name"] as String,
        active = m["active"] as Boolean,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false
    )

    private fun tableItemToMap(ti: com.example.caisse.data.TableItem) = mapOf(
        "id" to ti.id.toString(),
        "tableId" to ti.tableId.toString(),
        "productId" to ti.productId.toString(),
        "quantity" to ti.quantity,
        "updatedAt" to ti.updatedAt,
        "isDeleted" to ti.isDeleted
    )

    private fun mapToTableItem(m: Map<String, Any?>) = com.example.caisse.data.TableItem(
        id = UUID.fromString(m["id"] as String),
        tableId = UUID.fromString(m["tableId"] as String),
        productId = UUID.fromString(m["productId"] as String),
        quantity = (m["quantity"] as Number).toInt(),
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false
    )

    private fun invoiceToMap(i: com.example.caisse.data.Invoice) = mapOf(
        "id" to i.id.toString(),
        "tableId" to i.tableId.toString(),
        "totalAmount" to i.totalAmount,
        "date" to i.date,
        "updatedAt" to i.updatedAt,
        "isDeleted" to i.isDeleted
    )

    private fun mapToInvoice(m: Map<String, Any?>) = com.example.caisse.data.Invoice(
        id = UUID.fromString(m["id"] as String),
        tableId = UUID.fromString(m["tableId"] as String),
        totalAmount = (m["totalAmount"] as Number).toDouble(),
        date = (m["date"] as Number).toLong(),
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false
    )

    private fun invoiceItemToMap(ii: com.example.caisse.data.InvoiceItem) = mapOf(
        "id" to ii.id.toString(),
        "invoiceId" to ii.invoiceId.toString(),
        "productId" to ii.productId.toString(),
        "quantity" to ii.quantity,
        "updatedAt" to ii.updatedAt,
        "isDeleted" to ii.isDeleted
    )

    private fun mapToInvoiceItem(m: Map<String, Any?>) = com.example.caisse.data.InvoiceItem(
        id = UUID.fromString(m["id"] as String),
        invoiceId = UUID.fromString(m["invoiceId"] as String),
        productId = UUID.fromString(m["productId"] as String),
        quantity = (m["quantity"] as Number).toInt(),
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        isDirty = false
    )

    private fun venteToMap(v: Vente) = mapOf(
        "id" to v.id.toString(),
        "date" to v.date,
        "vendeurId" to v.vendeurId.toString(),
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
