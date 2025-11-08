package com.example.caisse.data

import com.example.caisse.model.CategorieDao
import com.example.caisse.model.InvoiceDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.TableDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CaisseRepository(
    private val categorieDao: CategorieDao,
    private val produitDao: ProduitDao,
    private val vendeurDao: VendeurDao,
     val venteDao: VenteDao,
    private val tableDao: TableDao,
    private val invoiceDao: InvoiceDao
) {
    // ----- CATEGORIES -----
    fun getAllCategories(): Flow<List<Category>> = categorieDao.getAllCategory()
    suspend fun addCategory(category: Category) = categorieDao.addCategory(category)
    suspend fun deleteCategory(category: Category) = categorieDao.deleteCategory(category)
    suspend fun getAllCategoriesOnce(): List<Category> = categorieDao.getAllCategoryOnce()
    suspend fun getCategoryById(id: UUID): Category? = categorieDao.get(id)




    // ----- PRODUITS -----
    fun getAllProduits(): Flow<List<Produit>> = produitDao.getAllProduits()
    suspend fun addProduit(produit: Produit) = produitDao.insertProduit(produit)
    suspend fun deleteProduit(produit: Produit) = produitDao.deleteProduit(produit)
    suspend fun getProduitById(id: UUID): Produit? = produitDao.getProduitById(id)
    suspend fun updateProduit(produit: Produit) = produitDao.updateProduit(produit)
    suspend fun getAllProduitsOnce(): List<Produit> = produitDao.getAllProduitsOnce()
    suspend fun insertProduit(produit: Produit) = produitDao.insertProduit(produit)





    // ----- VENDEURS -----
    fun getAllVendeurs(): Flow<List<Vendeur>> = vendeurDao.getAllVendeur()
    suspend fun addVendeur(vendeur: Vendeur) = vendeurDao.insertVendeur(vendeur)
    suspend fun deleteVendeur(vendeur: Vendeur) = vendeurDao.deleteVendeur(vendeur)

    // --- VENTES ---
    fun getAllVentes() = venteDao.getAllVentes()
    fun getLignesForVente(venteId: UUID) = venteDao.getLignesForVente(venteId)
    suspend fun updateVente(vente: Vente) = venteDao.updateVente(vente)
    suspend fun insertVente(vente: Vente) = venteDao.insertVente(vente)
    suspend fun insertLigne(ligne: VenteLigne) = venteDao.insertLigne(ligne)
    suspend fun deleteVente(vente: Vente) = venteDao.deleteVente(vente)
    suspend fun deleteLigne(ligne: VenteLigne) = venteDao.deleteLigne(ligne)
    suspend fun insertVenteWithLignes(vente: Vente, lignes: List<VenteLigne>) =
        venteDao.insertVenteWithLignes(vente, lignes)
    fun getProductReportBetween(start: Long, end: Long) =
        venteDao.getProductReportBetween(start, end)

    fun getTotalRevenueBetween(start: Long, end: Long) =
        venteDao.getTotalRevenueBetween(start, end)





    // Table methods
    suspend fun addTable(appTable: AppTable) = tableDao.addTable(appTable)
    suspend fun updateTable(appTable: AppTable) = tableDao.updateTable(appTable)
    suspend fun getActiveTables(): List<AppTable> = tableDao.getActiveTables()
    suspend fun addProductToTable(tableItem: TableItem) = tableDao.addProductToTable(tableItem)
    suspend fun updateProductInTable(tableItem: TableItem) = tableDao.updateProductInTable(tableItem)
    suspend fun deleteProductFromTable(tableId: UUID, productId: UUID) = tableDao.deleteProductFromTable(tableId, productId)
    suspend fun getTableItems(tableId: UUID): List<TableItem> = tableDao.getTableItems(tableId)


    // Invoice methods
    suspend fun addInvoice(invoice: Invoice) = invoiceDao.addInvoice(invoice)
    suspend fun addInvoiceItem(invoiceItem: InvoiceItem) = invoiceDao.addInvoiceItem(invoiceItem)
    suspend fun getAllInvoices(): List<Invoice> = invoiceDao.getAllInvoices()
    suspend fun getInvoiceItems(invoiceId: UUID): List<InvoiceItem> = invoiceDao.getInvoiceItems(invoiceId)

}
