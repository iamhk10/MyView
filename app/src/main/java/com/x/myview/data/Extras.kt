package com.x.myview.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small per-day counters that live in SharedPreferences: focus minutes, breathing sessions, and the
 * focus timer that is currently running (so it survives leaving the screen).
 */
class ExtrasStore(context: Context) {
    private val prefs = context.getSharedPreferences("myview_extras", Context.MODE_PRIVATE)

    private fun load(prefix: String): Map<Long, Int> {
        val map = HashMap<Long, Int>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(prefix) && value is Int) {
                val day = key.removePrefix(prefix).toLongOrNull() ?: continue
                map[day] = value
            }
        }
        return map
    }

    private val _focus = MutableStateFlow(load("f_"))
    /** epoch day -> focused minutes. */
    val focusMinutes: StateFlow<Map<Long, Int>> = _focus.asStateFlow()

    private val _breath = MutableStateFlow(load("b_"))
    /** epoch day -> finished breathing sessions. */
    val breathSessions: StateFlow<Map<Long, Int>> = _breath.asStateFlow()

    private val _focusEnd = MutableStateFlow(prefs.getLong("focusEnd", 0L))
    /** When the running focus timer ends (epoch millis), 0 when none is running. */
    val focusEnd: StateFlow<Long> = _focusEnd.asStateFlow()

    private val _focusPlanned = MutableStateFlow(prefs.getInt("focusMin", 25))
    /** Length of the running (or last used) focus timer in minutes. */
    val focusPlanned: StateFlow<Int> = _focusPlanned.asStateFlow()

    @Synchronized
    fun addFocus(day: Long, minutes: Int) {
        val total = (_focus.value[day] ?: 0) + minutes
        prefs.edit().putInt("f_$day", total).apply()
        _focus.value = _focus.value + (day to total)
    }

    @Synchronized
    fun addBreath(day: Long) {
        val total = (_breath.value[day] ?: 0) + 1
        prefs.edit().putInt("b_$day", total).apply()
        _breath.value = _breath.value + (day to total)
    }

    @Synchronized
    fun startFocus(minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        prefs.edit().putLong("focusEnd", end).putInt("focusMin", minutes).apply()
        _focusPlanned.value = minutes
        _focusEnd.value = end
    }

    @Synchronized
    fun clearFocus() {
        prefs.edit().putLong("focusEnd", 0L).apply()
        _focusEnd.value = 0L
    }
}
