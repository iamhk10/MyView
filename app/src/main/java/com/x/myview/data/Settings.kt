package com.x.myview.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentIndex: Int = 0,
    val dynamicColor: Boolean = false,
    val glass: Boolean = false,
    /** Daily water goal in ml. */
    val waterGoalMl: Int = 2500,
    /** Size of the "one glass" quick-add button in ml. */
    val glassMl: Int = 250,
)

/** Small settings store on top of SharedPreferences. Exposes the settings as a StateFlow. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("myview_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun read(): AppSettings {
        val mode = ThemeMode.values().getOrElse(prefs.getInt("themeMode", 0)) { ThemeMode.SYSTEM }
        return AppSettings(
            themeMode = mode,
            accentIndex = prefs.getInt("accentIndex", 0),
            dynamicColor = prefs.getBoolean("dynamicColor", false),
            glass = prefs.getBoolean("glass", false),
            waterGoalMl = prefs.getInt("waterGoalMl", 2500).coerceIn(500, 8000),
            glassMl = prefs.getInt("glassMl", 250).coerceIn(50, 1500),
        )
    }

    fun update(change: (AppSettings) -> AppSettings) {
        val next = change(_settings.value)
        _settings.value = next
        prefs.edit()
            .putInt("themeMode", next.themeMode.ordinal)
            .putInt("accentIndex", next.accentIndex)
            .putBoolean("dynamicColor", next.dynamicColor)
            .putBoolean("glass", next.glass)
            .putInt("waterGoalMl", next.waterGoalMl)
            .putInt("glassMl", next.glassMl)
            .apply()
    }
}
