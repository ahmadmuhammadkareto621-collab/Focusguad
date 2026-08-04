package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val dateString: String, // format "YYYY-MM-DD"
    val timeInForegroundMs: Long,
    val unlockCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
