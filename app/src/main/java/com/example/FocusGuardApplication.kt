package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.repository.AppLimitRepository
import com.example.data.repository.UsageStatsRepository
import com.example.data.repository.UserPreferencesRepository

class FocusGuardApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var appLimitRepository: AppLimitRepository
        private set

    lateinit var usageStatsRepository: UsageStatsRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        appLimitRepository = AppLimitRepository(database.appLimitDao())
        usageStatsRepository = UsageStatsRepository(this, database.usageLogDao())

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "focusguard_channel"
        lateinit var instance: FocusGuardApplication
            private set
    }
}
