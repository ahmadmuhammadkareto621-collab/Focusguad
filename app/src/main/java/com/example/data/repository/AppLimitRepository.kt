package com.example.data.repository

import com.example.data.db.AppLimitDao
import com.example.data.db.AppLimitEntity
import kotlinx.coroutines.flow.Flow

class AppLimitRepository(private val appLimitDao: AppLimitDao) {

    val allLimits: Flow<List<AppLimitEntity>> = appLimitDao.getAllLimits()
    val activeLimits: Flow<List<AppLimitEntity>> = appLimitDao.getActiveLimits()

    suspend fun getLimitForPackage(packageName: String): AppLimitEntity? {
        return appLimitDao.getLimitForPackage(packageName)
    }

    fun observeLimitForPackage(packageName: String): Flow<AppLimitEntity?> {
        return appLimitDao.observeLimitForPackage(packageName)
    }

    suspend fun setAppLimit(packageName: String, appName: String, limitMinutes: Int, category: String = "General") {
        val existing = appLimitDao.getLimitForPackage(packageName)
        val entity = AppLimitEntity(
            packageName = packageName,
            appName = appName,
            limitMinutes = limitMinutes,
            isEnabled = true,
            category = category,
            tempUnlockUntilMs = existing?.tempUnlockUntilMs ?: 0L
        )
        appLimitDao.insertOrUpdateLimit(entity)
    }

    suspend fun toggleLimitEnabled(limit: AppLimitEntity, isEnabled: Boolean) {
        val updated = limit.copy(isEnabled = isEnabled)
        appLimitDao.insertOrUpdateLimit(updated)
    }

    suspend fun tempUnlockForMinutes(packageName: String, minutes: Int) {
        val unlockUntil = System.currentTimeMillis() + (minutes * 60_000L)
        appLimitDao.setTempUnlock(packageName, unlockUntil)
    }

    suspend fun removeLimit(packageName: String) {
        appLimitDao.deleteLimitByPackage(packageName)
    }
}
