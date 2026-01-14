package com.example.piece.data

import com.example.piece.model.CategorieDao
import com.example.piece.model.InfosDao
import com.example.piece.model.InvoiceDao
import com.example.piece.model.ProduitDao
import com.example.piece.model.RecetteDao
import com.example.piece.model.TableDao
import com.example.piece.model.VendeurDao
import com.example.piece.model.VenteDao
import com.example.piece.model.VoitureDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CaisseRepository(
    private val categorieDao: CategorieDao,
    private val produitDao: ProduitDao,
    private val vendeurDao: VendeurDao,
    val venteDao: VenteDao,
    private val tableDao: TableDao,
    private val invoiceDao: InvoiceDao,
    private val infosDao: InfosDao,
    private val voitureDao: VoitureDao,
    private val recetteDao: RecetteDao
) {
    // ----- CATEGORIES -----
    fun getAllCategories(): Flow<List<Category>> = categorieDao.getAllCategory()
    suspend fun addCategory(category: Category) = categorieDao.addCategory(category)
    suspend fun deleteCategory(category: Category) = categorieDao.deleteCategory(category)
    suspend fun getAllCategoriesOnce(): List<Category> = categorieDao.getAllCategoryOnce()
    suspend fun getCategoryById(id: UUID): Category? = categorieDao.get(id)
    suspend fun updateCategory(category: Category) = categorieDao.updateCategory(category)





    // ----- PRODUITS -----
    fun getAllProduits(): Flow<List<Produit>> = produitDao.getAllProduits()
    suspend fun addProduit(produit: Produit) = produitDao.insertProduit(produit)
    suspend fun deleteProduit(produit: Produit) = produitDao.deleteProduit(produit)
    suspend fun getProduitById(id: UUID): Produit? = produitDao.getProduitById(id)
    suspend fun updateProduit(produit: Produit) = produitDao.updateProduit(produit)
    suspend fun getAllProduitsOnce(): List<Produit> = produitDao.getAllProduitsOnce()
    suspend fun insertProduit(produit: Produit) = produitDao.insertProduit(produit)
    suspend fun getProduitByBarcode(barcode: String): Produit? = produitDao.getProduitByBarcode(barcode)
    suspend fun countSameBarcode(barcode: String, excludeId: UUID): Int = produitDao.countSameBarcode(barcode, excludeId)





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
    suspend fun getAllTableItemsOnce(): List<TableItem> = tableDao.getAllTableItemsOnce()
    suspend fun upsertTable(appTable: AppTable) = tableDao.upsertTable(appTable)
    suspend fun upsertTableItem(ti: TableItem) = tableDao.upsertTableItem(ti)
    suspend fun getTableById(id: UUID): AppTable? = tableDao.getTableById(id)


    // Invoice methods
    suspend fun addInvoice(invoice: Invoice) = invoiceDao.addInvoice(invoice)
    suspend fun addInvoiceItem(invoiceItem: InvoiceItem) = invoiceDao.addInvoiceItem(invoiceItem)
    suspend fun getAllInvoices(): List<Invoice> = invoiceDao.getAllInvoices()
    suspend fun getInvoiceItems(invoiceId: UUID): List<InvoiceItem> = invoiceDao.getInvoiceItems(invoiceId)

    //Infos
    suspend fun insertInfos(infos: ShopInfos) = infosDao.insertInfos(infos)
    suspend fun updateInfos(infos: ShopInfos) = infosDao.updateInfos(infos)
    suspend fun getInfos(): ShopInfos? = infosDao.getInfos()
    suspend fun updatePassword(passwordHash: String, passwordSalt: String) = infosDao.updatePassword(passwordHash, passwordSalt)

    // ----- VOITURE -----
    fun getAllVoitures() = voitureDao.getAllVoitures()
    suspend fun addVoiture(voiture: Voiture) = voitureDao.insertVoiture(voiture)
    suspend fun updateVoiture(voiture: Voiture) = voitureDao.updateVoiture(voiture)
    suspend fun deleteVoiture(voiture: Voiture) = voitureDao.deleteVoiture(voiture)
    suspend fun softDeleteVoiture(id: UUID) = voitureDao.softDeleteVoiture(id)
    suspend fun getVoitureById(id: UUID) = voitureDao.getVoitureById(id)
    suspend fun getDirtyVoituresOnce() = voitureDao.getDirtyVoituresOnce()

    // ----- RECETTE -----
    fun getAllRecettes() = recetteDao.getAllRecettes()
    fun getRecettesByVoiture(voitureId: UUID) = recetteDao.getRecettesByVoiture(voitureId)
    fun getRecettesBetween(start: Long, end: Long) = recetteDao.getRecettesBetween(start, end)
    suspend fun addRecette(recette: Recette) = recetteDao.insertRecette(recette)
    suspend fun updateRecette(recette: Recette) = recetteDao.updateRecette(recette)
    suspend fun deleteRecette(recette: Recette) = recetteDao.deleteRecette(recette)
    suspend fun softDeleteRecette(id: UUID) = recetteDao.softDeleteRecette(id)
    suspend fun getDirtyRecettesOnce() = recetteDao.getDirtyRecettesOnce()
    suspend fun getRecetteById(id: UUID): Recette? = recetteDao.getRecetteById(id)





}
