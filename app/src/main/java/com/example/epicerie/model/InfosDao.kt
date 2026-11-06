package com.example.epicerie.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.epicerie.data.ShopInfos

@Dao
interface InfosDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInfos(infos: ShopInfos)

    @Update
    suspend fun updateInfos(infos: ShopInfos)

    @Query("SELECT * FROM ShopInfos LIMIT 1")
    suspend fun getInfos(): ShopInfos?

    /** Fiche magasin en continu (émet à chaque modification, y compris par la synchro). */
    @Query("SELECT * FROM ShopInfos LIMIT 1")
    fun observeInfos(): kotlinx.coroutines.flow.Flow<ShopInfos?>

    // Sync : ne baisse le flag dirty QUE si la ligne n'a pas été modifiée depuis sa lecture
    // (sinon une modification faite pendant le push serait perdue sans jamais être poussée).
    @Query("UPDATE ShopInfos SET isDirty = 0 WHERE id = 1 AND updatedAt = :updatedAt")
    suspend fun clearDirty(updatedAt: Long)

    @Query("UPDATE ShopInfos SET passwordHash = :passwordHash, passwordSalt = :passwordSalt WHERE id = 1")
    suspend fun updatePassword(passwordHash: String, passwordSalt: String)

}