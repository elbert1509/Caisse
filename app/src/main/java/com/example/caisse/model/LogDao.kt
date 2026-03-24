package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.caisse.data.LogTechnique
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogTechnique)

    @Query("SELECT * FROM logs_techniques ORDER BY date DESC")
    fun getAllLogs(): Flow<List<LogTechnique>>
}