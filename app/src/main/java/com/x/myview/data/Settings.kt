package com.x.myview.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class FontOption(val label: String) {
    DEFAULT("Default"),
    MEDIUM("Medium"),
    CONDENSED("Condensed"),
    LIGHT("Light"),
    SERIF("Serif"),
    MONO("Mono"),
}

/** Overall look. Health is the Google Health style (default); Midnight is silver and violet glass; Classic is the first design. */
enum class AppLook(val label: String) { MIDNIGHT("Midnight"), CLASSIC("Classic"), BENTO("Bento"), HEALTH("Health") }

/** Font for big titles and numbers (the body text uses [FontOption]). */
enum class DisplayFont(val label: String) {
    BLACK("Black"),
    CONDENSED("Condensed"),
    SERIF("Serif"),
    SCRIPT("Script"),
    CASUAL("Casual"),
    SMALL_CAPS("Small caps"),
    SAME("Same as text"),
}

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.9f),
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.12f),
    XL("Extra large", 1.25f),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentIndex: Int = 0,
    val dynamicColor: Boolean = false,
    val glass: Boolean = false,
    /** Daily water goal in ml. */
    val waterGoalMl: Int = 2500,
    /** Size of the "one glass" quick-add button in ml. */
    val glassMl: Int = 250,
    /** Daily step goal. */
    val stepGoal: Int = 8000,
    val waterReminder: Boolean = false,
    /** Hours between water reminders (they run 8:00 to 22:00). */
    val waterIntervalH: Int = 2,
    val workoutReminder: Boolean = false,
    /** Workout reminder time as minutes after midnight. */
    val workoutReminderMin: Int = 18 * 60,
    val fontOption: FontOption = FontOption.DEFAULT,
    val textSize: TextSize = TextSize.NORMAL,
    /** Sleep goal in minutes. */
    val sleepGoalMin: Int = 8 * 60,
    /** Show weight in pounds instead of kilograms (data is always stored in kg). */
    val useLb: Boolean = false,
    /** Weight goal in kg, 0 = no goal. */
    val weightGoalKg: Float = 0f,
    val haptics: Boolean = true,
    /** Confetti when a daily goal is completed. */
    val celebrate: Boolean = true,
    /** Keep counting steps in the background with a quiet notification, so widgets update at once. */
    val liveSteps: Boolean = true,
    val look: AppLook = AppLook.HEALTH,
    val displayFont: DisplayFont = DisplayFont.BLACK,
    /** Look of the picture-style widgets: 0 = light, 1 = dark, 2 = accent colour. */
    val widgetStyle: Int = 0,
    /** Optional Google Gemini key for the AI coach after a run. Typed by the user, kept only on this phone. */
    val geminiKey: String = "",
    /** Speak the distance and pace out loud during a run. */
    val voiceCoach: Boolean = true,
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
            stepGoal = prefs.getInt("stepGoal", 8000).coerceIn(1000, 50000),
            waterReminder = prefs.getBoolean("waterReminder", false),
            waterIntervalH = prefs.getInt("waterIntervalH", 2).coerceIn(1, 6),
            workoutReminder = prefs.getBoolean("workoutReminder", false),
            workoutReminderMin = prefs.getInt("workoutReminderMin", 18 * 60).coerceIn(0, 1439),
            fontOption = FontOption.values().getOrElse(prefs.getInt("fontOption", 0)) { FontOption.DEFAULT },
            textSize = TextSize.values().getOrElse(prefs.getInt("textSize", 1)) { TextSize.NORMAL },
            sleepGoalMin = prefs.getInt("sleepGoalMin", 480).coerceIn(240, 720),
            useLb = prefs.getBoolean("useLb", false),
            weightGoalKg = prefs.getFloat("weightGoalKg", 0f).coerceIn(0f, 500f),
            haptics = prefs.getBoolean("haptics", true),
            celebrate = prefs.getBoolean("celebrate", true),
            liveSteps = prefs.getBoolean("liveSteps", true),
            look = AppLook.values().getOrElse(prefs.getInt("look3", AppLook.HEALTH.ordinal)) { AppLook.HEALTH },
            displayFont = DisplayFont.values().getOrElse(prefs.getInt("displayFont", 0)) { DisplayFont.BLACK },
            widgetStyle = prefs.getInt("widgetStyle", 0).coerceIn(0, 2),
            geminiKey = prefs.getString("geminiKey", "") ?: "",
            voiceCoach = prefs.getBoolean("voiceCoach", true),
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
            .putInt("stepGoal", next.stepGoal)
            .putBoolean("waterReminder", next.waterReminder)
            .putInt("waterIntervalH", next.waterIntervalH)
            .putBoolean("workoutReminder", next.workoutReminder)
            .putInt("workoutReminderMin", next.workoutReminderMin)
            .putInt("fontOption", next.fontOption.ordinal)
            .putInt("textSize", next.textSize.ordinal)
            .putInt("sleepGoalMin", next.sleepGoalMin)
            .putBoolean("useLb", next.useLb)
            .putFloat("weightGoalKg", next.weightGoalKg)
            .putBoolean("haptics", next.haptics)
            .putBoolean("celebrate", next.celebrate)
            .putBoolean("liveSteps", next.liveSteps)
            .putInt("look3", next.look.ordinal)
            .putInt("displayFont", next.displayFont.ordinal)
            .putInt("widgetStyle", next.widgetStyle)
            .putString("geminiKey", next.geminiKey)
            .putBoolean("voiceCoach", next.voiceCoach)
            .apply()
    }
}
