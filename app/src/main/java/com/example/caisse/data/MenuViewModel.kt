package com.example.caisse.data

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caisse.util.PasswordHasher
import com.example.caisse.util.formatTimestampToDate
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
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
import java.io.File
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
            repository.addCategory(Category(name = name, description = description, icon = icon, isDirty = true))
            loggerEvenement(TypeEvenement.AJOUT_CATEGORIE.name, "Catégorie ajoutée : $name")
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.softDeleteCategory(category.id)
            loggerEvenement(TypeEvenement.SUPPRESSION_CATEGORIE.name, "Catégorie supprimée : ${category.name} (id=${category.id})")
        }
    }

    fun renameCategory(id: UUID, newName: String) {
        viewModelScope.launch {
            val category = categories.value.find { it.id == id }
            category?.let {
                repository.updateCategory(it.copy(name = newName))
                loggerEvenement(TypeEvenement.MODIF_CATEGORIE.name, "Catégorie renommée : ${it.name} → $newName (id=$id)")
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
    fun addProduit(name: String, price: Double, categoryId: UUID, stock: Int = 12, imageUri: String?) {
        viewModelScope.launch {
            val newProduit = Produit(nom = name, prix = price, categoryId = categoryId, stock = stock, image = imageUri).copy(updatedAt = now(), isDirty = true)
            repository.addProduit(newProduit)
            loggerEvenement(TypeEvenement.AJOUT_PRODUIT.name, "Produit ajouté : $name | prix=$price | cat=$categoryId")
        }
    }

    fun updateProduit(produit: Produit) {
        viewModelScope.launch {
            repository.updateProduit(produit.copy(updatedAt = now(), isDirty = true))
            loggerEvenement(TypeEvenement.MODIF_PRODUIT.name, "Produit modifié : ${produit.nom} (id=${produit.id}) | prix=${produit.prix}")
        }
    }

    fun deleteProduit(produit: Produit) {
        viewModelScope.launch {
            repository.softDeleteProduit(produit.id)
            loggerEvenement(TypeEvenement.SUPPRESSION_PRODUIT.name, "Produit supprimé : ${produit.nom} (id=${produit.id})")
        }
    }

    // Infos

    fun addInfos(name: String, address: String, phone: String, siret : String,   email: String, logo: Int? = null,devise : String,initialPassword: String) {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(initialPassword, salt)
        viewModelScope.launch {
            // isDirty = true : sans ce flag, la fiche magasin n'était JAMAIS poussée vers le
            // cloud (pushInfos n'envoie que le dirty) -> les nouveaux appareils ne recevaient
            // ni les infos ni le mot de passe gestion.
            repository.insertInfos(ShopInfos(1,name, address, phone, email, siret = siret, logo, passwordHash = hash, passwordSalt = salt, devise = devise, updatedAt = now(), isDirty = true))
        }
    }
    fun updateInfos(name: String, address: String, phone: String, email: String, siret: String, logo: Int? = null, devise: String) {
        viewModelScope.launch {
            val existing = repository.getInfos() ?: return@launch
            // isDirty = true + updatedAt : une édition de la fiche doit se propager aux autres
            // appareils (elle restait locale auparavant).
            repository.updateInfos(existing.copy(name = name, address = address, phone = phone, email = email, siret = siret, logo = logo, devise = devise, updatedAt = now(), isDirty = true))
            loggerEvenement(TypeEvenement.MODIF_CONFIG.name, "Configuration mise à jour : name=$name | siret=$siret | devise=$devise")
        }
    }

   fun getInfos(): ShopInfos? {
        return runBlocking {
            repository.getInfos()
        }
    }

    /** Fiche magasin en continu : émet dès que la sync l'importe (cas du nouvel appareil). */
    fun observeInfos() = repository.observeInfos()

    fun supdatePassword(passwordHash: String, passwordSalt: String) {
        viewModelScope.launch {
            val existing = repository.getInfos() ?: return@launch
            repository.updateInfos(existing.copy(passwordHash = passwordHash, passwordSalt = passwordSalt, updatedAt = System.currentTimeMillis(), isDirty = true))
            loggerEvenement(TypeEvenement.MODIF_MOT_DE_PASSE.name, "Mot de passe modifié")
        }
    }


    // ---- VENDEURS ----
    fun addVendeur(vendeur: Vendeur) {
        viewModelScope.launch {
            val newVendeur = vendeur.copy(updatedAt = now(), isDirty = true)
            repository.addVendeur(newVendeur)
            loggerEvenement(TypeEvenement.AJOUT_VENDEUR.name, "Vendeur ajouté : ${vendeur.nom} (id=${vendeur.id})")
        }
    }

    fun deleteVendeur(vendeur: Vendeur) {
        viewModelScope.launch {
            repository.softDeleteVendeur(vendeur.id)
            loggerEvenement(TypeEvenement.SUPPRESSION_VENDEUR.name, "Vendeur supprimé : ${vendeur.nom} (id=${vendeur.id})")
        }
    }


    val ventes: StateFlow<List<Vente>> =
        repository.getAllVentes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun confirmerVente(vendeurId: UUID? = null) {
        viewModelScope.launch {
            val cartItems = cart.value
            if (cartItems.isEmpty()) return@launch

            val venteId = UUID.randomUUID()
            val ts      = now()
            val vente = Vente(
                id        = venteId,
                vendeurId = vendeurId,
                total     = totalPrice.value,
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
                    tauxTVA      = ticket.produit.tauxTVA,
                    updatedAt    = ts,
                    isDirty      = true
                )
            }

            // NF525 Axe B : transaction atomique avec hash + numéro de séquence
            val venteSignee = repository.insertVenteSecurisee(vente, lignes)

            loggerEvenement(
                type        = TypeEvenement.VENTE_VALIDEE.name,
                description = "Vente validée | seq=${venteSignee.sequenceNumber} | " +
                              "total=${venteSignee.total} | articles=${cartItems.sumOf { it.quantity }} | " +
                              "hash=${venteSignee.hash}"
            )
            decrementStocks(cartItems)
            clearCart()
        }
    }

    fun deleteVenteWithStock(vente: Vente) {
        viewModelScope.launch {
            val ts = now()
            // 1) Requête directe (suspend, pas Flow) — évite le bug Flow.first() vide
            val lignes = repository.getLignesForVenteOnce(vente.id)

            // 2) Rétablir les stocks pour chaque ligne
            lignes.forEach { l ->
                val p = repository.getProduitById(l.produitId)
                if (p != null) {
                    repository.updateProduit(p.copy(stock = p.stock + l.quantity, updatedAt = ts, isDirty = true))
                }
            }

            // 3) NF525 Axe A : soft-delete atomique
            repository.softDeleteVente(vente.id)
            repository.venteDao.softDeleteLignesForVente(vente.id, ts)

            loggerEvenement(
                type        = TypeEvenement.VENTE_ANNULEE.name,
                description = "Vente annulée | seq=${vente.sequenceNumber} | hash=${vente.hash} | montant=${vente.total}"
            )
        }
    }

    private val _selectedVente = MutableStateFlow<VenteWithDetails?>(null)
    val selectedVente: StateFlow<VenteWithDetails?> = _selectedVente

    fun loadVenteWithDetailsById(id: UUID) {
        viewModelScope.launch {
            _selectedVente.value = repository.getVenteWithDetailsById(id)
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

    // Flux réactif issu de Room : se met à jour automatiquement quand le SyncWorker (ou une action
    // locale) écrit des tables en base, sans rechargement manuel.
    val tables: StateFlow<List<AppTable>> = repository.getActiveTablesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    // Conservée pour compatibilité avec les appelants existants. Le flux `tables` étant désormais
    // réactif (Room Flow), aucun rechargement manuel n'est nécessaire.
    fun loadTables() { /* no-op : `tables` est un flux réactif */ }

    fun addTable(name: String) {
        viewModelScope.launch {
            repository.addTable(AppTable(name = name).copy(updatedAt = now(), isDirty = true))
            loggerEvenement(TypeEvenement.OUVERTURE_TABLE.name, "Table ouverte : $name")
            _tableItems.value = emptyList()
            loadTables()
        }
    }

    fun loadTableItems(tableId: UUID) {
        viewModelScope.launch {
            val items = repository.getTableItems(tableId).filter { !it.isDeleted && it.quantity > 0 }
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
            val table = tables.value.find { it.id == tableId }
            if (table != null) {
                repository.updateTable(table.copy(active = false, updatedAt = now(), isDirty = true))
                loggerEvenement(TypeEvenement.FERMETURE_TABLE.name, "Table fermée : ${table.name} (id=$tableId)")
                loadTables()
            }
        }
    }
    fun addProductToTable(productId: UUID, tableId: UUID) {
        viewModelScope.launch {
            val existingItem = repository.getTableItems(tableId).find { it.productId == productId }
            if (existingItem != null) {
                repository.updateProductInTable(
                    existingItem.copy(quantity = existingItem.quantity + 1, updatedAt = now(), isDirty = true)
                )
            } else {
                repository.addProductToTable(
                    TableItem(tableId = tableId, productId = productId, quantity = 1).copy(updatedAt = now(), isDirty = true)
                )
            }
            loadTableItems(tableId)
        }
    }



    fun updateTableItemQuantity(productId: UUID, tableId: UUID, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity > 0) {
                val item = repository.getTableItems(tableId).find { it.productId == productId }
                item?.let {
                    repository.updateProductInTable(it.copy(quantity = newQuantity, updatedAt = now(), isDirty = true))
                }
            } else {
                val item = repository.getTableItems(tableId).find { it.productId == productId }
                item?.let {
                    repository.updateProductInTable(
                        it.copy(
                            quantity = 0,
                            updatedAt = now(),
                            isDirty = true,
                            isDeleted = true
                        )
                    )
                }
               // repository.deleteProductFromTable(tableId, productId)
            }
            loadTableItems(tableId)
        }
    }

    fun getTableById(id: UUID): AppTable? {
        return tables.value.find { it.id == id }
    }
    fun payTable(tableId: UUID) {
        viewModelScope.launch {
            val itemsToPay = _tableItems.value
            if (itemsToPay.isEmpty()) return@launch

            val venteId = UUID.randomUUID()
            val ts      = now()
            val vente = Vente(
                id        = venteId,
                vendeurId = null,
                total     = itemsToPay.sumOf { it.produit.prix * it.quantity },
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
                    tauxTVA      = ticket.produit.tauxTVA,
                    updatedAt    = ts,
                    isDirty      = true
                )
            }
            // NF525 Axe B : transaction atomique
            val venteSignee = repository.insertVenteSecurisee(vente, lignes)

            loggerEvenement(
                type        = TypeEvenement.VENTE_VALIDEE.name,
                description = "Vente table encaissée | tableId=$tableId | seq=${venteSignee.sequenceNumber} | " +
                              "total=${venteSignee.total} | articles=${itemsToPay.sumOf { it.quantity }} | " +
                              "hash=${venteSignee.hash}"
            )

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

    // insertVenteSecurisee est maintenant dans VenteDao (@Transaction atomique)


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

    @RequiresApi(Build.VERSION_CODES.O)
    fun loggerEvenement(type: String, description: String, vendeurId: UUID? = null) {
        viewModelScope.launch {
            repository.loggerEvenement(type, description, vendeurId)
        }
    }

    fun getLogs() = repository.getAllLogs()

    @RequiresApi(Build.VERSION_CODES.O)
    fun clotureJournaliere(
        onSuccess: (Cloture) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cloture = repository.genererClotureJournaliere()
                loggerEvenement(
                    type = TypeEvenement.CLOTURE_JOURNALIERE.name,
                    description = "Clôture ${cloture.dateCloture} | CA=${cloture.chiffreAffaireBrut} | " +
                                  "TVA=${cloture.totalTVA} | ventes=${cloture.compteurVentes} | GT=${cloture.grandTotalCumule}"
                )
                onSuccess(cloture)
            } catch (e: Exception) {
                loggerEvenement(
                    type        = TypeEvenement.ERREUR_CLOTURE.name,
                    description = e.message ?: "Erreur inconnue"
                )
                onError(e.message ?: "Erreur inconnue")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCatalogueData()
        }
    }

    /** NF525 Axe B — Lance la vérification de la chaîne de hash et retourne les IDs de ventes rompues. */
    fun verifierIntegriteChaineVentes(onResult: (List<java.util.UUID>) -> Unit) {
        viewModelScope.launch {
            val rompues = repository.verifierIntegriteChaineVentes()
            onResult(rompues)
        }
    }

    // On observe l'état de la caisse en temps réel
    val caisseOuverte: StateFlow<Boolean> = repository.observeEtatCaisse()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Fonction pour ouvrir la caisse (déjà discutée, à ajouter si absente)
    @RequiresApi(Build.VERSION_CODES.O)
    fun ouvrirLaCaisse(vendeurId: UUID) {
        viewModelScope.launch {
            repository.ouvrirCaisse(vendeurId)
        }
    }

    // Fonction pour fermer la caisse
    @RequiresApi(Build.VERSION_CODES.O)
    fun fermerCaisse() {
        viewModelScope.launch {
            repository.fermerCaisse()
        }
    }

    // Dans MenuViewModel.kt
    fun exportVentesToCSV(context: Context) {
        viewModelScope.launch {
            val ventes = repository.venteDao.getAllVentesOnce() // Créez cette méthode dans le DAO
            val csvHeader = "ID;Date;Montant;Vendeur;Hashpre;Hash\n"
            val csvData = ventes.joinToString("\n") {
                "${it.id};${formatTimestampToDate(it.date)};${it.total};${it.vendeurId};${it.previousHash};${it.hash}"
            }
            shareFile(context, "export_ventes.csv", csvHeader + csvData)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportJETToCSV(context: Context) {
        viewModelScope.launch {
            val logs = repository.getAllLogsOnce() // Récupère tous les LogTechnique
            val csvHeader = "ID;Date;Type;Description;Signature\n"
            val csvData = logs.joinToString("\n") {
                val dateOriginale = java.time.LocalDateTime.parse(it.date)
                val dateFormatee = dateOriginale.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                "${it.id};${dateFormatee};${it.typeEvenement};${it.description};${it.empreinte}"
            }
            shareFile(context, "journal_technique.csv", csvHeader + csvData)
        }
    }

    private fun shareFile(context: Context, fileName: String, content: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager le fichier"))
    }



    // Room @Transaction+@Relation : fiable, auto-mis à jour, plus de construction manuelle
    val ventesWithDetails: StateFlow<List<VenteWithDetails>> =
        repository.getVentesCartWithDetails(sentinelPanier)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Compatibilité avec les appels LaunchedEffect dans HistoriqueScreen (devenu no-op)
    fun loadVentesHistory() {}

    val ventesTablesWithDetails: StateFlow<List<VenteWithDetails>> =
        repository.getVentesTablesWithDetails(sentinelPanier)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadVentesTablesHistory() {}
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
                        database.infosDao(),
                        database.logDao(),
                        database.clotureDao()
                    )
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

    var isAdminMode by mutableStateOf(false)
        private set

    fun unlockAdmin() {
        isAdminMode = true
    }

    fun lockAdmin() {
        isAdminMode = false
    }
}
