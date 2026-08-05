package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.FocusGuardApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentForegroundPackage: String? = null
    private var lastCheckTime: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Intercept foreground activity and window state transitions
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val packageName = event.packageName?.toString() ?: return
            if (packageName.isBlank()) return

            val now = System.currentTimeMillis()
            // Throttle rapid identical events for the same package within 500ms
            if (packageName == currentForegroundPackage && (now - lastCheckTime < 500)) return

            currentForegroundPackage = packageName
            lastCheckTime = now

            // Skip blocking checks for our own app, launchers, and system UI, but ensure currentForegroundPackage was updated
            if (packageName == applicationContext.packageName ||
                packageName.contains("launcher") ||
                packageName.contains("systemui")) return

            checkAndBlockPackageIfNeeded(packageName)
        }
    }

    private fun checkAndBlockPackageIfNeeded(packageName: String) {
        serviceScope.launch {
            try {
                val app = FocusGuardApplication.instance
                val limitEntity = app.appLimitRepository.getLimitForPackage(packageName) ?: return@launch
                
                // Do not block if rule is disabled
                if (!limitEntity.isEnabled) return@launch

                val now = System.currentTimeMillis()
                // Skip blocking if app is temporarily unlocked via PIN
                if (limitEntity.tempUnlockUntilMs > now) return@launch

                val todayMap = app.usageStatsRepository.getTodayUsageStatsMap()
                val usedMs = todayMap[packageName] ?: 0L
                val usedMins = (usedMs / 60_000L).toInt()

                // Block if limit is set to 0 (always block) or if accumulated usage meets/exceeds limit
                if (limitEntity.limitMinutes == 0 || usedMins >= limitEntity.limitMinutes) {
                    launchBlockingOverlay(
                        packageName = packageName,
                        appName = limitEntity.appName,
                        limitMins = limitEntity.limitMinutes,
                        usedMins = usedMins
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun launchBlockingOverlay(packageName: String, appName: String, limitMins: Int, usedMins: Int) {
        val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockingOverlayActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            putExtra(BlockingOverlayActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockingOverlayActivity.EXTRA_LIMIT_MINS, limitMins)
            putExtra(BlockingOverlayActivity.EXTRA_USED_MINS, usedMins)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val prefString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedService = "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
            return prefString.contains(expectedService)
        }

        fun openAccessibilitySettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
    }
}

