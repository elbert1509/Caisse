package com.example.epicerie.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.epicerie.data.Vendeur
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface VendeurDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendeur(vendeur: Vendeur)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateVendeur(vendeur: Vendeur)
    @Delete
    suspend fun deleteVendeur(vendeur: Vendeur)

    // NF525 Axe A : à préférer à deleteVendeur pour rester cohérent avec les produits/catégories.
    @Query("UPDATE vendeur SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteVendeur(id: UUID, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM Vendeur")
    fun getAllVendeur(): Flow<List<Vendeur>>
    @Query("SELECT * FROM Vendeur")
    fun getAllVendeursOnce(): List<Vendeur>

    @Query("SELECT * FROM vendeur WHERE id = :id")
    suspend fun getVendeurById(id: UUID): Vendeur?
}