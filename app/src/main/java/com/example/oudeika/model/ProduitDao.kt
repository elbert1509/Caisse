package com.example.oudeika.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.oudeika.data.Produit
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ProduitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // ou IGNORE si tu préfères
    suspend fun insertProduit(produit: Produit)

    @Update
    suspend fun updateProduit(produit: Produit)
    @Delete
    suspend fun deleteProduit(produit: Produit)

    @Query("SELECT * FROM produit")
    fun getAllProduits(): Flow<List<Produit>>
    @Query("SELECT * FROM produit WHERE id = :id")
    suspend fun getProduitById(id: UUID): Produit?

    @Query("SELECT * FROM produit")
    suspend fun getAllProduitsOnce(): List<Produit> // one-shot pour le Worker

    @Query("SELECT * FROM Produit WHERE codeBarre = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProduitByBarcode(barcode: String): Produit?
    @Query("SELECT COUNT(*) FROM Produit WHERE codeBarre = :barcode AND id != :excludeId")
    suspend fun countSameBarcode(barcode: String, excludeId: UUID): Int







}