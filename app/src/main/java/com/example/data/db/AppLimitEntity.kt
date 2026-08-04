package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val limitMinutes: Int, // daily limit in minutes
    val isEnabled: Boolean = true,
    val category: String = "General",
    val tempUnlockUntilMs: Long = 0L // timestamp until which app is temporarily unlocked by PIN
)
