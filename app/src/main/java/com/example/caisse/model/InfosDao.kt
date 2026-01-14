package com.example.piece.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.piece.data.ShopInfos

@Dao
interface InfosDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInfos(infos: ShopInfos)

    @Update
    suspend fun updateInfos(infos: ShopInfos)

    @Query("SELECT * FROM ShopInfos LIMIT 1")
    suspend fun getInfos(): ShopInfos?

    @Query("UPDATE ShopInfos SET passwordHash = :passwordHash, passwordSalt = :passwordSalt WHERE id = 1")
    suspend fun updatePassword(passwordHash: String, passwordSalt: String)

}