package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.Category
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategorieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    // NF525 Axe A : soft delete — les catégories sont conservées pour l'historique
    @Query("UPDATE category SET isDeleted = 1, isDirty = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDeleteCategory(id: UUID, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM category WHERE isDeleted = 0")
    fun getAllCategory(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE isDeleted = 0")
    suspend fun getAllCategoryOnce(): List<Category>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun get(id: UUID): Category?

    // NF525 : suppression physique de masse remplacée par soft-delete
    @Query("UPDATE category SET isDeleted = 1, isDirty = 1, updatedAt = :ts")
    suspend fun softDeleteAllCategories(ts: Long = System.currentTimeMillis())
}