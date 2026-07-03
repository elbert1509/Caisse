package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.ShopInfos
import kotlinx.coroutines.flow.Flow

@Dao
interface InfosDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInfos(infos: ShopInfos)

    @Update
    suspend fun updateInfos(infos: ShopInfos)

    @Query("SELECT * FROM ShopInfos LIMIT 1")
    suspend fun getInfos(): ShopInfos?

    // Flux réactif : émet dès que le SyncWorker importe/actualise la fiche, pour que les
    // écrans Infos se remplissent sans rechargement manuel (cas du nouvel appareil).
    @Query("SELECT * FROM ShopInfos LIMIT 1")
    fun getInfosFlow(): Flow<ShopInfos?>

    @Query("UPDATE ShopInfos SET passwordHash = :passwordHash, passwordSalt = :passwordSalt WHERE id = 1")
    suspend fun updatePassword(passwordHash: String, passwordSalt: String)

    // Sync : ne baisse le flag dirty QUE si la ligne n'a pas été modifiée depuis sa lecture
    // (sinon une modification faite pendant le push serait perdue sans jamais être poussée).
    @Query("UPDATE ShopInfos SET isDirty = 0 WHERE id = 1 AND updatedAt = :updatedAt")
    suspend fun clearDirty(updatedAt: Long)

}