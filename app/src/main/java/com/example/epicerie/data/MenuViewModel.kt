package com.example.epicerie.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.epicerie.util.PasswordHasher
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            repository.softDeleteCategory(category.id)
        }
    }

    fun renameCategory(id: UUID, newName: String) {
        viewModelScope.launch {
            val category = categories.value.find { it.id == id }
            category?.let {
                repository.updateCategory(it.copy(name = newName)) // REPLACE grâce à onConflictStrategy
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
    fun addProduit(name: String, price: Double, categoryId: UUID,stock:Int = 12, photo : String? = null,barcode : String? = null) {

        viewModelScope.launch {
            val newProduit = Produit(nom = name, prix = price, categoryId = categoryId,stock = stock ,codeBarre = barcode,image = photo).copy(updatedAt = now(), isDirty = true)
            repository.insertProduit(newProduit)
            if (stock != 0) {
                repository.enregistrerMouvementStock(newProduit.id, stock, "Stock initial")
            }
        }
    }

    // ---- RECHERCHE PRODUITS ----
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredProduits: StateFlow<List<Produit>> =
        produits
            .combine(_searchQuery) { produits, query ->
                if (query.length < 2) {
                    emptyList()
                } else {
                    produits.filter {
                        it.isActive &&
                                !it.isDeleted &&
                                it.nom.contains(query, ignoreCase = true)
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun updateProduit(produit: Produit) {
        viewModelScope.launch {
            if (!produit.codeBarre.isNullOrBlank() && repository.countSameBarcode(
                barcode = produit.codeBarre,
                excludeId = produit.id
            ) > 0)
            {
               // Toast.makeText(context, "Ce code barre existe déjà", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (produit.stock == null) {
                Log.e("Produit", "Le stock ne peut pas être nul")
                return@launch
            }
            val ancien = repository.getProduitById(produit.id)
            repository.updateProduit(
                produit.copy(
                    updatedAt = now(),
                    isDirty = true
                )
            )
            val delta = produit.stock - (ancien?.stock ?: produit.stock)
            if (delta != 0) {
                repository.enregistrerMouvementStock(
                    produit.id, delta,
                    if (delta > 0) "Ajout manuel" else "Ajustement manuel"
                )
            }
        }
    }

    /** Détail stock (Gestion > Stock > produit) : Stock/Ajout/Vente/Reste sur les 7 derniers jours. */
    suspend fun getDetailStockProduit(produitId: UUID): List<CaisseRepository.JourStockDetail> =
        repository.getDetailStockProduit(produitId)
    fun deleteProduit(produit: Produit) {
        viewModelScope.launch {
            repository.softDeleteProduit(produit.id)
        }
    }

    // Infos

    fun addInfos(name: String, address: String, phone: String, email: String, logo: Int? = null,devise : String,initialPassword: String="1234", heureFermetureAuto: String = "05:00") {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(initialPassword, salt)
        viewModelScope.launch {
            // isDirty = true : sans ce flag, la fiche magasin n'est jamais poussée vers le cloud
            // (pushInfos n'envoie que le dirty) -> les autres pads ne la reçoivent pas.
            repository.insertInfos(ShopInfos(1,name, address, phone, email, logo, passwordHash = hash, passwordSalt = salt, devise = devise, heureFermetureAuto = heureFermetureAuto, updatedAt = now(), isDirty = true))
        }
    }
    fun updateInfos(name: String, address: String, phone: String, email: String, logo: Int? = null,devise : String, heureFermetureAuto: String? = null) {
        viewModelScope.launch {
            val existing = repository.getInfos()
                ?: return@launch // ou alors créer par défaut

            repository.updateInfos(
                existing.copy(
                    name = name,
                    address = address,
                    phone = phone,
                    email = email,
                    logo = logo,
                    devise = devise,
                    heureFermetureAuto = heureFermetureAuto ?: existing.heureFermetureAuto,
                    updatedAt = now(),
                    isDirty = true
                )
            )
        }
    }

    /**
     * Fiche magasin en mémoire, tenue à jour par Room. Dans un écran Compose, lire
     * `infos.collectAsState().value` pour être recomposé quand elle change.
     */
    val infos: StateFlow<ShopInfos?> = repository.observeInfos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Dernière fiche magasin connue, sans bloquer le thread appelant. */
    fun getInfos(): ShopInfos? = infos.value

    /** Fiche magasin en continu : émet dès que la sync l'importe (cas du nouvel appareil). */
    fun observeInfos() = repository.observeInfos()

    fun supdatePassword(passwordHash: String, passwordSalt: String) {
        viewModelScope.launch {
            val existing = repository.getInfos() ?: return@launch

            repository.updateInfos(
                existing.copy(
                    passwordHash = passwordHash,
                    passwordSalt = passwordSalt,
                    updatedAt = System.currentTimeMillis(),
                    isDirty = true
                )
            )
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
            repository.softDeleteVendeur(vendeur.id)
        }
    }


    val ventes: StateFlow<List<Vente>> =
        repository.getAllVentes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ---- NF525 : JET, ouverture/fermeture de caisse ----
    @RequiresApi(Build.VERSION_CODES.O)
    fun loggerEvenement(type: String, description: String, vendeurId: UUID? = null) {
        viewModelScope.launch {
            repository.loggerEvenement(type, description, vendeurId)
        }
    }

    fun getLogs() = repository.getAllLogs()

    val caisseOuverte: StateFlow<Boolean> = repository.observeEtatCaisse()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun ouvrirLaCaisse(vendeurId: UUID) {
        viewModelScope.launch {
            repository.ouvrirCaisse(vendeurId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fermerCaisse() {
        viewModelScope.launch {
            repository.fermerCaisse()
        }
    }

    fun deleteVente(vente: Vente) {
        viewModelScope.launch {
            repository.softDeleteVente(vente.id)
            repository.softDeleteLignesForVente(vente.id)
        }
    }


    fun confirmerVente(vendeurId: UUID? = null) {
        // Panier pris ET vidé en une seule opération atomique : un deuxième appui rapide sur
        // « Confirmer la commande » trouve un panier vide et ne crée pas une deuxième vente.
        val cartItems = _cart.getAndUpdate { emptyList() }
        if (cartItems.isEmpty()) return
        viewModelScope.launch {
            val venteId = UUID.randomUUID()
            val ts      = now()
            // Total calculé sur le panier pris (totalPrice est un flux qui peut ne pas être à jour).
            val vente = Vente(
                id        = venteId,
                vendeurId = vendeurId,
                total     = cartItems.sumOf { it.produit.prix * it.quantity },
                date      = ts,
                updatedAt = ts,
                isDirty   = true
            )

            val lignes = cartItems.map { ticket ->
                VenteLigne(
                    id           = UUID.randomUUID(),
                    venteId      = venteId,
                    produitId    = ticket.produit.id,
                    quantity     = ticket.quantity,
                    prixUnitaire = ticket.produit.prix,
                    sousTotal    = ticket.produit.prix * ticket.quantity,
                    updatedAt    = ts,
                    isDirty      = true
                )
            }

            // NF525 Axe B : transaction atomique avec hash + numéro de séquence + stock
            val venteSignee = try {
                repository.insertVenteSecurisee(vente, lignes)
            } catch (e: Exception) {
                // Rien n'a été écrit : on rend le panier pour pouvoir réessayer.
                _cart.value = cartItems
                loggerEvenement(TypeEvenement.ERREUR_SYSTEME.name, "Échec d'enregistrement de la vente : ${e.message}")
                return@launch
            }

            loggerEvenement(
                type        = TypeEvenement.VENTE_VALIDEE.name,
                description = "Vente validée | seq=${venteSignee.sequenceNumber} | " +
                              "total=${venteSignee.total} | articles=${cartItems.sumOf { it.quantity }} | " +
                              "hash=${venteSignee.hash}"
            )
        }
    }

    fun deleteVenteWithStock(vente: Vente) {
        viewModelScope.launch {
            // NF525 Axe A : soft-delete + stock rendu dans une seule transaction. null = vente
            // déjà annulée (double appui) : rien à refaire, le stock n'est pas rendu deux fois.
            repository.annulerVente(vente.id) ?: return@launch

            loggerEvenement(
                type        = TypeEvenement.VENTE_ANNULEE.name,
                description = "Vente annulée | seq=${vente.sequenceNumber} | hash=${vente.hash} | montant=${vente.total}"
            )
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
            repository.addTable(
                AppTable(name = name).copy(updatedAt = now(), isDirty = true)
            )
            _tableItems.value = emptyList()
            loadTables()
        }
    }

    fun loadTableItems(tableId: UUID) {
        viewModelScope.launch {
            val items = repository.getTableItems(tableId).filter { !it.isDeleted && it.quantity > 0 }
            // Une seule ligne par produit, même si d'anciens doublons existent en base.
            val tickets = items.groupBy { it.productId }.mapNotNull { (productId, rows) ->
                repository.getProduitById(productId)?.let { product ->
                    Ticket(product, rows.sumOf { it.quantity })
                }
            }
            _tableItems.value = tickets
        }
    }
    fun clearTableItems() {
        _tableItems.value = emptyList()
    }
    /**
     * Suppression d'une table par l'utilisateur. Si des articles n'ont pas été encaissés, la
     * facture est annulée : on la trace dans le JET avant de fermer la table.
     */
    fun annulerTable(tableId: UUID) {
        viewModelScope.launch {
            val items = repository.getTableItems(tableId).filter { !it.isDeleted && it.quantity > 0 }
            if (items.isNotEmpty()) {
                val lignes = items.groupBy { it.productId }.map { (productId, rows) ->
                    val produit = repository.getProduitById(productId)
                    val quantite = rows.sumOf { it.quantity }
                    Triple(produit?.nom ?: productId.toString(), quantite, (produit?.prix ?: 0.0) * quantite)
                }
                val tableName = repository.getTableById(tableId)?.name ?: tableId.toString()
                loggerEvenement(
                    type = TypeEvenement.ANNULATION_FACTURE.name,
                    description = "Table $tableName supprimée sans encaissement : " +
                        lignes.joinToString(", ") { "${it.second}x ${it.first}" } +
                        " | total=${lignes.sumOf { it.third }}"
                )
            }
            deleteTable(tableId)
        }
    }

    fun deleteTable(tableId: UUID) {
        viewModelScope.launch {
            val table = _tables.value.find { it.id == tableId }
            if (table != null) {
                repository.updateTable(
                    table.copy(active = false, updatedAt = now(), isDirty = true)
                )
                loadTables()
            }
        }
    }
    // Sérialise les modifications de quantité : deux appuis rapides ne doivent ni se perdre
    // ni créer deux lignes pour le même produit.
    private val tableItemsMutex = Mutex()

    fun addProductToTable(productId: UUID, tableId: UUID) =
        changeTableItemQuantity(productId, tableId, +1)

    /**
     * Ajoute [delta] à la quantité d'un produit sur une table, à partir de la base (et non de
     * l'affichage). Une ligne retombée à 0 est marquée supprimée (soft-delete) ; si le produit
     * est rajouté ensuite, la ligne est réactivée. Les doublons hérités sont fusionnés.
     */
    fun changeTableItemQuantity(productId: UUID, tableId: UUID, delta: Int) {
        viewModelScope.launch {
            tableItemsMutex.withLock {
                val rows = repository.getTableItems(tableId).filter { it.productId == productId }
                val active = rows.filter { !it.isDeleted && it.quantity > 0 }
                val newQuantity = (active.sumOf { it.quantity } + delta).coerceAtLeast(0)
                val ts = now()
                val keep = active.firstOrNull() ?: rows.firstOrNull()
                if (keep == null) {
                    if (newQuantity > 0) {
                        repository.addProductToTable(
                            TableItem(tableId = tableId, productId = productId, quantity = newQuantity)
                                .copy(updatedAt = ts, isDirty = true)
                        )
                    }
                } else {
                    repository.updateProductInTable(
                        keep.copy(quantity = newQuantity, isDeleted = newQuantity == 0, updatedAt = ts, isDirty = true)
                    )
                    active.filter { it.id != keep.id }.forEach { duplicate ->
                        repository.updateProductInTable(
                            duplicate.copy(quantity = 0, isDeleted = true, updatedAt = ts, isDirty = true)
                        )
                    }
                }
                val oldQuantity = active.sumOf { it.quantity }
                if (newQuantity < oldQuantity) {
                    val produit = repository.getProduitById(productId)
                    val tableName = repository.getTableById(tableId)?.name ?: tableId.toString()
                    val retire = oldQuantity - newQuantity
                    loggerEvenement(
                        type = TypeEvenement.ANNULATION_ARTICLE.name,
                        description = "Table $tableName : ${produit?.nom ?: productId} x$oldQuantity -> x$newQuantity" +
                            (if (newQuantity == 0) " (retiré de la facture)" else "") +
                            " | montant retiré=${(produit?.prix ?: 0.0) * retire}"
                    )
                }
            }
            loadTableItems(tableId)
        }
    }

    fun getTableById(id: UUID): AppTable? {
        return _tables.value.find { it.id == id }
    }
    /**
     * Encaissement d'une table. Articles relus en base (et non l'affichage, qui peut ne pas
     * contenir un article ajouté entre-temps par un autre pad), sous le même verrou que les
     * changements de quantité. La table est fermée dans ce verrou : un deuxième appui sur
     * « Valider » la trouve fermée et ne crée pas une deuxième vente.
     */
    fun payTable(tableId: UUID) {
        viewModelScope.launch {
            tableItemsMutex.withLock {
                val table = repository.getTableById(tableId)
                if (table == null || !table.active || table.isDeleted) return@withLock

                val itemsToPay = repository.getTableItems(tableId)
                    .filter { !it.isDeleted && it.quantity > 0 }
                    .groupBy { it.productId }
                    .mapNotNull { (productId, rows) ->
                        repository.getProduitById(productId)?.let { Ticket(it, rows.sumOf { r -> r.quantity }) }
                    }
                if (itemsToPay.isEmpty()) return@withLock

                val venteId = UUID.randomUUID()
                val ts      = now()
                val total   = itemsToPay.sumOf { it.produit.prix * it.quantity }
                val vente = Vente(
                    id        = venteId,
                    vendeurId = null,
                    total     = total,
                    date      = ts,
                    tableId   = tableId,
                    updatedAt = ts,
                    isDirty   = true
                )

                val lignes = itemsToPay.map { ticket ->
                    VenteLigne(
                        id           = UUID.randomUUID(),
                        venteId      = venteId,
                        produitId    = ticket.produit.id,
                        quantity     = ticket.quantity,
                        prixUnitaire = ticket.produit.prix,
                        sousTotal    = ticket.produit.prix * ticket.quantity,
                        updatedAt    = ts,
                        isDirty      = true
                    )
                }
                // NF525 Axe B : transaction atomique (vente + lignes + stock)
                val venteSignee = repository.insertVenteSecurisee(vente, lignes)

                loggerEvenement(
                    type        = TypeEvenement.VENTE_VALIDEE.name,
                    description = "Vente table encaissée | tableId=$tableId | seq=${venteSignee.sequenceNumber} | " +
                                  "total=${venteSignee.total} | articles=${itemsToPay.sumOf { it.quantity }} | " +
                                  "hash=${venteSignee.hash}"
                )

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

                repository.updateTable(table.copy(active = false, updatedAt = now(), isDirty = true))
                loggerEvenement(TypeEvenement.FERMETURE_TABLE.name, "Table fermée : ${table.name} (id=$tableId)")
            }
            loadTables()
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
    // ---- Import de catalogue client depuis Firestore ----
    // Permet d'ajouter un nouveau client (catégories + produits + vendeurs, images en URL
    // distante) sans recompiler/redéployer l'app : il suffit d'ajouter un document dans la
    // collection Firestore "catalogues_epicerie" (namespacée pour ne pas entrer en collision
    // avec les catalogues de com.example.caisse sur le même projet Firebase).
    data class CatalogueClient(val id: String, val label: String)

    data class CatalogueImportResult(
        val categories: List<Category>,
        val produits: List<Produit>,
        val vendeurs: List<Vendeur>
    )

    /** Liste les clients disponibles dans Firestore (collection "catalogues_epicerie"). */
    suspend fun listCataloguesClients(): List<CatalogueClient> {
        val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("catalogues_epicerie").get().await()
        return snap.documents.map { doc ->
            CatalogueClient(id = doc.id, label = doc.getString("label") ?: doc.id)
        }
    }

    /**
     * Récupère et parse le catalogue Firestore `catalogues_epicerie/{clientId}` :
     *   - categories: [{ key, name, description? }]
     *   - produits:   [{ nom, prix, categoryKey, stock?, imageUrl?, description?, codeBarre? }]
     *   - vendeurs:   [{ nom, prenom }]
     * `key`/`categoryKey` ne sont que des identifiants locaux au document, utilisés pour
     * relier chaque produit à sa catégorie ; de vrais UUID sont générés à l'import.
     */
    suspend fun fetchCatalogue(clientId: String): CatalogueImportResult {
        val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("catalogues_epicerie")
            .document(clientId)
            .get()
            .await()
        val data = doc.data ?: throw IllegalStateException("Catalogue introuvable : $clientId")

        @Suppress("UNCHECKED_CAST")
        val categoriesRaw = data["categories"] as? List<Map<String, Any?>> ?: emptyList()
        val categoryIdByKey = mutableMapOf<String, UUID>()
        val categories = categoriesRaw.mapNotNull { c ->
            val key = c["key"] as? String ?: return@mapNotNull null
            val id = UUID.randomUUID()
            categoryIdByKey[key] = id
            Category(
                id = id,
                name = c["name"] as? String ?: key,
                description = c["description"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        val produitsRaw = data["produits"] as? List<Map<String, Any?>> ?: emptyList()
        val produits = produitsRaw.mapNotNull { p ->
            val categoryId = (p["categoryKey"] as? String)?.let { categoryIdByKey[it] }
                ?: return@mapNotNull null
            Produit(
                nom = p["nom"] as? String ?: return@mapNotNull null,
                prix = (p["prix"] as? Number)?.toDouble() ?: 0.0,
                image = p["imageUrl"] as? String,
                categoryId = categoryId,
                stock = (p["stock"] as? Number)?.toInt() ?: 0,
                description = p["description"] as? String,
                codeBarre = p["codeBarre"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        val vendeursRaw = data["vendeurs"] as? List<Map<String, Any?>> ?: emptyList()
        val vendeurs = vendeursRaw.map { v ->
            Vendeur(
                nom = v["nom"] as? String ?: "",
                prenom = v["prenom"] as? String ?: ""
            )
        }

        return CatalogueImportResult(categories, produits, vendeurs)
    }

    // Tuile « Donnée » de Gestion masquée par défaut (comme dans la caisse) : 7 appuis sur
    // « Accès autorisé » l'affichent jusqu'au redémarrage de l'app.
    var isAdminMode by mutableStateOf(false)
        private set

    fun unlockAdmin() {
        isAdminMode = true
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    val repository = CaisseRepository.create(context)
                    MenuViewModel(repository)
                }
            }
        }
    }

    // -------- Temps réel Tables (liste) --------
    private var tablesListener: ListenerRegistration? = null

    fun startRealtimeTables(uid: String) {
        stopRealtimeTables()
        val cloud = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        tablesListener = cloud.collection("users").document(uid)
            .collection("tables")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    viewModelScope.launch {
                        val remote = snap.documents.mapNotNull { doc ->
                            val m = doc.data ?: return@mapNotNull null
                            AppTable(
                                id = java.util.UUID.fromString(m["id"] as String),
                                name = m["name"] as String,
                                active = (m["active"] as? Boolean) ?: true,
                                updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                isDirty = false,
                                isDeleted = (m["isDeleted"] as? Boolean) ?: false
                            )
                        }
                        // upsert local (Room) + rafraîchir _tables
                        remote.forEach { r ->
                            val local = repository.getTableById(r.id) // ⚠️ pas seulement les actives
                            val shouldApply =
                                local == null ||
                                        r.updatedAt >= local.updatedAt ||           // version plus récente
                                        r.active != local.active ||                 // état actif changé → applique
                                        r.isDeleted != local.isDeleted              // statut supprimé changé → applique

                            if (shouldApply) {
                                repository.upsertTable(r.copy(isDirty = false))
                            }
                        }
                        loadTables()
                    }
                }
            }
    }

    fun stopRealtimeTables() {
        tablesListener?.remove()
        tablesListener = null
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {

        val shop = getInfos() ?: return Result.failure(Exception("Aucune config"))

        val isValid = PasswordHasher.verify(
            inputPassword = oldPassword,
            storedHash = shop.passwordHash,
            storedSalt = shop.passwordSalt
        )

        if (!isValid) {
            return Result.failure(Exception("Ancien mot de passe incorrect"))
        }

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hash(newPassword, newSalt)

        supdatePassword(
            passwordHash = newHash,
            passwordSalt = newSalt
        )

        return Result.success(Unit)
    }

    // ---- VOITURES ----

    private val _voitures = MutableStateFlow<List<Voiture>>(emptyList())
    val voitures: StateFlow<List<Voiture>> =
        repository.getAllVoitures().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // ⚡ Charger les tables dès que le ViewModel est instancié
        loadTables()

        // ✅ Charger les voitures dès que le ViewModel est instancié
        loadVoitures()
    }

    fun loadVoitures() {
        viewModelScope.launch {
            // Si tu as déjà repository.getAllVoitures() en Flow, on peut faire collect.
            // Mais comme pour tables tu fais un "once", je garde la même logique.
            // 👉 Donc il faut ajouter repository.getAllVoituresOnce() OU utiliser collect ici.

            repository.getAllVoitures().collect { list ->
                _voitures.value = list
            }
        }
    }

    fun addVoiture(name: String) {
        viewModelScope.launch {
            val newVoiture = Voiture(
                name = name.trim()
            ).copy(updatedAt = now(), isDirty = true)

            repository.addVoiture(newVoiture)
            // refresh automatique via collect() si loadVoitures() tourne
        }
    }

    fun deleteVoiture(voitureId: UUID) {
        viewModelScope.launch {
            val v = _voitures.value.find { it.id == voitureId } ?: return@launch
            // Soft delete comme tu fais pour sync
            repository.updateVoiture(
                v.copy(
                    isDeleted = true,
                    isDirty = true,
                    updatedAt = now()
                )
            )
        }
    }

    fun renameVoiture(id: UUID, newName: String) {
        viewModelScope.launch {
            val v = _voitures.value.find { it.id == id } ?: return@launch
            repository.updateVoiture(
                v.copy(
                    name = newName.trim(),
                    isDirty = true,
                    updatedAt = now()
                )
            )
        }
    }

    fun recettesByVoiture(voitureId: UUID): StateFlow<List<Recette>> {
        return repository.getRecettesByVoiture(voitureId)
            .map { list -> list.filter { !it.isDeleted }.sortedByDescending { it.date } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
    val allRecettes: StateFlow<List<Recette>> =
        repository.getAllRecettes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addRecetteForVoiture(
        voitureId: UUID,
        name: String,
        amount: Double,
        isRecette: Boolean,

    ) {
        viewModelScope.launch {
            val recette = Recette(
                name = name.trim(),
                voitureId = voitureId,
                amount = amount,
                isRecette = isRecette,
            ).copy(updatedAt = now(), isDirty = true)

            repository.addRecette(recette)
        }
    }

    fun deleteRecette(recetteId: UUID) {
        viewModelScope.launch {
            val r = repository.getRecetteById(recetteId) ?: return@launch
            repository.updateRecette(
                r.copy(
                    isDeleted = true,
                    isDirty = true,
                    updatedAt = now()
                )
            )
        }
    }

    fun getVoitureById(id: UUID): Voiture? {
        return _voitures.value.find { it.id == id }
    }



}
