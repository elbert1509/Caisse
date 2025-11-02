package com.example.caisse.model

import androidx.room.*
import com.example.caisse.data.ProductReport
import com.example.caisse.data.ProductSale
import com.example.caisse.data.SalesData
import com.example.caisse.data.Vente
import com.example.caisse.data.VenteLigne
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface VenteDao {

    // ---- VENTES ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVente(vente: Vente)

    @Delete
    suspend fun deleteVente(vente: Vente)

    @Query("SELECT * FROM Vente ORDER BY date DESC")
    fun getAllVentes(): Flow<List<Vente>>

    // ---- LIGNES DE VENTE ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLigne(ligne: VenteLigne)

    @Query("SELECT * FROM VenteLigne WHERE venteId = :venteId")
    fun getLignesForVente(venteId: UUID): Flow<List<VenteLigne>>

    @Delete
    suspend fun deleteLigne(ligne: VenteLigne)

    // ---- STATS ----
    @Query("""
        SELECT strftime('%Y-%m-%d', date / 1000, 'unixepoch') as label, SUM(total) as amount
        FROM Vente
        WHERE date >= :startDate
        GROUP BY label
        ORDER BY label ASC
    """)
    fun getSalesSince(startDate: Long): Flow<List<SalesData>>

    @Query("""
        SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as label, SUM(total) as amount
        FROM Vente
        GROUP BY label
        ORDER BY label ASC
    """)
    fun getSalesByMonth(): Flow<List<SalesData>>

    @Query("""
    SELECT COALESCE(c.name, 'Sans catégorie') AS label,
           COALESCE(SUM(vl.quantity * p.prix), 0) AS amount
    FROM VenteLigne vl
    LEFT JOIN Produit p
         ON CAST(vl.produitId AS TEXT) = CAST(p.id AS TEXT)
    LEFT JOIN category c
         ON CAST(p.categoryId AS TEXT) = CAST(c.id AS TEXT)
    GROUP BY COALESCE(c.name, 'Sans catégorie')
    ORDER BY amount DESC
""")
    fun getSalesByCategory(): Flow<List<SalesData>>

    @Query("""
    SELECT p.nom AS productName, SUM(vl.quantity) AS totalQuantity
    FROM VenteLigne vl
    JOIN Produit p ON vl.produitId = p.id
    GROUP BY p.nom
    ORDER BY totalQuantity DESC
    LIMIT 10
""")
    fun getTopSellingProducts(): Flow<List<ProductSale>>

    @Query("SELECT SUM(total) FROM Vente WHERE date >= :startDate AND date < :endDate")
    fun getTotalSalesBetween(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0) FROM vente WHERE date >= :start")
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
    JOIN Produit p ON vl.produitId = p.id
    JOIN Vente v   ON vl.venteId = v.id
    WHERE v.date >= :start AND v.date < :end
    GROUP BY p.nom,p.stock
    ORDER BY revenue DESC
""")
    fun getProductReportBetween(start: Long, end: Long): Flow<List<ProductReport>>

    @Query("""
    SELECT COALESCE(SUM(vl.quantity * vl.prixUnitaire), 0)
    FROM VenteLigne vl
    INNER JOIN Vente v ON vl.venteId = v.id
    WHERE v.date >= :start AND v.date < :end
""")
    fun getTotalRevenueBetween(start: Long, end: Long): Flow<Double>



}
