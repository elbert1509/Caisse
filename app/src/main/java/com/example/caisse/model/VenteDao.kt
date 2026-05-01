package com.example.caisse.model

import androidx.room.*
import com.example.caisse.data.Cloture
import com.example.caisse.data.EtatCaisse
import com.example.caisse.data.ProductReport
import com.example.caisse.data.ProductSale
import com.example.caisse.data.SalesData
import com.example.caisse.data.Vente
import com.example.caisse.data.VenteLigne
import com.example.caisse.data.VenteWithDetails
import com.example.caisse.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface VenteDao {

    // ---- VENTES ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVente(vente: Vente)

    // NF525 : pas de suppression physique — soft delete uniquement
    @Query("UPDATE Vente SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteVente(id: UUID, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM Vente ORDER BY date DESC")
    fun getAllVentes(): Flow<List<Vente>>

    @Update
    suspend fun updateLigne(ligne: VenteLigne)

    // ---- LIGNES DE VENTE ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLigne(ligne: VenteLigne)

    @Query("SELECT * FROM VenteLigne WHERE venteId = :venteId")
    fun getLignesForVente(venteId: UUID): Flow<List<VenteLigne>>

    // NF525 : pas de suppression physique des lignes
    @Query("UPDATE VenteLigne SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE venteId = :venteId")
    suspend fun softDeleteLignesForVente(venteId: UUID, ts: Long = System.currentTimeMillis())

    // ---- STATS ----
    @Query("""
        SELECT strftime('%Y-%m-%d', date / 1000, 'unixepoch','localtime') as label, SUM(total) as amount
        FROM Vente
        WHERE date >= :startDate AND isDeleted = 0
        GROUP BY label
        ORDER BY label ASC
    """)
    fun getSalesSince(startDate: Long): Flow<List<SalesData>>

    @Query("""
        SELECT strftime('%Y-%m', date / 1000, 'unixepoch','localtime') as label, SUM(total) as amount
        FROM Vente
        WHERE isDeleted = 0
        GROUP BY label
        ORDER BY label ASC
    """)
    fun getSalesByMonth(): Flow<List<SalesData>>

    @Query("""
    SELECT COALESCE(c.name, 'Sans catégorie') AS label,
           COALESCE(SUM(vl.quantity * vl.prixUnitaire), 0) AS amount
    FROM VenteLigne vl
    INNER JOIN Vente v
        ON v.id = vl.venteId AND v.isDeleted = 0
    INNER JOIN Produit p
        ON CAST(vl.produitId AS TEXT) = CAST(p.id AS TEXT)
       AND p.isDeleted = 0
    LEFT JOIN category c
        ON CAST(p.categoryId AS TEXT) = CAST(c.id AS TEXT)
    WHERE vl.isDeleted = 0
    GROUP BY COALESCE(c.name, 'Sans catégorie')
    ORDER BY amount DESC
""")
    fun getSalesByCategory(): Flow<List<SalesData>>

    @Query("""
    SELECT COALESCE(p.nom, 'Produit inconnu') AS productName,
           COALESCE(SUM(vl.quantity), 0)      AS totalQuantity
    FROM VenteLigne vl
    LEFT JOIN Produit p
      ON CAST(vl.produitId AS TEXT) = CAST(p.id AS TEXT)
       AND p.isDeleted = 0
    WHERE vl.isDeleted = 0
    GROUP BY COALESCE(p.nom, 'Produit inconnu')
    ORDER BY totalQuantity DESC
    LIMIT 10
""")
    fun getTopSellingProducts(): Flow<List<ProductSale>>

    @Query("SELECT SUM(total) FROM Vente WHERE date >= :startDate AND date < :endDate AND isDeleted = 0")
    fun getTotalSalesBetween(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0) FROM vente WHERE date >= :start AND isDeleted = 0" )
    fun getTotalSalesSince(start: Long): Flow<Double>


    @Query("SELECT COUNT(*) FROM Vente") suspend fun countVente(): Int
    @Query("SELECT COUNT(*) FROM VenteLigne") suspend fun countVenteLigne(): Int
    @Query("SELECT COUNT(*) FROM Produit") suspend fun countProduit(): Int
    @Query("SELECT COUNT(*) FROM category") suspend fun countCategory(): Int

    @Query("""
    SELECT COUNT(*)
    FROM VenteLigne vl
    LEFT JOIN Produit p
         ON CAST(vl.produitId AS TEXT) = CAST(p.id AS TEXT)
""")
    suspend fun countJoinVLProduit(): Int

    @Query("""
    SELECT COUNT(*)
    FROM Produit p
    LEFT JOIN category c
         ON CAST(p.categoryId AS TEXT) = CAST(c.id AS TEXT)
""")
    suspend fun countJoinProduitCategory(): Int


    @Transaction
    suspend fun insertVenteWithLignes(vente: Vente, lignes: List<VenteLigne>) {
        insertVente(vente)
        for (l in lignes) insertLigne(l)
    }

    /**
     * NF525 Axe B — Transaction atomique garantissant la cohérence de la chaîne de hash
     * et la séquence ininterrompue des tickets.
     */
    @Transaction
    suspend fun insertVenteSecurisee(vente: Vente, lignes: List<VenteLigne>): Vente {
        val lastVente = getLastVente()
        val prevHash = lastVente?.hash ?: "0000000000000000"
        val nextSeq  = (lastVente?.sequenceNumber ?: 0L) + 1L

        val venteAvecLien = vente.copy(previousHash = prevHash, sequenceNumber = nextSeq)
        val finalHash     = SecurityUtils.calculateHash(venteAvecLien, lignes)
        val venteSignee   = venteAvecLien.copy(hash = finalHash)

        insertVente(venteSignee)
        for (l in lignes) insertLigne(l)
        return venteSignee
    }

    /** NF525 — Vérifie la cohérence de toute la chaîne de hash. Retourne les IDs rompus. */
    // ---- REQUÊTES FIABLES POUR L'HISTORIQUE ----

    // Requête directe (suspend, pas Flow) — évite le problème Flow.first() vide
    @Query("SELECT * FROM VenteLigne WHERE venteId = :venteId AND isDeleted = 0")
    suspend fun getLignesForVenteOnce(venteId: UUID): List<VenteLigne>

    // Room @Transaction + @Relation : Room génère le JOIN correctement, sans construction manuelle fragile
    @Transaction
    @Query("SELECT * FROM Vente WHERE tableId = :sentinelId AND isDeleted = 0 ORDER BY date DESC")
    fun getVentesCartWithDetails(sentinelId: UUID): Flow<List<VenteWithDetails>>

    @Transaction
    @Query("SELECT * FROM Vente WHERE tableId IS NOT NULL AND tableId != :sentinelId AND isDeleted = 0 ORDER BY date DESC")
    fun getVentesTablesWithDetails(sentinelId: UUID): Flow<List<VenteWithDetails>>

    @Transaction
    suspend fun verifierIntegriteChaineVentes(): List<UUID> {
        val toutes = getAllVentesOnce().sortedBy { it.sequenceNumber }
        val brisees = mutableListOf<UUID>()
        var prevHash = "0000000000000000"
        for (v in toutes) {
            if (v.isDeleted) continue
            if (v.previousHash != prevHash) brisees.add(v.id)
            prevHash = v.hash
        }
        return brisees
    }
    @Query("""
    SELECT p.nom AS productName,
           SUM(vl.quantity) AS totalQuantity,
           p.stock AS productStock,
           SUM(vl.quantity * vl.prixUnitaire) AS revenue
    FROM VenteLigne vl
    JOIN Produit p ON vl.produitId = p.id AND p.isDeleted = 0
    JOIN Vente v   ON vl.venteId = v.id AND v.isDeleted = 0
    WHERE vl.isDeleted = 0 AND v.date >= :start AND v.date < :end
    GROUP BY p.nom,p.stock
    ORDER BY revenue DESC
""")
    fun getProductReportBetween(start: Long, end: Long): Flow<List<ProductReport>>

    @Query("""
    SELECT COALESCE(SUM(vl.quantity * vl.prixUnitaire), 0)
    FROM VenteLigne vl
    INNER JOIN Vente v ON vl.venteId = v.id AND v.isDeleted = 0
    WHERE v.date >= :start AND v.date < :end
""")
    fun getTotalRevenueBetween(start: Long, end: Long): Flow<Double>

    @Update
    suspend fun updateVente(vente: Vente)

    @Transaction
    @Query("SELECT * FROM Vente WHERE id = :id LIMIT 1")
    suspend fun getVenteWithDetailsById(id: UUID): VenteWithDetails?

    @Query("SELECT * FROM Vente")
    suspend fun getAllVentesOnce(): List<Vente>

    @Query("SELECT * FROM Vente WHERE id = :id")
    suspend fun getVenteById(id: UUID): Vente?

    @Query("SELECT * FROM VenteLigne")
    suspend fun getAllVenteLignesOnce(): List<VenteLigne>

    @Query("SELECT * FROM VenteLigne WHERE id = :id")
    suspend fun getVenteLigneById(id: UUID): VenteLigne?

    @Query("SELECT * FROM vente ORDER BY date DESC LIMIT 1")
    suspend fun getLastVente(): Vente?

    /**
     * Récupère toutes les ventes d'une journée spécifique pour le calcul du Z de caisse.
     * NF525 : Nécessaire pour l'intégrité des calculs de clôture.
     */
    @Query("SELECT * FROM vente WHERE date LIKE :date || '%' AND isDeleted = 0")
    suspend fun getVentesByDate(date: String): List<Vente>

    /**
     * Récupère la dernière clôture effectuée.
     * NF525 : Utilisé pour récupérer le 'Grand Total Cumulé' précédent et assurer la continuité.
     */
    @Query("SELECT * FROM clotures ORDER BY dateCloture DESC LIMIT 1")
    suspend fun getLastCloture(): Cloture?

    /**
     * Insère une nouvelle clôture (Z de caisse).
     * NF525 : Une fois insérée, cette donnée est inaltérable.
     */

    @Query("""
    SELECT * FROM Vente
    WHERE date >= :startOfDay
      AND date < :endOfDay
      AND isDeleted = 0
""")
    suspend fun getVentesByPeriod(startOfDay: Long, endOfDay: Long): List<Vente>



    @Query("SELECT * FROM etat_caisse WHERE id = 1")
    suspend fun getEtatCaisse(): EtatCaisse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateEtatCaisse(etat: EtatCaisse)
    @Query("SELECT * FROM etat_caisse WHERE id = 1")
    fun observeEtatCaisse(): Flow<EtatCaisse?>

}
