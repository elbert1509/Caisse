package com.example.caisse.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.caisse.model.CategorieDao
import com.example.caisse.model.InfosDao
import com.example.caisse.model.InvoiceDao
import com.example.caisse.model.LogDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.TableDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao
import com.example.caisse.util.FiscalHashUtils.calculateClotureHash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CaisseRepository(
    private val categorieDao: CategorieDao,
    private val produitDao: ProduitDao,
    private val vendeurDao: VendeurDao,
     val venteDao: VenteDao,
    private val tableDao: TableDao,
    private val invoiceDao: InvoiceDao,
    private val infosDao: InfosDao,
    private val logDao: LogDao
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





    // ----- VENDEURS -----
    fun getAllVendeurs(): Flow<List<Vendeur>> = vendeurDao.getAllVendeur()
    suspend fun addVendeur(vendeur: Vendeur) = vendeurDao.insertVendeur(vendeur)
    suspend fun deleteVendeur(vendeur: Vendeur) = vendeurDao.deleteVendeur(vendeur)

    // --- VENTES ---
    fun getAllVentes() = venteDao.getAllVentes()
    fun getLignesForVente(venteId: UUID) = venteDao.getLignesForVente(venteId)
    suspend fun getVenteWithDetailsById(id: UUID): VenteWithDetails? =
        venteDao.getVenteWithDetailsById(id)
    suspend fun updateVente(vente: Vente) = venteDao.updateVente(vente)
    suspend fun insertVente(vente: Vente) = venteDao.insertVente(vente)
    suspend fun insertLigne(ligne: VenteLigne) = venteDao.insertLigne(ligne)
    suspend fun deleteVente(vente: Vente) = venteDao.deleteVente(vente)
    suspend fun insertVenteWithLignes(vente: Vente, lignes: List<VenteLigne>) =
        venteDao.insertVenteWithLignes(vente, lignes)
    fun getProductReportBetween(start: Long, end: Long) =
        venteDao.getProductReportBetween(start, end)

    fun getTotalRevenueBetween(start: Long, end: Long) =
        venteDao.getTotalRevenueBetween(start, end)

    suspend fun getLastVente () = venteDao.getLastVente()





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


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loggerEvenement(type: String, description: String, vendeurId: UUID? = null) {
        val date = LocalDateTime.now().toString()
        // On peut aussi hasher le log pour plus de sécurité (similaire à l'axe B)
        val log = LogTechnique(
            date = date,
            typeEvenement = type,
            description = description,
            idVendeur = vendeurId,
            empreinte = "" // Optionnel: calcul du hash ici
        )
        logDao.insertLog(log)
    }

    fun getAllLogs(): Flow<List<LogTechnique>> = logDao.getAllLogs()

    suspend fun getAllLogsOnce(): List<LogTechnique> = logDao.getAllLogsOnce()

    suspend fun genererClotureJournaliere(): Cloture {
        val dateAujourdhui = LocalDate.now().toString()
        val today = LocalDate.now()
        val zone = java.time.ZoneId.systemDefault()


        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()


        val clotureExistante = venteDao.getClotureByDateAndType(
            dateCloture = dateAujourdhui,
            type = "JOURNALIERE"
        )

        if (clotureExistante != null) {
            throw IllegalStateException("La clôture journalière du $dateAujourdhui existe déjà.")

        }
        // 1. Calculer les totaux des ventes non clôturées
        val ventesDuJour =venteDao.getVentesByPeriod(startOfDay, endOfDay)
        val totalJour = ventesDuJour.sumOf { it.total }

        // 2. Récupérer la dernière clôture pour le cumul perpétuel
        val derniereCloture = venteDao.getLastCloture()
        val nouveauGrandTotal = (derniereCloture?.grandTotalCumule ?: 0.0) + totalJour
        val previousHash = derniereCloture?.hash ?: "0000000000000000" // Valeur par défaut pour la toute première clôture

        // 3. Créer l'objet temporaire pour le calcul
        val clotureTemp = Cloture(
            dateCloture = dateAujourdhui,
            type = "JOURNALIERE",
            chiffreAffaireBrut = totalJour,
            totalTVA = totalJour * 0.20, // À adapter selon vos taux
            compteurVentes = ventesDuJour.size,
            grandTotalCumule = nouveauGrandTotal,
            hash = "" // Calculer le hash comme dans l'Axe B
        )

        // 4. Calculer le Hash NF525
        val finalHash = calculateClotureHash(clotureTemp, previousHash)

        // 5. Créer la clôture finale signée
        val cloture = clotureTemp.copy(hash = finalHash)

        // 6. Enregistrer en base
        venteDao.insertCloture(cloture)

        return cloture
    }

    suspend fun ouvrirCaisse(vendeurId: UUID) {
        val date = LocalDateTime.now().toString()

        // 1. Mettre à jour l'état local
        venteDao.updateEtatCaisse(
            EtatCaisse(isOuverte = true, dateOuverture = date, idVendeurOuverture = vendeurId)
        )

        // 2. Inscrire l'événement dans le journal (IMPORTANT NF525)
        loggerEvenement(
            type = "OUVERTURE_SESSION",
            description = "Ouverture de caisse par le vendeur $vendeurId",
            vendeurId = vendeurId
        )
    }

    suspend fun estCaisseOuverte(): Boolean {
        return venteDao.getEtatCaisse()?.isOuverte ?: false
    }


    suspend fun fermerCaisse() {
        val date = LocalDateTime.now().toString()
        // 1. Mettre à jour l'état à "fermé"
        venteDao.updateEtatCaisse(
            EtatCaisse(id = 1, isOuverte = false, dateOuverture = null, idVendeurOuverture = null)
        )
        // 2. Log technique de l'événement
        loggerEvenement(
            type = "FERMETURE_SESSION",
            description = "Fermeture de session de caisse",
            vendeurId = null // Optionnel : passer l'ID du vendeur actuel
        )
    }
    fun observeEtatCaisse(): Flow<Boolean> {
        return venteDao.observeEtatCaisse().map { it?.isOuverte ?: false }
    }

    /**
     * Mutualisation de la clôture comptable (Z) et de la fermeture technique.
     * NF525 : Garantit que l'état de la caisse passe à "Fermé" dès que le rapport est scellé.
     */
    suspend fun executerClotureGlobale(): Cloture {
        // 1. Générer le rapport Z (Calcul, Signature/Hash, Insertion)
        val clotureResult = genererClotureJournaliere()

        // 2. Fermer techniquement la caisse
        val dateHeure = LocalDateTime.now().toString()

        // Mise à jour de l'état (ID 1 est fixe pour l'état unique)
        venteDao.updateEtatCaisse(
            EtatCaisse(
                id = 1,
                isOuverte = false,
                dateOuverture = null,
                idVendeurOuverture = null
            )
        )

        // 3. Loguer la fermeture dans le JET (Journal des Événements Techniques)
        loggerEvenement(
            type = "CLOTURE_ET_FERMETURE",
            description = "Clôture Z n°${clotureResult.idCloture} générée et session fermée.",
            vendeurId = null
        )

        return clotureResult
    }
    suspend fun clearCatalogueData() {
        produitDao.deleteAllProduits()
        categorieDao.deleteAllCategories()
        vendeurDao.deleteAllVendeurs()

        // Optionnel : Loguer l'action dans le journal technique
        loggerEvenement(
            type = "RESET_CATALOGUE",
            description = "Suppression complète des produits, catégories et vendeurs par l'utilisateur.",
            vendeurId = null
        )
    }



}
