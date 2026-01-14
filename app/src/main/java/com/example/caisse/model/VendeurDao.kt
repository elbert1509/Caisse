package com.example.piece.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.piece.data.Vendeur
import kotlinx.coroutines.flow.Flow

@Dao
interface VendeurDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendeur(vendeur: Vendeur)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateVendeur(vendeur: Vendeur)
    @Delete
    suspend fun deleteVendeur(vendeur: Vendeur)

    @Query("SELECT * FROM Vendeur")
    fun getAllVendeur(): Flow<List<Vendeur>>
    @Query("SELECT * FROM Vendeur")
    fun getAllVendeursOnce(): List<Vendeur>

    @Query("SELECT * FROM vendeur WHERE id = :id")
    suspend fun getVendeurById(id: Int): Vendeur?
}