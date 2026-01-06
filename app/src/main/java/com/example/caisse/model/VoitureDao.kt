package com.example.caisse.model

import androidx.room.*
import com.example.caisse.data.Voiture
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface VoitureDao {

    @Query("SELECT * FROM Voiture WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllVoitures(): Flow<List<Voiture>>

    @Query("SELECT * FROM Voiture WHERE id = :id LIMIT 1")
    suspend fun getVoitureById(id: UUID): Voiture?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiture(voiture: Voiture)

    @Update
    suspend fun updateVoiture(voiture: Voiture)

    // Soft delete (recommandé pour sync)
    @Query("""
        UPDATE Voiture
        SET isDeleted = 1, isDirty = 1, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun softDeleteVoiture(id: UUID, updatedAt: Long = System.currentTimeMillis())

    // Hard delete si tu veux vraiment supprimer de SQLite
    @Delete
    suspend fun deleteVoiture(voiture: Voiture)

    @Query("SELECT * FROM Voiture WHERE isDirty = 1")
    suspend fun getDirtyVoituresOnce(): List<Voiture>

    @Query("SELECT * FROM Voiture")
    suspend fun getAllVoituresOnce(): List<Voiture>


}
