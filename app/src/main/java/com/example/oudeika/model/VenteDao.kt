package com.example.oudeika.model

import androidx.room.*
import com.example.oudeika.data.ProductReport
import com.example.oudeika.data.ProductSale
import com.example.oudeika.data.SalesData
import com.example.oudeika.data.Vente
import com.example.oudeika.data.VenteLigne
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface VenteDao {

    // ---- VENTES ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVente(vente: Vente)

    @Delete
    suspend fun deleteVente(vente: Vente)

    @Query("SELECT * FROM Vente ORDER BY date DESC")
    fun getAllVentes(): Flow<List<Vente>>

    @Update
    suspend fun updateLigne(ligne: VenteLigne)

    // ---- LIGNES DE VENTE ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLigne(ligne: VenteLigne)

    @Query("SELECT * FROM VenteLigne WHERE venteId = :venteId")
    fun getLignesForVente(venteId: UUID): Flow<List<VenteLigne>>

    @Delete
    suspend fun deleteLigne(ligne: VenteLigne)

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



    @Query("SELECT * FROM Vente")
    suspend fun getAllVentesOnce(): List<Vente>

    @Query("SELECT * FROM Vente WHERE id = :id")
    suspend fun getVenteById(id: UUID): Vente?

    @Query("SELECT * FROM VenteLigne")
    suspend fun getAllVenteLignesOnce(): List<VenteLigne>

    @Query("SELECT * FROM VenteLigne WHERE id = :id")
    suspend fun getVenteLigneById(id: UUID): VenteLigne?


}
