package com.example.epicerie.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.epicerie.data.Category
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategorieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)


    // NF525 Axe A : plus de suppression physique — uniquement soft delete.
    @Query("UPDATE category SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteCategory(id: UUID, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM category")
    fun getAllCategory(): Flow<List<Category>>

    @Query("SELECT * FROM category")
    suspend fun getAllCategoryOnce(): List<Category> // one-shot pour le Worker

    // Sync : TOUTES les catégories y compris supprimées (soft-delete), pour propager la suppression
    @Query("SELECT * FROM category")
    suspend fun getAllCategoriesForSync(): List<Category>

    // Sync : ne baisse le flag dirty QUE si la ligne n'a pas été modifiée depuis sa lecture
    // (sinon une modification faite pendant le push serait perdue sans jamais être poussée).
    @Query("UPDATE category SET isDirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun clearDirty(id: UUID, updatedAt: Long)

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun get(id: UUID): Category?


}