package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getActiveLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getLimitForPackage(packageName: String): AppLimitEntity?

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    fun observeLimitForPackage(packageName: String): Flow<AppLimitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: AppLimitEntity)

    @Query("UPDATE app_limits SET tempUnlockUntilMs = :unlockUntilMs WHERE packageName = :packageName")
    suspend fun setTempUnlock(packageName: String, unlockUntilMs: Long)

    @Delete
    suspend fun deleteLimit(limit: AppLimitEntity)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimitByPackage(packageName: String)
}
