package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs WHERE dateString = :date ORDER BY timeInForegroundMs DESC")
    fun getUsageLogsForDate(date: String): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_logs WHERE dateString >= :startDate ORDER BY dateString ASC")
    fun getUsageLogsFromDate(startDate: String): Flow<List<UsageLogEntity>>

    @Query("SELECT SUM(timeInForegroundMs) FROM usage_logs WHERE dateString = :date")
    fun getTotalUsageMsForDate(date: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLogs(logs: List<UsageLogEntity>)

    @Query("DELETE FROM usage_logs")
    suspend fun clearAllLogs()
}
