package com.example.caisse.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.caisse.model.CategorieDao
import com.example.caisse.model.ClotureDao
import com.example.caisse.model.InfosDao
import com.example.caisse.model.InvoiceDao
import com.example.caisse.model.LogDao
import com.example.caisse.model.MouvementStockDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.TableDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao
import com.example.caisse.util.FiscalHashUtils.calculateClotureHash
import com.example.caisse.util.FiscalHashUtils.sha256
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
    private val logDao: LogDao,
    private val clotureDao: ClotureDao,
    private val mouvementStockDao: MouvementStockDao
) {
    companion object {
        /** Repository hors ViewModel (workers, journal des crashs). */
        fun create(context: android.content.Context): CaisseRepository {
            val db = CaisseDataBase.getDatabase(context)
            return CaisseRepository(
                db.categorieDao(), db.produitDao(), db.vendeurDao(), db.venteDao(), db.tableDao(),
                db.invoiceDao(), db.infosDao(), db.logDao(), db.clotureDao(), db.mouvementStockDao()
            )
        }
    }

    // ----- CATEGORIES -----
    fun getAllCategories(): Flow<List<Category>> = categorieDao.getAllCategory()
    suspend fun addCategory(category: Category) = categorieDao.addCategory(category)
    suspend fun softDeleteCategory(id: UUID) = categorieDao.softDeleteCategory(id)
    suspend fun getAllCategoriesOnce(): List<Category> = categorieDao.getAllCategoryOnce()
    suspend fun getCategoryById(id: UUID): Category? = categorieDao.get(id)
    suspend fun updateCategory(category: Category) = categorieDao.updateCategory(category)

    // ----- PRODUITS -----
    fun getAllProduits(): Flow<List<Produit>> = produitDao.getAllProduits()
    suspend fun addProduit(produit: Produit) = produitDao.insertProduit(produit)
    suspend fun softDeleteProduit(id: UUID) = produitDao.softDeleteProduit(id)
    suspend fun getProduitById(id: UUID): Produit? = produitDao.getProduitById(id)
    suspend fun updateProduit(produit: Produit) = produitDao.updateProduit(produit)
    suspend fun getAllProduitsOnce(): List<Produit> = produitDao.getAllProduitsOnce()
    suspend fun insertProduit(produit: Produit) = produitDao.insertProduit(produit)

    // ----- DÉTAIL STOCK (Gestion > Stock > produit) -----
    suspend fun enregistrerMouvementStock(produitId: UUID, delta: Int, motif: String) {
        if (delta == 0) return
        mouvementStockDao.insert(
            MouvementStock(produitId = produitId, delta = delta, motif = motif, isDirty = true)
        )
    }

    data class JourStockDetail(
        val dateDebutJour: Long,
        val stockDebut: Int,
        val ajout: Int,
        val vente: Int,
        val ajustementNegatif: Int,
        val reste: Int
    )

    /**
     * Reconstitue Stock/Ajout/Vente/Reste pour les [nbJours] derniers jours (index 0 = aujourd'hui).
     * Les ventes sont recalculées depuis VenteLigne/Vente (toujours exactes). Les ajouts/ajustements
     * manuels viennent du journal `mouvements_stock`. On part du stock LIVE du produit (= "Reste"
     * du jour en cours) et on remonte le temps jour par jour : Reste(j-1) = Stock(j).
     */
    suspend fun getDetailStockProduit(produitId: UUID, nbJours: Int = 7): List<JourStockDetail> {
        val produit = produitDao.getProduitById(produitId) ?: return emptyList()
        val cal = java.util.Calendar.getInstance()
        var resteCourant = produit.stock
        val jours = mutableListOf<JourStockDetail>()

        for (i in 0 until nbJours) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24L * 60 * 60 * 1000

            val vente = venteDao.getQuantiteVendue(produitId, startOfDay, endOfDay)
            val mouvements = mouvementStockDao.getMouvementsForPeriod(produitId, startOfDay, endOfDay)
            val ajout = mouvements.filter { it.delta > 0 }.sumOf { it.delta }
            val ajustementNegatif = mouvements.filter { it.delta < 0 }.sumOf { -it.delta }

            val reste = resteCourant
            // Reste = StockDébut + Ajout - Vente - Ajustement  ⇒  StockDébut = Reste - Ajout + Vente + Ajustement
            val stockDebut = reste - ajout + vente + ajustementNegatif

            jours.add(
                JourStockDetail(
                    dateDebutJour = startOfDay,
                    stockDebut = stockDebut,
                    ajout = ajout,
                    vente = vente,
                    ajustementNegatif = ajustementNegatif,
                    reste = reste
                )
            )
            resteCourant = stockDebut // chaînage : le "Reste" d'hier = le "Stock" d'aujourd'hui
        }
        return jours
    }

    // ----- VENDEURS -----
    fun getAllVendeurs(): Flow<List<Vendeur>> = vendeurDao.getAllVendeur()
    suspend fun addVendeur(vendeur: Vendeur) = vendeurDao.insertVendeur(vendeur)
    suspend fun softDeleteVendeur(id: UUID) = vendeurDao.softDeleteVendeur(id)

    // --- VENTES ---
    fun getAllVentes() = venteDao.getAllVentes()
    fun getLignesForVente(venteId: UUID) = venteDao.getLignesForVente(venteId)
    suspend fun getVenteWithDetailsById(id: UUID): VenteWithDetails? =
        venteDao.getVenteWithDetailsById(id)
    suspend fun updateVente(vente: Vente) = venteDao.updateVente(vente)
    suspend fun insertVente(vente: Vente) = venteDao.insertVente(vente)
    suspend fun insertLigne(ligne: VenteLigne) = venteDao.insertLigne(ligne)

    // NF525 Axe A : seul le soft-delete est autorisé sur une vente
    suspend fun softDeleteVente(venteId: UUID) = venteDao.softDeleteVente(venteId)

    // Requête directe pour éviter le problème Flow.first() vide dans deleteVenteWithStock
    suspend fun getLignesForVenteOnce(venteId: UUID) = venteDao.getLignesForVenteOnce(venteId)

    // Historique : Room @Transaction+@Relation gère le JOIN de manière fiable
    fun getVentesCartWithDetails(sentinelId: UUID) = venteDao.getVentesCartWithDetails(sentinelId)
    fun getVentesTablesWithDetails(sentinelId: UUID) = venteDao.getVentesTablesWithDetails(sentinelId)

    suspend fun insertVenteWithLignes(vente: Vente, lignes: List<VenteLigne>) =
        venteDao.insertVenteWithLignes(vente, lignes)

    // NF525 Axe B : insertion atomique avec hash et séquence — retourne la vente signée
    suspend fun insertVenteSecurisee(vente: Vente, lignes: List<VenteLigne>): Vente =
        venteDao.insertVenteSecurisee(vente, lignes)

    fun getProductReportBetween(start: Long, end: Long) =
        venteDao.getProductReportBetween(start, end)

    fun getTotalRevenueBetween(start: Long, end: Long) =
        venteDao.getTotalRevenueBetween(start, end)

    suspend fun getLastVente() = venteDao.getLastVente()

    /** NF525 Axe B — Vérifie l'intégrité de toute la chaîne de hash des ventes. */
    suspend fun verifierIntegriteChaineVentes(): List<UUID> =
        venteDao.verifierIntegriteChaineVentes()

    // Table methods
    suspend fun addTable(appTable: AppTable) = tableDao.addTable(appTable)
    suspend fun updateTable(appTable: AppTable) = tableDao.updateTable(appTable)
    suspend fun getActiveTables(): List<AppTable> = tableDao.getActiveTables()
    fun getActiveTablesFlow(): kotlinx.coroutines.flow.Flow<List<AppTable>> = tableDao.getActiveTablesFlow()
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
    fun observeInfos(): Flow<ShopInfos?> = infosDao.getInfosFlow()
    suspend fun updatePassword(passwordHash: String, passwordSalt: String) = infosDao.updatePassword(passwordHash, passwordSalt)

    /**
     * NF525 Axe B — Insère un événement dans le JET avec empreinte SHA-256 calculée.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loggerEvenement(type: String, description: String, vendeurId: UUID? = null) {
        val date = LocalDateTime.now().toString()
        val contenu = "$date|$type|$description|${vendeurId ?: ""}"
        val empreinte = sha256(contenu)
        val log = LogTechnique(
            date          = date,
            typeEvenement = type,
            description   = description,
            idVendeur     = vendeurId,
            empreinte     = empreinte
        )
        logDao.insertLog(log)
    }

    fun getAllLogs(): Flow<List<LogTechnique>> = logDao.getAllLogs()

    suspend fun getAllLogsOnce(): List<LogTechnique> = logDao.getAllLogsOnce()

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun genererClotureJournaliere(): Cloture {
        val dateAujourdhui = LocalDate.now().toString()
        val today = LocalDate.now()
        val zone  = java.time.ZoneId.systemDefault()

        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay   = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val clotureExistante = clotureDao.getClotureByDateAndType(
            dateCloture = dateAujourdhui,
            type        = "JOURNALIERE"
        )
        if (clotureExistante != null) {
            throw IllegalStateException("La clôture journalière du $dateAujourdhui existe déjà.")
        }

        val ventesDuJour = venteDao.getVentesByPeriod(startOfDay, endOfDay)
        val totalJour    = ventesDuJour.sumOf { it.total }

        // NF525 : calcul de TVA à partir du taux réel stocké dans chaque ligne
        val toutesLignes = venteDao.getAllVenteLignesOnce()
        val venteIds     = ventesDuJour.map { it.id }.toSet()
        val lignesDuJour = toutesLignes.filter { it.venteId in venteIds && !it.isDeleted }
        val totalTVA     = lignesDuJour.sumOf { l ->
            l.sousTotal * l.tauxTVA / (100.0 + l.tauxTVA)
        }

        val derniereCloture   = venteDao.getLastCloture()
        val nouveauGrandTotal = (derniereCloture?.grandTotalCumule ?: 0.0) + totalJour
        val previousHash      = derniereCloture?.hash ?: "0000000000000000"

        val clotureTemp = Cloture(
            dateCloture        = dateAujourdhui,
            type               = "JOURNALIERE",
            chiffreAffaireBrut = totalJour,
            totalTVA           = totalTVA,
            compteurVentes     = ventesDuJour.size,
            grandTotalCumule   = nouveauGrandTotal,
            hash               = ""
        )

        val finalHash = calculateClotureHash(clotureTemp, previousHash)
        val cloture   = clotureTemp.copy(hash = finalHash)

        clotureDao.insertCloture(cloture)
        return cloture
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun ouvrirCaisse(vendeurId: UUID) {
        // NF525 : vérifier que la caisse de la veille a bien été clôturée
        val hier = LocalDate.now().minusDays(1).toString()
        val clotureHier     = clotureDao.getClotureByDateAndType(hier, "JOURNALIERE")
        val derniereCloture = venteDao.getLastCloture()

        if (derniereCloture != null && clotureHier == null) {
            loggerEvenement(
                type        = TypeEvenement.ERREUR_SYSTEME.name,
                description = "Ouverture de caisse sans clôture de la veille ($hier). Vérification requise.",
                vendeurId   = vendeurId
            )
        }

        venteDao.updateEtatCaisse(
            EtatCaisse(isOuverte = true, dateOuverture = LocalDateTime.now().toString(), idVendeurOuverture = vendeurId)
        )
        loggerEvenement(
            type        = TypeEvenement.OUVERTURE_SESSION.name,
            description = "Ouverture de caisse par le vendeur $vendeurId",
            vendeurId   = vendeurId
        )
    }

    suspend fun estCaisseOuverte(): Boolean = venteDao.getEtatCaisse()?.isOuverte ?: false

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fermerCaisse() {
        venteDao.updateEtatCaisse(
            EtatCaisse(id = 1, isOuverte = false, dateOuverture = null, idVendeurOuverture = null)
        )
        loggerEvenement(
            type        = TypeEvenement.FERMETURE_SESSION.name,
            description = "Fermeture de session de caisse"
        )
    }

    fun observeEtatCaisse(): Flow<Boolean> {
        return venteDao.observeEtatCaisse().map { it?.isOuverte ?: false }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun executerClotureGlobale(): Cloture {
        val clotureResult = genererClotureJournaliere()
        venteDao.updateEtatCaisse(
            EtatCaisse(id = 1, isOuverte = false, dateOuverture = null, idVendeurOuverture = null)
        )
        loggerEvenement(
            type        = TypeEvenement.CLOTURE_ET_FERMETURE.name,
            description = "Clôture Z n°${clotureResult.idCloture} générée et session fermée. " +
                          "CA=${clotureResult.chiffreAffaireBrut} | GT=${clotureResult.grandTotalCumule} | hash=${clotureResult.hash}"
        )
        return clotureResult
    }

    /**
     * NF525 Axe A — Réinitialisation du catalogue par soft-delete uniquement.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun clearCatalogueData() {
        produitDao.softDeleteAllProduits()
        categorieDao.softDeleteAllCategories()
        vendeurDao.softDeleteAllVendeurs()
        loggerEvenement(
            type        = TypeEvenement.RESET_CATALOGUE.name,
            description = "Réinitialisation catalogue (soft-delete) — produits, catégories et vendeurs masqués."
        )
    }
}
