package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.Vendeur
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface VendeurDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendeur(vendeur: Vendeur)

    @Update
    suspend fun updateVendeur(vendeur: Vendeur)

    // NF525 Axe A : soft delete — les opérateurs doivent être conservés
    @Query("UPDATE vendeur SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteVendeur(id: UUID, ts: Long = System.currentTimeMillis())

    // Sync : ne baisse le flag dirty QUE si la ligne n'a pas été modifiée depuis sa lecture
    // (sinon une modification faite pendant le push serait perdue sans jamais être poussée).
    @Query("UPDATE vendeur SET isDirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun clearDirty(id: UUID, updatedAt: Long)

    @Query("SELECT * FROM Vendeur WHERE isDeleted = 0")
    fun getAllVendeur(): Flow<List<Vendeur>>

    @Query("SELECT * FROM Vendeur WHERE isDeleted = 0")
    fun getAllVendeursOnce(): List<Vendeur>

    // Sync : tous les vendeurs y compris supprimés (soft-delete), pour propager la suppression
    @Query("SELECT * FROM Vendeur")
    fun getAllVendeursForSync(): List<Vendeur>

    @Query("SELECT * FROM vendeur WHERE id = :id")
    suspend fun getVendeurById(id: UUID): Vendeur?

    // NF525 : suppression physique de masse remplacée par soft-delete de masse.
    // isDirty = 0 VOLONTAIREMENT : reset de masse local, non propagé (voir ProduitDao.softDeleteAllProduits).
    @Query("UPDATE vendeur SET isDeleted = 1, isDirty = 0, updatedAt = :ts")
    suspend fun softDeleteAllVendeurs(ts: Long = System.currentTimeMillis())
}