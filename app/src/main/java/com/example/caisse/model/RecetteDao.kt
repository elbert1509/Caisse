package com.example.caisse.model

import androidx.room.*
import com.example.caisse.data.Recette
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface RecetteDao {

    @Query("SELECT * FROM Recette WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllRecettes(): Flow<List<Recette>>

    @Query("SELECT * FROM Recette WHERE id = :id LIMIT 1")
    suspend fun getRecetteById(id: UUID): Recette?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecette(recette: Recette)

    @Update
    suspend fun updateRecette(recette: Recette)

    @Delete
    suspend fun deleteRecette(recette: Recette)

    @Query("""
        UPDATE Recette
        SET isDeleted = 1, isDirty = 1, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun softDeleteRecette(id: UUID, updatedAt: Long = System.currentTimeMillis())

    // Recettes d’une voiture
    @Query("""
        SELECT * FROM Recette
        WHERE isDeleted = 0 AND VoitureId = :voitureId
        ORDER BY date DESC
    """)
    fun getRecettesByVoiture(voitureId: UUID): Flow<List<Recette>>

    // Période
    @Query("""
        SELECT * FROM Recette
        WHERE isDeleted = 0 AND date BETWEEN :start AND :end
        ORDER BY date DESC
    """)
    fun getRecettesBetween(start: Long, end: Long): Flow<List<Recette>>

    @Query("SELECT * FROM Recette WHERE isDirty = 1")
    suspend fun getDirtyRecettesOnce(): List<Recette>

    @Query("SELECT * FROM Recette")
    suspend fun getAllRecettesOnce(): List<Recette>
}
