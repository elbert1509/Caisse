package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.caisse.data.Vendeur
import kotlinx.coroutines.flow.Flow

@Dao
interface VendeurDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendeur(vendeur: Vendeur)

    @Delete
    suspend fun deleteVendeur(vendeur: Vendeur)

    @Query("SELECT * FROM Vendeur")
    fun getAllVendeur(): Flow<List<Vendeur>>

}