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
import com.example.caisse.util.SecurityUtils
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
    fun addProduit(name: String, price: Double, categoryId: UUID,stock:Int = 12, imageUri: String?) {
        viewModelScope.launch {
            val newProduit = Produit(nom = name, prix = price, categoryId = categoryId,stock = stock ,image = imageUri).copy(updatedAt = now(), isDirty = true)
            repository.addProduit(newProduit)
        }
    }


    fun updateProduit(produit: Produit) {
        viewModelScope.launch {
            repository.updateProduit(
                produit.copy(
                    updatedAt = now(),
                    isDirty = true
                )
            )
        }
    }
    fun deleteProduit(produit: Produit) {
        viewModelScope.launch {
            repository.deleteProduit(produit)
        }
    }

    // Infos

    fun addInfos(name: String, address: String, phone: String, siret : String,   email: String, logo: Int? = null,devise : String,initialPassword: String="1234") {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(initialPassword, salt)
        viewModelScope.launch {
            repository.insertInfos(ShopInfos(1,name, address, phone, email, siret = siret, logo, passwordHash = hash, passwordSalt = salt, devise = devise))
        }
    }
    fun updateInfos(name: String, address: String, phone: String, email: String,siret : String,   logo: Int? = null,devise : String) {
        viewModelScope.launch {
            val existing = repository.getInfos()
                ?: return@launch // ou alors créer par défaut

            repository.updateInfos(
                existing.copy(
                    name = name,
                    address = address,
                    phone = phone,
                    email = email,
                    siret = siret,
                    logo = logo,
                    devise = devise
                )
            )
        }
    }

   fun getInfos(): ShopInfos? {
        return runBlocking {
            repository.getInfos()
        }
    }

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
            insertVenteSecurisee(vente, lignes) // ✅ une seule transaction

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
                repository.updateTable(
                    table.copy(active = false, updatedAt = now(), isDirty = true)
                )
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
            insertVenteSecurisee(vente, lignes)

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

    suspend fun insertVenteSecurisee(vente: Vente, lignes: List<VenteLigne>) {
        // 1. Récupérer la dernière vente pour avoir son hash
        val lastVente = repository.getLastVente() // Il faudra ajouter cette méthode dans le DAO
        val prevHash = lastVente?.hash ?: "0000000000000000"

        // 2. Créer la vente avec le lien vers la précédente
        val venteAvecLien = vente.copy(previousHash = prevHash)

        // 3. Calculer le hash de la vente actuelle
        val finalHash = SecurityUtils.calculateHash(venteAvecLien, lignes)
        val venteSignee = venteAvecLien.copy(hash = finalHash)

        // 4. Enregistrer en base
        repository.insertVenteWithLignes(venteSignee, lignes)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun loggerEvenement(type: String, description: String, vendeurId: UUID? = null) {
        viewModelScope.launch {
            repository.loggerEvenement(type, description, vendeurId)
        }
    }

    fun getLogs() = repository.getAllLogs()

    fun clotureJournaliere(
        onSuccess: (Cloture) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cloture = repository.genererClotureJournaliere()

                // 👉 Log enrichi (beaucoup mieux)
                loggerEvenement(
                    type = "CLOTURE",
                    description = "Clôture ${cloture.dateCloture} | CA=${cloture.chiffreAffaireBrut} | ventes=${cloture.compteurVentes}"
                )

                onSuccess(cloture)

            } catch (e: Exception) {

                loggerEvenement(
                    type = "ERREUR_CLOTURE",
                    description = e.message ?: "Erreur inconnue"
                )

                onError(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCatalogueData()
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
    fun ouvrirLaCaisse(vendeurId: UUID) {
        viewModelScope.launch {
            repository.ouvrirCaisse(vendeurId)
        }
    }

    // Fonction pour fermer la caisse
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



    private val _ventesWithDetails = MutableStateFlow<List<VenteWithDetails>>(emptyList())
    val ventesWithDetails: StateFlow<List<VenteWithDetails>> = _ventesWithDetails.asStateFlow()


    fun loadVentesHistory() {
        viewModelScope.launch {
            repository.getAllVentes().collect { allVentes ->
                val onlyCart = allVentes.filter { it.tableId ==  UUID.fromString("22222222-0000-2222-2222-222222222222") &&  !it.isDeleted}
                val details = onlyCart.map { vente ->
                    val lignes = repository.getLignesForVente(vente.id).first()
                    val lignesAvecProduit = lignes.mapNotNull { ligne ->
                        repository.getProduitById(ligne.produitId)?.let { produit ->
                            VenteLigneWithProduit(
                                ligne = ligne,
                                produit = produit
                            )
                        }
                    }
                    VenteWithDetails(vente, lignesAvecProduit)
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
                    val lignesAvecProduit = lignes.mapNotNull { l ->
                        repository.getProduitById(l.produitId)?.let { p ->
                            VenteLigneWithProduit(
                                ligne = l,
                                produit = p
                            )
                        }
                    }
                    VenteWithDetails(vente, lignesAvecProduit)
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
                        database.infosDao(),
                        database.logDao()
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
