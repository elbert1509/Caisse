package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.Produit
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ProduitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduit(produit: Produit)

    @Update
    suspend fun updateProduit(produit: Produit)

    // NF525 Axe A : soft delete — les produits référencés par des ventes historiques sont conservés
    @Query("UPDATE Produit SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteProduit(id: UUID, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM produit WHERE isDeleted = 0")
    fun getAllProduits(): Flow<List<Produit>>

    @Query("SELECT * FROM produit WHERE id = :id")
    suspend fun getProduitById(id: UUID): Produit?

    @Query("SELECT * FROM produit WHERE isDeleted = 0")
    suspend fun getAllProduitsOnce(): List<Produit>

    // NF525 : suppression physique de masse remplacée par soft-delete
    @Query("UPDATE Produit SET isDeleted = 1, isDirty = 1, updatedAt = :ts")
    suspend fun softDeleteAllProduits(ts: Long = System.currentTimeMillis())
}
