package com.example.caisse.data

import com.example.caisse.model.CategorieDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CaisseRepository(
    private val categorieDao: CategorieDao,
    private val produitDao: ProduitDao,
    private val vendeurDao: VendeurDao,
    private val venteDao: VenteDao
) {
    // ----- CATEGORIES -----
    fun getAllCategories(): Flow<List<Category>> = categorieDao.getAllCategory()
    suspend fun addCategory(category: Category) = categorieDao.addCategory(category)
    suspend fun deleteCategory(category: Category) = categorieDao.deleteCategory(category)

    // ----- PRODUITS -----
    fun getAllProduits(): Flow<List<Produit>> = produitDao.getAllProduits()
    suspend fun addProduit(produit: Produit) = produitDao.insertProduit(produit)
    suspend fun deleteProduit(produit: Produit) = produitDao.deleteProduit(produit)

    // ----- VENDEURS -----
    fun getAllVendeurs(): Flow<List<Vendeur>> = vendeurDao.getAllVendeur()
    suspend fun addVendeur(vendeur: Vendeur) = vendeurDao.insertVendeur(vendeur)
    suspend fun deleteVendeur(vendeur: Vendeur) = vendeurDao.deleteVendeur(vendeur)

    // --- VENTES ---
    fun getAllVentes() = venteDao.getAllVentes()
    fun getLignesForVente(venteId: UUID) = venteDao.getLignesForVente(venteId)
    suspend fun insertVente(vente: Vente) = venteDao.insertVente(vente)
    suspend fun insertLigne(ligne: VenteLigne) = venteDao.insertLigne(ligne)
    suspend fun deleteVente(vente: Vente) = venteDao.deleteVente(vente)
    suspend fun deleteLigne(ligne: VenteLigne) = venteDao.deleteLigne(ligne)
}
