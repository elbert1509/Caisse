package com.example.epicerie.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.epicerie.model.CategorieDao
import com.example.epicerie.model.ClotureDao
import com.example.epicerie.model.InfosDao
import com.example.epicerie.model.InvoiceDao
import com.example.epicerie.model.LogDao
import com.example.epicerie.model.ProduitDao
import com.example.epicerie.model.RecetteDao
import com.example.epicerie.model.TableDao
import com.example.epicerie.model.VendeurDao
import com.example.epicerie.model.VenteDao
import com.example.epicerie.model.VoitureDao
import com.example.epicerie.util.FiscalHashUtils.calculateClotureHash
import com.example.epicerie.util.FiscalHashUtils.sha256
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
    private val voitureDao: VoitureDao,
    private val recetteDao: RecetteDao,
    private val logDao: LogDao,
    private val clotureDao: ClotureDao
) {
    // ----- CATEGORIES -----
    fun getAllCategories(): Flow<List<Category>> = categorieDao.getAllCategory()
    suspend fun addCategory(category: Category) = categorieDao.addCategory(category)
    // NF525 Axe A : seul le soft-delete est autorisé
    suspend fun softDeleteCategory(id: UUID) = categorieDao.softDeleteCategory(id)
    suspend fun getAllCategoriesOnce(): List<Category> = categorieDao.getAllCategoryOnce()
    suspend fun getCategoryById(id: UUID): Category? = categorieDao.get(id)
    suspend fun updateCategory(category: Category) = categorieDao.updateCategory(category)





    // ----- PRODUITS -----
    fun getAllProduits(): Flow<List<Produit>> = produitDao.getAllProduits()
    suspend fun addProduit(produit: Produit) = produitDao.insertProduit(produit)
    // NF525 Axe A : seul le soft-delete est autorisé
    suspend fun softDeleteProduit(id: UUID) = produitDao.softDeleteProduit(id)
    suspend fun getProduitById(id: UUID): Produit? = produitDao.getProduitById(id)
    suspend fun updateProduit(produit: Produit) = produitDao.updateProduit(produit)
    suspend fun getAllProduitsOnce(): List<Produit> = produitDao.getAllProduitsOnce()
    suspend fun insertProduit(produit: Produit) = produitDao.insertProduit(produit)
    suspend fun getProduitByBarcode(barcode: String): Produit? = produitDao.getProduitByBarcode(barcode)
    suspend fun countSameBarcode(barcode: String, excludeId: UUID): Int = produitDao.countSameBarcode(barcode, excludeId)





    // ----- VENDEURS -----
    fun getAllVendeurs(): Flow<List<Vendeur>> = vendeurDao.getAllVendeur()
    suspend fun addVendeur(vendeur: Vendeur) = vendeurDao.insertVendeur(vendeur)
    // NF525 Axe A : seul le soft-delete est autorisé
    suspend fun softDeleteVendeur(id: UUID) = vendeurDao.softDeleteVendeur(id)

    // --- VENTES ---
    fun getAllVentes() = venteDao.getAllVentes()
    fun getLignesForVente(venteId: UUID) = venteDao.getLignesForVente(venteId)
    suspend fun updateVente(vente: Vente) = venteDao.updateVente(vente)
    suspend fun insertVente(vente: Vente) = venteDao.insertVente(vente)
    suspend fun insertLigne(ligne: VenteLigne) = venteDao.insertLigne(ligne)
    // NF525 Axe A : seul le soft-delete est autorisé sur une vente
    suspend fun softDeleteVente(venteId: UUID) = venteDao.softDeleteVente(venteId)
    suspend fun softDeleteLignesForVente(venteId: UUID) = venteDao.softDeleteLignesForVente(venteId)
    suspend fun getLignesForVenteOnce(venteId: UUID) = venteDao.getLignesForVenteOnce(venteId)
    suspend fun insertVenteWithLignes(vente: Vente, lignes: List<VenteLigne>) =
        venteDao.insertVenteWithLignes(vente, lignes)

    // NF525 Axe B : insertion atomique avec hash et séquence — retourne la vente signée
    suspend fun insertVenteSecurisee(vente: Vente, lignes: List<VenteLigne>): Vente =
        venteDao.insertVenteSecurisee(vente, lignes)

    /** NF525 Axe B — Vérifie l'intégrité de toute la chaîne de hash des ventes. */
    suspend fun verifierIntegriteChaineVentes(): List<UUID> =
        venteDao.verifierIntegriteChaineVentes()

    fun getProductReportBetween(start: Long, end: Long) =
        venteDao.getProductReportBetween(start, end)

    fun getTotalRevenueBetween(start: Long, end: Long) =
        venteDao.getTotalRevenueBetween(start, end)





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

    // ----- VOITURE -----
    fun getAllVoitures() = voitureDao.getAllVoitures()
    suspend fun addVoiture(voiture: Voiture) = voitureDao.insertVoiture(voiture)
    suspend fun updateVoiture(voiture: Voiture) = voitureDao.updateVoiture(voiture)
    suspend fun deleteVoiture(voiture: Voiture) = voitureDao.deleteVoiture(voiture)
    suspend fun softDeleteVoiture(id: UUID) = voitureDao.softDeleteVoiture(id)
    suspend fun getVoitureById(id: UUID) = voitureDao.getVoitureById(id)
    suspend fun getDirtyVoituresOnce() = voitureDao.getDirtyVoituresOnce()

    // ----- RECETTE -----
    fun getAllRecettes() = recetteDao.getAllRecettes()
    fun getRecettesByVoiture(voitureId: UUID) = recetteDao.getRecettesByVoiture(voitureId)
    fun getRecettesBetween(start: Long, end: Long) = recetteDao.getRecettesBetween(start, end)
    suspend fun addRecette(recette: Recette) = recetteDao.insertRecette(recette)
    suspend fun updateRecette(recette: Recette) = recetteDao.updateRecette(recette)
    suspend fun deleteRecette(recette: Recette) = recetteDao.deleteRecette(recette)
    suspend fun softDeleteRecette(id: UUID) = recetteDao.softDeleteRecette(id)
    suspend fun getDirtyRecettesOnce() = recetteDao.getDirtyRecettesOnce()
    suspend fun getRecetteById(id: UUID): Recette? = recetteDao.getRecetteById(id)

    // ---------------------------------------------------------------------
    // NF525 : journal des événements techniques (JET), clôture journalière,
    // ouverture/fermeture de caisse.
    // ---------------------------------------------------------------------

    /** NF525 Axe B — Insère un événement dans le JET avec empreinte SHA-256 calculée. */
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

        val derniereCloture   = clotureDao.getLastCloture()
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
        val derniereCloture = clotureDao.getLastCloture()

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

    suspend fun getLastCloture(): Cloture? = clotureDao.getLastCloture()

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


}
