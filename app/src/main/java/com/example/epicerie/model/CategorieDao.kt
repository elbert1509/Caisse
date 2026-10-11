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

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun get(id: UUID): Category?


}