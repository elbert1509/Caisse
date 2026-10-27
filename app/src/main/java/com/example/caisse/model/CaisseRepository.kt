package com.example.caisse.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.caisse.model.CategorieDao
import com.example.caisse.model.GENESIS_HASH
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
    private val mouvementStockDao: MouvementStockDao,
    /** ANDROID_ID de ce pad : posé sur ses ventes, ses clôtures et ses événements du journal. */
    val deviceId: String
) {
    companion object {
        /** Repository hors ViewModel (workers, journal des crashs). */
        fun create(context: android.content.Context): CaisseRepository {
            val db = CaisseDataBase.getDatabase(context)
            return CaisseRepository(
                db.categorieDao(), db.produitDao(), db.vendeurDao(), db.venteDao(), db.tableDao(),
                db.invoiceDao(), db.infosDao(), db.logDao(), db.clotureDao(), db.mouvementStockDao(),
                com.example.caisse.kiosk.SilentUpdater.deviceId(context)
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
    // Le stock est baissé dans la même transaction.
    suspend fun insertVenteSecurisee(vente: Vente, lignes: List<VenteLigne>): Vente =
        venteDao.insertVenteSecurisee(vente.copy(deviceId = deviceId), lignes)

    /** Annulation atomique (vente + lignes + stock rendu). null si déjà annulée ou introuvable. */
    suspend fun annulerVente(venteId: UUID): Vente? =
        venteDao.annulerVente(venteId, System.currentTimeMillis())

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
            empreinte     = empreinte,
            deviceId      = deviceId,
            isDirty       = true
        )
        logDao.insertLog(log)
    }

    fun getAllLogs(): Flow<List<LogTechnique>> = logDao.getAllLogs()

    suspend fun getAllLogsOnce(): List<LogTechnique> = logDao.getAllLogsOnce()

    /**
     * Dernière clôture de la chaîne de ce pad. Tant que ce pad n'a fait aucune clôture avec le
     * champ deviceId, sa chaîne continue sur la dernière clôture antérieure à ce champ.
     * Les clôtures des autres pads ne comptent pas : chaque pad a son Z, son grand total et sa
     * chaîne de hash (avant, un pad pouvait s'enchaîner sur la clôture d'un autre).
     */
    suspend fun derniereClotureDuPad(): Cloture? =
        clotureDao.getLastClotureForDevice(deviceId) ?: clotureDao.getLastClotureForDevice("")

    /**
     * Période couverte par la prochaine clôture journalière de ce pad.
     *  - [debut] : heure de la clôture précédente de ce pad (sinon ouverture de la session, sinon
     *    minuit) -> chaque vente appartient à exactement une clôture, même si la caisse reste
     *    ouverte après minuit.
     *  - [dateCloture] : journée d'exploitation = date d'ouverture de la session (une session
     *    ouverte le 23 et clôturée le 24 à 05:00 est la clôture du 23). Si ce pad a déjà la
     *    clôture de cette date (deuxième session le même jour), la date du jour.
     *  - Ventes comptées : celles de ce pad seulement (avant, le Z comptait aussi les ventes des
     *    autres pads -> CA compté deux fois). Ventes antérieures au champ deviceId (pad inconnu) :
     *    comptées une seule fois, dans le premier Z de ce pad après la mise à jour.
     */
    data class PeriodeCloture(
        val debut: Long,
        val dateCloture: LocalDate,
        val deviceId: String,
        val inclureAnciennes: Boolean
    ) {
        fun compte(v: Vente): Boolean =
            !v.isDeleted && v.date >= debut &&
                (v.deviceId == deviceId || (inclureAnciennes && v.deviceId.isEmpty()))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun periodeClotureEnCours(): PeriodeCloture {
        val zone = java.time.ZoneId.systemDefault()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val ouverture = venteDao.getEtatCaisse()?.dateOuverture
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

        val debut = derniereClotureDuPad()?.updatedAt?.takeIf { it in 1..now }
            ?: ouverture?.atZone(zone)?.toInstant()?.toEpochMilli()?.takeIf { it <= now }
            ?: today.atStartOfDay(zone).toInstant().toEpochMilli()

        var date = ouverture?.toLocalDate()?.takeIf { !it.isAfter(today) } ?: today
        if (date != today && clotureDao.getClotureForDevice(deviceId, date.toString(), "JOURNALIERE") != null) {
            date = today
        }
        val premierZ = clotureDao.getLastClotureForDevice(deviceId) == null
        return PeriodeCloture(debut, date, deviceId, inclureAnciennes = premierZ)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun genererClotureJournaliere(): Cloture {
        val periode = periodeClotureEnCours()
        val dateCloture = periode.dateCloture.toString()

        val clotureExistante = clotureDao.getClotureForDevice(
            deviceId    = deviceId,
            dateCloture = dateCloture,
            type        = "JOURNALIERE"
        )
        if (clotureExistante != null) {
            throw IllegalStateException("La clôture journalière du $dateCloture existe déjà.")
        }

        // Ventes de ce pad depuis sa clôture précédente, jusqu'à maintenant (inclus).
        val ventesDuJour = venteDao.getVentesByPeriod(periode.debut, System.currentTimeMillis() + 1)
            .filter(periode::compte)
        val totalJour    = ventesDuJour.sumOf { it.total }

        // NF525 : calcul de TVA à partir du taux réel stocké dans chaque ligne
        val toutesLignes = venteDao.getAllVenteLignesOnce()
        val venteIds     = ventesDuJour.map { it.id }.toSet()
        val lignesDuJour = toutesLignes.filter { it.venteId in venteIds && !it.isDeleted }
        val totalTVA     = lignesDuJour.sumOf { l ->
            l.sousTotal * l.tauxTVA / (100.0 + l.tauxTVA)
        }

        val derniereCloture   = derniereClotureDuPad()
        val nouveauGrandTotal = (derniereCloture?.grandTotalCumule ?: 0.0) + totalJour
        val previousHash      = derniereCloture?.hash ?: GENESIS_HASH

        val clotureTemp = Cloture(
            dateCloture        = dateCloture,
            type               = "JOURNALIERE",
            chiffreAffaireBrut = totalJour,
            totalTVA           = totalTVA,
            compteurVentes     = ventesDuJour.size,
            grandTotalCumule   = nouveauGrandTotal,
            hash               = "",
            deviceId           = deviceId,
            isDirty            = true
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
        val clotureHier     = clotureDao.getClotureForDevice(deviceId, hier, "JOURNALIERE")
            ?: clotureDao.getClotureForDevice("", hier, "JOURNALIERE")
        val derniereCloture = derniereClotureDuPad()

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
