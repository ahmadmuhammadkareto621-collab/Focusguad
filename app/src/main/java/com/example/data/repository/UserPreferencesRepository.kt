package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focusguard_prefs", Context.MODE_PRIVATE)

    private val _pinState = MutableStateFlow(getPin())
    val pinState: StateFlow<String?> = _pinState

    private val _isOnboardedState = MutableStateFlow(isOnboarded())
    val isOnboardedState: StateFlow<Boolean> = _isOnboardedState

    private val _isDarkThemeState = MutableStateFlow(isDarkTheme())
    val isDarkThemeState: StateFlow<Boolean> = _isDarkThemeState

    fun savePin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        _pinState.value = hash
    }

    fun verifyPin(enteredPin: String): Boolean {
        val currentHash = prefs.getString(KEY_PIN_HASH, null) ?: return true
        return hashPin(enteredPin) == currentHash
    }

    fun hasPin(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    private fun getPin(): String? = prefs.getString(KEY_PIN_HASH, null)

    fun setOnboarded(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDED, completed).apply()
        _isOnboardedState.value = completed
    }

    private fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _isDarkThemeState.value = enabled
    }

    private fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, false)

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_ONBOARDED = "is_onboarded"
        private const val KEY_DARK_THEME = "is_dark_theme"
    }
}
