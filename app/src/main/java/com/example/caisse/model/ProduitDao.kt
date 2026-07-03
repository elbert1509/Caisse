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

    // Sync : ne baisse le flag dirty QUE si la ligne n'a pas été modifiée depuis sa lecture
    // (sinon une modification faite pendant le push serait perdue sans jamais être poussée).
    @Query("UPDATE Produit SET isDirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun clearDirty(id: UUID, updatedAt: Long)

    @Query("SELECT * FROM produit WHERE isDeleted = 0")
    fun getAllProduits(): Flow<List<Produit>>

    @Query("SELECT * FROM produit WHERE id = :id")
    suspend fun getProduitById(id: UUID): Produit?

    @Query("SELECT * FROM produit WHERE isDeleted = 0")
    suspend fun getAllProduitsOnce(): List<Produit>

    // Sync : TOUS les produits y compris supprimés (soft-delete), pour propager la suppression
    @Query("SELECT * FROM produit")
    suspend fun getAllProduitsForSync(): List<Produit>

    // NF525 : suppression physique de masse remplacée par soft-delete.
    // isDirty = 0 VOLONTAIREMENT : un reset de masse reste LOCAL et ne se propage pas vers le cloud
    // (sinon un "Effacer toutes les données" sur un appareil viderait tous les autres). Seules les
    // suppressions UNITAIRES (softDeleteProduit, isDirty=1) sont propagées.
    @Query("UPDATE Produit SET isDeleted = 1, isDirty = 0, updatedAt = :ts")
    suspend fun softDeleteAllProduits(ts: Long = System.currentTimeMillis())
}
