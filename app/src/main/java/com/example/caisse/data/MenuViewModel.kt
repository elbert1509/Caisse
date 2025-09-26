package com.example.caisse.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    val produits: StateFlow<List<Produit>> =
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
    fun addProduit(name: String, price: Double, categoryId: UUID) {
        viewModelScope.launch {
            val newProduit = Produit(nom = name, prix = price, categoryId = categoryId)
            repository.addProduit(newProduit)
        }
    }

    fun updateProduit(produit: Produit) {
        viewModelScope.launch {
            repository.addProduit(produit) // OnConflictStrategy.REPLACE will handle the update
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


    val ventes: StateFlow<List<Vente>> =
        repository.getAllVentes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addVente(vente: Vente, lignes: List<VenteLigne>) {
        viewModelScope.launch {
            repository.insertVente(vente)
            lignes.forEach { repository.insertLigne(it) }
        }
    }

    fun deleteVente(vente: Vente) {
        viewModelScope.launch {
            repository.deleteVente(vente)
        }
    }

    fun confirmerVente(vendeurId: Int? = null) {
        viewModelScope.launch {
            val cartItems = cart.value
            if (cartItems.isEmpty()) return@launch

            // 1. Créer la vente
            val venteId = UUID.randomUUID()
            val vente = Vente(
                id = venteId,
                vendeurId = vendeurId,
                total = totalPrice.value
            )
            repository.insertVente(vente)

            // 2. Créer les lignes
            cartItems.forEach { ticket ->
                val ligne = VenteLigne(
                    venteId = venteId,
                    produitId = ticket.produit.id,
                    quantity = ticket.quantity,
                    prixUnitaire = ticket.produit.prix,
                    sousTotal = ticket.produit.prix * ticket.quantity
                )
                repository.insertLigne(ligne)
            }

            // 3. Vider le panier
            clearCart()
        }
    }



    // ---- PANIER ----
    private val _cart = MutableStateFlow<List<Ticket>>(emptyList())
    val cart: StateFlow<List<Ticket>> = _cart.asStateFlow()

    val totalPrice: StateFlow<Double> = _cart.map { tickets ->
        tickets.sumOf { it.produit.prix * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addToCart(produit: Produit) {
        _cart.update { currentCart ->
            val existingTicket = currentCart.find { it.produit.id == produit.id }
            if (existingTicket != null) {
                currentCart.map {
                    if (it.produit.id == produit.id) {
                        it.copy(quantity = it.quantity + 1)
                    } else {
                        it
                    }
                }
            } else {
                currentCart + Ticket(produit, 1)
            }
        }
    }
    fun clearCart() {
        _cart.value = emptyList()
    }
    fun increaseQuantity(produitId: UUID) {
        _cart.update { currentCart ->
            currentCart.map {
                if (it.produit.id == produitId) {
                    it.copy(quantity = it.quantity + 1)
                } else {
                    it
                }
            }
        }
    }

    fun decreaseQuantity(produitId: UUID) {
        _cart.update { currentCart ->
            val ticket = currentCart.find { it.produit.id == produitId }
            if (ticket != null && ticket.quantity > 1) {
                currentCart.map {
                    if (it.produit.id == produitId) {
                        it.copy(quantity = it.quantity - 1)
                    } else {
                        it
                    }
                }
            } else {
                currentCart.filterNot { it.produit.id == produitId }
            }
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
                        database.vendeurDao(),
                        database.venteDao()
                    )
                    MenuViewModel(repository)
                }
            }
        }
    }
}
