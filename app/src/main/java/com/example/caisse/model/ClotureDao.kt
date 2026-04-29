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

    // NF525 Axe A : seule la mise à jour du flag de sync est autorisée (pas de modification fiscale)
    @Query("UPDATE clotures SET isDirty = 0 WHERE idCloture = :id")
    suspend fun markSynced(id: java.util.UUID)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCloture(cloture: Cloture)

}