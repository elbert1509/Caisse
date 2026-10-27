package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.caisse.data.MouvementStock
import java.util.UUID

@Dao
interface MouvementStockDao {

    @Insert
    suspend fun insert(mouvement: MouvementStock)

    @Query("""
        SELECT * FROM mouvements_stock
        WHERE produitId = :produitId AND date >= :start AND date < :end AND isDeleted = 0
    """)
    suspend fun getMouvementsForPeriod(produitId: UUID, start: Long, end: Long): List<MouvementStock>

    // ----- SYNC -----
    @Query("SELECT * FROM mouvements_stock")
    suspend fun getAllMouvementsOnce(): List<MouvementStock>

    @Query("SELECT * FROM mouvements_stock WHERE id = :id LIMIT 1")
    suspend fun getMouvementById(id: UUID): MouvementStock?

    @Query("UPDATE mouvements_stock SET isDirty = 0 WHERE id = :id")
    suspend fun markSynced(id: UUID)
}
