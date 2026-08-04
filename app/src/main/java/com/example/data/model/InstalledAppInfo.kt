package com.example.data.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val usageTimeMs: Long = 0L,
    val limitMinutes: Int = 0, // 0 = no limit set
    val isEnabled: Boolean = true,
    val category: AppCategory = AppCategory.GENERAL,
    val isSystemApp: Boolean = false,
    val tempUnlockUntilMs: Long = 0L
) {
    val usageMinutes: Int get() = (usageTimeMs / 60_000L).toInt()
    
    val isLimitExceeded: Boolean
        get() {
            if (limitMinutes <= 0 || !isEnabled) return false
            val now = System.currentTimeMillis()
            if (tempUnlockUntilMs > now) return false // temporarily unlocked via PIN
            return usageMinutes >= limitMinutes
        }

    val isWarningThresholdReached: Boolean
        get() {
            if (limitMinutes <= 0 || !isEnabled) return false
            val remainingMins = limitMinutes - usageMinutes
            return remainingMins in 1..5 && !isLimitExceeded
        }

    val progressPercent: Float
        get() {
            if (limitMinutes <= 0) return 0f
            return (usageMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
        }
}

enum class AppCategory(val displayName: String, val iconName: String) {
    SOCIAL("Social Media", "Share"),
    ENTERTAINMENT("Entertainment", "Play"),
    GAMES("Gaming", "Games"),
    PRODUCTIVITY("Productivity", "Work"),
    COMMUNICATION("Messaging & Call", "Chat"),
    GENERAL("Utilities & Other", "Category");

    companion object {
        fun fromPackageName(pkg: String): AppCategory {
            val lower = pkg.lowercase()
            return when {
                lower.contains("instagram") || lower.contains("twitter") || lower.contains("tiktok") ||
                        lower.contains("facebook") || lower.contains("reddit") || lower.contains("snapchat") ||
                        lower.contains("linkedin") || lower.contains("pinterest") -> SOCIAL

                lower.contains("youtube") || lower.contains("netflix") || lower.contains("spotify") ||
                        lower.contains("twitch") || lower.contains("prime") || lower.contains("disney") ||
                        lower.contains("hulu") || lower.contains("music") || lower.contains("vlc") -> ENTERTAINMENT

                lower.contains("game") || lower.contains("pubg") || lower.contains("clash") ||
                        lower.contains("candy") || lower.contains("roblox") || lower.contains("minecraft") -> GAMES

                lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("messenger") ||
                        lower.contains("signal") || lower.contains("slack") || lower.contains("discord") ||
                        lower.contains("meet") || lower.contains("zoom") -> COMMUNICATION

                lower.contains("docs") || lower.contains("sheets") || lower.contains("office") ||
                        lower.contains("notion") || lower.contains("notes") || lower.contains("mail") ||
                        lower.contains("drive") || lower.contains("keep") -> PRODUCTIVITY

                else -> GENERAL
            }
        }
    }
}
