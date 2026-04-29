package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.Vendeur
import kotlinx.coroutines.flow.Flow

@Dao
interface VendeurDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendeur(vendeur: Vendeur)

    @Update
    suspend fun updateVendeur(vendeur: Vendeur)

    // NF525 Axe A : soft delete — les opérateurs doivent être conservés
    @Query("UPDATE vendeur SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteVendeur(id: Int, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM Vendeur WHERE isDeleted = 0")
    fun getAllVendeur(): Flow<List<Vendeur>>

    @Query("SELECT * FROM Vendeur WHERE isDeleted = 0")
    fun getAllVendeursOnce(): List<Vendeur>

    @Query("SELECT * FROM vendeur WHERE id = :id")
    suspend fun getVendeurById(id: Int): Vendeur?

    // NF525 : suppression physique de masse remplacée par soft-delete de masse
    @Query("UPDATE vendeur SET isDeleted = 1, isDirty = 1, updatedAt = :ts")
    suspend fun softDeleteAllVendeurs(ts: Long = System.currentTimeMillis())
}