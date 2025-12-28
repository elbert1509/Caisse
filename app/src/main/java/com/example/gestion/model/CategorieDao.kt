package com.example.gestion.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gestion.data.Category
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategorieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)


    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM category")
    fun getAllCategory(): Flow<List<Category>>

    @Query("SELECT * FROM category")
    suspend fun getAllCategoryOnce(): List<Category> // one-shot pour le Worker

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun get(id: UUID): Category?


}