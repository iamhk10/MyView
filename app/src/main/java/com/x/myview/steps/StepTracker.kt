package com.x.myview.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.x.myview.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Daily step counting on top of the phone's step-counter sensor.
 *
 * The sensor returns the total steps since the phone last booted. We keep the last value we saw and
 * add the difference to today's total every time we read it. A value lower than the last one means
 * the phone rebooted, so the new value itself is the difference. Phones without the sensor simply
 * report [isSupported] = false and the app hides everything about steps.
 */
class StepTracker(private val context: Context) {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("myview_steps", Context.MODE_PRIVATE)

    val isSupported: Boolean get() = sensor != null

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private val _steps = MutableStateFlow(load())
    /** epoch day -> steps counted that day. */
    val steps: StateFlow<Map<Long, Int>> = _steps.asStateFlow()

    fun stepsFor(day: Long): Int = _steps.value[day] ?: 0

    /**
     * Swaps the saved daily totals for [days] (used when restoring a backup). Today keeps the larger of
     * the two numbers so steps counted since the backup are not lost. The sensor's starting point is
     * not touched, so counting carries on normally.
     */
    @Synchronized
    fun replaceDays(days: Map<Long, Int>) {
        val today = todayEpochDay()
        val merged = HashMap(days)
        merged[today] = maxOf(days[today] ?: 0, _steps.value[today] ?: 0)
        val editor = prefs.edit()
        for (key in prefs.all.keys) if (key.startsWith("d_")) editor.remove(key)
        for ((day, n) in merged) if (n > 0) editor.putInt("d_$day", n)
        editor.apply()
        _steps.value = merged.filterValues { it > 0 }
    }

    private fun load(): Map<Long, Int> {
        val cutoff = todayEpochDay() - 3650
        val map = HashMap<Long, Int>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("d_") && value is Int) {
                val day = key.removePrefix("d_").toLongOrNull() ?: continue
                if (day >= cutoff) map[day] = value
            }
        }
        return map
    }

    @Synchronized
    private fun record(raw: Long) {
        val today = todayEpochDay()
        val last = prefs.getLong("raw", -1L)
        var delta = when {
            last < 0 -> 0L                 // first reading ever: only sets the starting point
            raw >= last -> raw - last
            else -> raw                    // phone rebooted since the last reading
        }
        if (delta > 60000) delta = 0       // an impossible jump, ignore it
        val editor = prefs.edit().putLong("raw", raw)
        if (delta > 0) {
            val total = (_steps.value[today] ?: 0) + delta.toInt()
            editor.putInt("d_$today", total)
            _steps.value = _steps.value + (today to total)
        }
        editor.apply()
    }

    private val liveListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            record(event.values[0].toLong())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    private var live = false
    private val owners = HashSet<String>()

    /** True while a live listener is registered (the sensor is then already feeding us). */
    val isLive: Boolean get() = live

    /**
     * Keeps the count updating. The app screen and the background step service each ask for it under
     * their own name, and the sensor listener stays on until both have let go.
     */
    @Synchronized
    fun startLive(owner: String = OWNER_APP) {
        owners.add(owner)
        val s = sensor ?: return
        if (live || !hasPermission()) return
        live = manager?.registerListener(liveListener, s, SensorManager.SENSOR_DELAY_NORMAL, 0) == true
    }

    @Synchronized
    fun stopLive(owner: String = OWNER_APP) {
        owners.remove(owner)
        if (!live || owners.isNotEmpty()) return
        manager?.unregisterListener(liveListener)
        live = false
    }

    /** Reads the sensor once (used by the background worker). */
    suspend fun readOnce(timeoutMs: Long = 5000) {
        val s = sensor ?: return
        val m = manager ?: return
        if (!hasPermission()) return
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Unit> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        m.unregisterListener(this)
                        record(event.values[0].toLong())
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                val ok = m.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
                if (!ok && cont.isActive) cont.resume(Unit)
                cont.invokeOnCancellation { m.unregisterListener(listener) }
            }
        }
    }

    companion object {
        const val OWNER_APP = "app"
        const val OWNER_SERVICE = "service"
    }
}
