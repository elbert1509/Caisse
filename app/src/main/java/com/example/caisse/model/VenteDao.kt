package com.example.caisse.model

import androidx.room.*
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
}
