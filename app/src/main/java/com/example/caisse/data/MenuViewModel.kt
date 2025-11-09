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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class MenuViewModel( val repository: CaisseRepository) : ViewModel() {

    private fun now() = System.currentTimeMillis()
    private val sentinelPanier = UUID.fromString("22222222-0000-2222-2222-222222222222")

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
            repository.addCategory(Category(name = name, description = description, icon = icon,isDirty = true))
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
    fun addCategorySample(category: Category) {
        viewModelScope.launch {

            val newCategory = category.copy(updatedAt = now(), isDirty = true)
            repository.addCategory(newCategory)
        }
    }

    // ---- PRODUITS ----
    fun addProduit(name: String, price: Double, categoryId: UUID,stock:Int = 12) {
        viewModelScope.launch {
            val newProduit = Produit(nom = name, prix = price, categoryId = categoryId,stock = stock).copy(updatedAt = now(), isDirty = true)
            repository.addProduit(newProduit)
        }
    }

    fun updateProduit(produit: Produit) {
        viewModelScope.launch {
            repository.addProduit(produit.copy(updatedAt = now(), isDirty = true)) // OnConflictStrategy.REPLACE will handle the update
        }
    }

    fun deleteProduit(produit: Produit) {
        viewModelScope.launch {
            repository.deleteProduit(produit)
        }
    }

    // Infos

    fun addInfos(name: String, address: String, phone: String, email: String, logo: Int? = null) {
        viewModelScope.launch {
            repository.insertInfos(ShopInfos(1,name, address, phone, email, logo))
        }
    }
    fun updateInfos(name: String, address: String, phone: String, email: String, logo: Int? = null) {
        viewModelScope.launch {
            repository.updateInfos(ShopInfos(1, name, address, phone, email, logo))
        }
    }

   fun getInfos(): ShopInfos? {
        return runBlocking {
            repository.getInfos()
        }
    }

    // ---- VENDEURS ----
    fun addVendeur(vendeur: Vendeur) {
        viewModelScope.launch {
            val newVendeur = vendeur.copy(updatedAt = now(), isDirty = true)
            repository.addVendeur(newVendeur )
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
            val v = vente.copy(updatedAt = now(), isDirty = true)
            val ls = lignes.map { it.copy(updatedAt = now(), isDirty = true) }
            repository.insertVenteWithLignes(v, ls)

            val tickets = ls.mapNotNull { l ->
                repository.getProduitById(l.produitId)?.let { p -> Ticket(p, l.quantity) }
            }
            decrementStocks(tickets)
        }
    }

    fun deleteVente(vente: Vente) {
        viewModelScope.launch {
            repository.deleteVente(vente)
        }
    }


    fun confirmerVente(vendeurId: Int? = 1) {
        viewModelScope.launch {
            val cartItems = cart.value
            if (cartItems.isEmpty()) return@launch

            val venteId = UUID.randomUUID()
            val vente = Vente(
                id = venteId,
                vendeurId = vendeurId,
                total = totalPrice.value,
                date = now()
            ).copy(updatedAt = now(), isDirty = true)

            val lignes = cartItems.map { ticket ->
                VenteLigne(
                    id = UUID.randomUUID(),
                    venteId = venteId,
                    produitId = ticket.produit.id,
                    quantity = ticket.quantity,
                    prixUnitaire = ticket.produit.prix,
                    sousTotal = ticket.produit.prix * ticket.quantity
                ).copy(updatedAt = now(), isDirty = true)
            }

            // ❌ plus de repository.insertVente(vente) ici
            repository.insertVenteWithLignes(vente, lignes) // ✅ une seule transaction

            decrementStocks(cartItems)
            clearCart()
        }
    }

    fun deleteVenteWithStock(vente: Vente) {
        viewModelScope.launch {
            // 1) Récupérer les lignes de la vente
            val lignes = repository.getLignesForVente(vente.id).first()

            // 2) Rétablir les stocks (on ajoute les quantités vendues)
            lignes.forEach { l ->
                val p = repository.getProduitById(l.produitId)
                if (p != null) {
                    repository.updateProduit(
                        p.copy(
                            stock = (p.stock + l.quantity),
                            updatedAt = now(),
                            isDirty = true
                        )
                    )
                }
            }

            // 3) Marquer la vente + ses lignes en "supprimé" (soft delete) et "dirty" pour synchro
            repository.updateVente(
                vente.copy(
                    isDeleted = true,
                    isDirty = true,
                    updatedAt = now()
                )
            )

            lignes.forEach { l ->
                // on marque la ligne supprimée + dirty pour push
                val updated = l.copy(isDeleted = true, isDirty = true, updatedAt = now())
                // accès direct au DAO exposé par le repo pour faire un @Update
                repository.venteDao.updateLigne(updated)
            }

            // (Option) si tu veux vraiment purger localement, tu pourrais deleteVente + deleteLigne,
            // mais ça ne pousserait pas l'info au cloud. Le soft delete permet à SyncWorker d'envoyer isDeleted.
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



    // ---- TABLES ----

    private val _tables = MutableStateFlow<List<AppTable>>(emptyList())
    val tables: StateFlow<List<AppTable>> = _tables.asStateFlow()

    private val _tableItems = MutableStateFlow<List<Ticket>>(emptyList())
    val tableItems: StateFlow<List<Ticket>> = _tableItems.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()
    val totalAmount: StateFlow<Double> = _tableItems.map { tickets ->
        tickets.sumOf { it.produit.prix * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // ⚡ Charger les tables dès que le ViewModel est instancié
        loadTables()
    }
    fun loadTables() {
        viewModelScope.launch {
            _tables.value = repository.getActiveTables()
        }
    }

    fun addTable(name: String) {
        viewModelScope.launch {
            repository.addTable(AppTable(name = name))
            _tableItems.value = emptyList() // Clear items from previous table
            loadTables()
        }
    }

    fun loadTableItems(tableId: UUID) {
        viewModelScope.launch {
            val items = repository.getTableItems(tableId)
            val tickets = items.mapNotNull { item ->
                repository.getProduitById(item.productId)?.let { product ->
                    Ticket(product, item.quantity)
                }
            }
            _tableItems.value = tickets
        }
    }
    fun clearTableItems() {
        _tableItems.value = emptyList()
    }
    fun deleteTable(tableId: UUID) {
        viewModelScope.launch {
            val table = _tables.value.find { it.id == tableId }
            if (table != null) {
                repository.updateTable(table.copy(active = false))
                loadTables()
            }
        }
    }
    fun addProductToTable(productId: UUID, tableId: UUID) {
        viewModelScope.launch {
            val existingItem = repository.getTableItems(tableId).find { it.productId == productId }
            if (existingItem != null) {
                repository.updateProductInTable(existingItem.copy(quantity = existingItem.quantity + 1))
            } else {
                repository.addProductToTable(TableItem(tableId = tableId, productId = productId, quantity = 1))
            }
            loadTableItems(tableId)
        }
    }

    fun updateTableItemQuantity(productId: UUID, tableId: UUID, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity > 0) {
                val item = repository.getTableItems(tableId).find { it.productId == productId }
                item?.let {
                    repository.updateProductInTable(it.copy(quantity = newQuantity))
                }
            } else {
                // If quantity is 0 or less, delete the item
                repository.deleteProductFromTable(tableId, productId)
            }
            loadTableItems(tableId)
        }
    }

    fun getTableById(id: UUID): AppTable? {
        return _tables.value.find { it.id == id }
    }
    fun payTable(tableId: UUID) {
        viewModelScope.launch {
            val itemsToPay = _tableItems.value
            if (itemsToPay.isEmpty()) return@launch

            val venteId = UUID.randomUUID()
            val vente = Vente(
                id = venteId,
                vendeurId = 1,
                total = itemsToPay.sumOf { it.produit.prix * it.quantity },
                date = now(),
                tableId = tableId
            ).copy(updatedAt = now(), isDirty = true)

            val lignes = itemsToPay.map { ticket ->
                VenteLigne(
                    id = UUID.randomUUID(),
                    venteId = venteId,
                    produitId = ticket.produit.id,
                    quantity = ticket.quantity,
                    prixUnitaire = ticket.produit.prix,
                    sousTotal = ticket.produit.prix * ticket.quantity
                ).copy(updatedAt = now(), isDirty = true)
            }
            repository.insertVenteWithLignes(vente, lignes)

            val total = itemsToPay.sumOf { it.produit.prix * it.quantity }
            val invoice = Invoice(tableId = tableId, totalAmount = total)
            repository.addInvoice(invoice)
            itemsToPay.forEach { ticket ->
                repository.addInvoiceItem(
                    InvoiceItem(
                        invoiceId = invoice.id,
                        productId = ticket.produit.id,
                        quantity = ticket.quantity
                    )
                )
            }

            decrementStocks(itemsToPay)

            deleteTable(tableId)
            _tableItems.value = emptyList()
        }
    }

    fun confirmerVenteTable(tableId: UUID) {
        viewModelScope.launch {
            payTable(tableId)
        }
    }


    private val _invoicesWithDetails = MutableStateFlow<List<InvoiceWithDetails>>(emptyList())
    val invoicesWithDetails: StateFlow<List<InvoiceWithDetails>> = _invoicesWithDetails.asStateFlow()
    data class InvoiceWithDetails(
        val invoice: Invoice,
        val items: List<Ticket>
    )


    // Historique
    fun loadHistory() {
        viewModelScope.launch {
            val allInvoices = repository.getAllInvoices()
            val details = allInvoices.map { invoice ->
                val items = repository.getInvoiceItems(invoice.id)
                val tickets = items.mapNotNull { item ->
                    repository.getProduitById(item.productId)?.let { product ->
                        Ticket(product, item.quantity)
                    }
                }
                InvoiceWithDetails(invoice, tickets)
            }
            _invoicesWithDetails.value = details
        }
    }


    private val _ventesWithDetails = MutableStateFlow<List<VenteWithDetails>>(emptyList())
    val ventesWithDetails: StateFlow<List<VenteWithDetails>> = _ventesWithDetails.asStateFlow()


    fun loadVentesHistory() {
        viewModelScope.launch {
            repository.getAllVentes().collect { allVentes ->
                val onlyCart = allVentes.filter { it.tableId ==  UUID.fromString("22222222-0000-2222-2222-222222222222") &&  !it.isDeleted}
                val details = onlyCart.map { vente ->
                    // Collecter les lignes de cette vente
                    val lignes = repository.getLignesForVente(vente.id).first()
                    val tickets = lignes.mapNotNull { ligne ->
                        repository.getProduitById(ligne.produitId)?.let { produit ->
                            Ticket(produit, ligne.quantity)
                        }
                    }
                    VenteWithDetails(vente, tickets)
                }
                _ventesWithDetails.value = details
            }
        }
    }


    private val _ventesTablesWithDetails = MutableStateFlow<List<VenteWithDetails>>(emptyList())
    val ventesTablesWithDetails: StateFlow<List<VenteWithDetails>> = _ventesTablesWithDetails.asStateFlow()

    fun loadVentesTablesHistory() {
        viewModelScope.launch {
            repository.getAllVentes().collect { allVentes ->
                val onlyTables = allVentes.filter { it.tableId != null && it.tableId != sentinelPanier && !it.isDeleted }
                val details = onlyTables.map { vente ->
                    val lignes = repository.getLignesForVente(vente.id).first()
                    val tickets = lignes.mapNotNull { l ->
                        repository.getProduitById(l.produitId)?.let { p -> Ticket(p, l.quantity) }
                    }
                    VenteWithDetails(vente, tickets)
                }
                _ventesTablesWithDetails.value = details
            }
        }
    }
    private suspend fun updateStocksForTickets(tickets: List<Ticket>) {
        tickets.forEach { ticket ->
            val produitActuel = repository.getProduitById(ticket.produit.id)
            if (produitActuel != null) {
                val newStock = (produitActuel.stock - ticket.quantity).coerceAtLeast(0)
                if (newStock != produitActuel.stock) {
                    repository.updateProduit(
                        produitActuel.copy(
                            stock = newStock,
                            updatedAt = now(),
                            isDirty = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun decrementStocks(tickets: List<Ticket>) {
        tickets.forEach { ticket ->
            val produitActuel = repository.getProduitById(ticket.produit.id)
            if (produitActuel != null) {
                val newStock = (produitActuel.stock - ticket.quantity).coerceAtLeast(0)
                if (newStock != produitActuel.stock) {
                    repository.updateProduit(
                        produitActuel.copy(
                            stock = newStock,
                            updatedAt = now(),
                            isDirty = true
                        )
                    )
                }
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
                        database.venteDao(),
                        database.tableDao(),
                        database.invoiceDao(),
                        database.infosDao()
                    )
                    MenuViewModel(repository)
                }
            }
        }
    }
}
