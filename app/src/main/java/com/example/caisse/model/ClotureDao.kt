package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.Cloture
import java.util.UUID

@Dao
interface ClotureDao {
    @Query("""
    SELECT * FROM clotures
    WHERE dateCloture = :dateCloture AND type = :type
    LIMIT 1
""")
    suspend fun getClotureByDateAndType(dateCloture: String, type: String): Cloture?

    @Query("SELECT * FROM clotures")
    suspend fun getAllCloturesOnce(): List<Cloture>

    @Query("SELECT * FROM clotures WHERE idCloture = :id LIMIT 1")
    suspend fun getClotureById(id: UUID): Cloture?

    @Update
    suspend fun updateCloture(cloture: Cloture)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCloture(cloture: Cloture)

}