package com.example.caisse.data

import com.example.caisse.model.CategorieDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.VendeurDao
import kotlinx.coroutines.flow.Flow

class CaisseRepository(
    private val categorieDao: CategorieDao,
    private val produitDao: ProduitDao,
    private val vendeurDao: VendeurDao
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
}
