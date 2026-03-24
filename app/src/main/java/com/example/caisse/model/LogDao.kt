package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.caisse.data.LogTechnique
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogTechnique)

    @Query("SELECT * FROM logs_techniques ORDER BY date DESC")
    fun getAllLogs(): Flow<List<LogTechnique>>

    @Query("SELECT * FROM logs_techniques")
    suspend fun getAllLogsOnce(): List<LogTechnique>

    @Query("SELECT * FROM logs_techniques WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): LogTechnique?


    @Update
    suspend fun updateLog(log: LogTechnique)
}