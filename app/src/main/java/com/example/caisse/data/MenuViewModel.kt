package com.example.caisse.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MenuViewModel(private val repository: CaisseRepository) : ViewModel() {

    // ---- Flows exposés ----
    val categories: StateFlow<List<Category>> =
        repository.getAllCategories().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val products: StateFlow<List<Produit>> =
        repository.getAllProduits().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vendeurs: StateFlow<List<Vendeur>> =
        repository.getAllVendeurs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ---- CATEGORIES ----
    fun addCategory(name: String, description: String? = null, icon: Int? = null) {
        viewModelScope.launch {
            repository.addCategory(Category(name = name, description = description, icon = icon))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun renameCategory(id: UUID, newName: String) {
        viewModelScope.launch {
            val category = categories.value.find { it.id == id }
            category?.let {
                repository.addCategory(it.copy(name = newName)) // REPLACE grâce à onConflictStrategy
            }
        }
    }

    // ---- PRODUITS ----
    fun addProduit(produit: Produit) {
        viewModelScope.launch {
            repository.addProduit(produit)
        }
    }

    fun deleteProduit(produit: Produit) {
        viewModelScope.launch {
            repository.deleteProduit(produit)
        }
    }

    // ---- VENDEURS ----
    fun addVendeur(vendeur: Vendeur) {
        viewModelScope.launch {
            repository.addVendeur(vendeur)
        }
    }

    fun deleteVendeur(vendeur: Vendeur) {
        viewModelScope.launch {
            repository.deleteVendeur(vendeur)
        }
    }



    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    val database = CaisseDataBase.getDatabase(context)
                    val repository = CaisseRepository(
                        database.categorieDao(),
                        database.produitDao(),
                        database.vendeurDao()
                    )
                    MenuViewModel(repository)
                }
            }
        }
    }
}
