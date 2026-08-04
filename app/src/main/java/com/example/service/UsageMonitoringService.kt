package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.FocusGuardApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notifiedFiveMinWarning = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()
        startPeriodicCheck()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, FocusGuardApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_shield)
            .setContentTitle("FocusGuard Protection Active")
            .setContentText("Monitoring app usage limits & screen time.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun startPeriodicCheck() {
        serviceScope.launch {
            while (isActive) {
                checkLimitsAndWarn()
                delay(30_000L) // Check every 30 seconds
            }
        }
    }

    private suspend fun checkLimitsAndWarn() {
        try {
            val app = FocusGuardApplication.instance
            val activeLimits = app.appLimitRepository.activeLimits.first()
            if (activeLimits.isEmpty()) return

            val usageMap = app.usageStatsRepository.getTodayUsageStatsMap()
            val now = System.currentTimeMillis()

            for (limit in activeLimits) {
                if (limit.limitMinutes <= 0) continue
                if (limit.tempUnlockUntilMs > now) continue

                val usedMs = usageMap[limit.packageName] ?: 0L
                val usedMins = (usedMs / 60_000L).toInt()
                val remainingMins = limit.limitMinutes - usedMins

                // 5 minutes warning notification
                if (remainingMins in 1..5 && !notifiedFiveMinWarning.contains(limit.packageName)) {
                    sendWarningNotification(limit.appName, limit.packageName, remainingMins)
                    notifiedFiveMinWarning.add(limit.packageName)
                }

                if (remainingMins > 5) {
                    notifiedFiveMinWarning.remove(limit.packageName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendWarningNotification(appName: String, packageName: String, remainingMins: Int) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, packageName.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, FocusGuardApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_shield)
            .setContentTitle("⏳ $appName Limit Warning")
            .setContentText("You have $remainingMins minute${if (remainingMins > 1) "s" else ""} remaining for $appName today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(packageName.hashCode(), notification)
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, UsageMonitoringService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
