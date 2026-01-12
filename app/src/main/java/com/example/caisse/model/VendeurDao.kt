package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.caisse.data.Vendeur
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

    @Query("SELECT * FROM vendeur")
    fun getAllVendeur(): Flow<List<Vendeur>>
    @Query("SELECT * FROM vendeur")
    fun getAllVendeursOnce(): List<Vendeur>

    @Query("SELECT * FROM vendeur WHERE id = :id")
    suspend fun getVendeurById(id: UUID): Vendeur?
}