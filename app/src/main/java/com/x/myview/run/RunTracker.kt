package com.x.myview.run

import android.content.Context
import android.location.Location
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

enum class RunStatus { IDLE, RUNNING, PAUSED, DONE }

/** FREE = no target, TIME = run for N seconds, DISTANCE = run N metres. */
enum class RunGoal { FREE, TIME, DISTANCE }

data class RunPoint(val lat: Double, val lon: Double)

/** A finished run as it is stored in the history (the route is not stored, only the numbers). */
class RunRecord(
    val startedAt: Long,
    val durationSec: Int,
    val distanceM: Float,
    val maxSpeedMs: Float,
    val goal: RunGoal,
    val goalValue: Int,
    val walk: Boolean,
    val calories: Int,
    /** Seconds taken for each completed kilometre. */
    val splits: List<Int>,
) {
    val avgSpeedMs: Float get() = if (durationSec > 0) distanceM / durationSec else 0f
}

data class RunState(
    val status: RunStatus = RunStatus.IDLE,
    val goal: RunGoal = RunGoal.FREE,
    /** Seconds when [goal] is TIME, metres when DISTANCE. */
    val goalValue: Int = 0,
    val walk: Boolean = false,
    val elapsedSec: Int = 0,
    val distanceM: Float = 0f,
    /** Smoothed current speed in metres per second. */
    val speedMs: Float = 0f,
    val maxSpeedMs: Float = 0f,
    val gpsFix: Boolean = false,
    val accuracyM: Float = 99f,
    val points: List<RunPoint> = emptyList(),
    val splits: List<Int> = emptyList(),
    val goalReached: Boolean = false,
    val startedAt: Long = 0L,
    val record: RunRecord? = null,
)

/**
 * Keeps the numbers of the run that is going on. [RunService] feeds it GPS points and a one-second
 * tick; the screen only reads [state]. Everything lives in memory until the run is finished.
 */
object RunTracker {
    private const val MAX_ACCURACY_M = 30f
    private const val MAX_SPEED_MS = 12f // about 43 km/h: anything faster is a GPS jump
    private const val MIN_STEP_M = 2f

    private val _state = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = _state.asStateFlow()

    @Volatile private var last: Location? = null
    @Volatile private var accumMs = 0L
    @Volatile private var resumedAt = 0L
    @Volatile private var weightKg = 65f
    @Volatile private var kmMarkSec = 0

    fun configure(goal: RunGoal, value: Int, walk: Boolean, kg: Float) {
        if (_state.value.status == RunStatus.RUNNING || _state.value.status == RunStatus.PAUSED) return
        weightKg = if (kg in 25f..250f) kg else 65f
        _state.value = RunState(goal = goal, goalValue = value, walk = walk)
    }

    fun begin() {
        last = null
        accumMs = 0L
        resumedAt = SystemClock.elapsedRealtime()
        kmMarkSec = 0
        _state.update { it.copy(status = RunStatus.RUNNING, startedAt = System.currentTimeMillis()) }
    }

    fun pause() {
        if (_state.value.status != RunStatus.RUNNING) return
        accumMs += SystemClock.elapsedRealtime() - resumedAt
        last = null // distance during the pause must not count and there must be no jump afterwards
        _state.update { it.copy(status = RunStatus.PAUSED, speedMs = 0f) }
    }

    fun resume() {
        if (_state.value.status != RunStatus.PAUSED) return
        resumedAt = SystemClock.elapsedRealtime()
        _state.update { it.copy(status = RunStatus.RUNNING) }
    }

    private fun elapsedNow(): Int {
        val extra = if (_state.value.status == RunStatus.RUNNING) SystemClock.elapsedRealtime() - resumedAt else 0L
        return ((accumMs + extra) / 1000L).toInt()
    }

    /** Called once a second by the service. */
    fun tick() {
        val s = _state.value
        if (s.status != RunStatus.RUNNING) return
        val e = elapsedNow()
        val reached = s.goalReached || (s.goal == RunGoal.TIME && s.goalValue > 0 && e >= s.goalValue)
        _state.update { it.copy(elapsedSec = e, goalReached = reached) }
    }

    fun onLocation(loc: Location) {
        val acc = if (loc.hasAccuracy()) loc.accuracy else 99f
        val s = _state.value
        val fix = acc <= MAX_ACCURACY_M
        if (s.status != RunStatus.RUNNING || !fix) {
            _state.update { it.copy(gpsFix = fix, accuracyM = acc) }
            return
        }
        val prev = last
        if (prev == null) {
            last = loc
            _state.update { it.copy(gpsFix = true, accuracyM = acc, points = it.points + RunPoint(loc.latitude, loc.longitude)) }
            return
        }
        val dt = (loc.time - prev.time) / 1000f
        if (dt <= 0f) return
        val d = prev.distanceTo(loc)
        if (d / dt > MAX_SPEED_MS) return
        val raw = if (loc.hasSpeed()) loc.speed else d / dt
        val moving = d >= MIN_STEP_M && raw >= 0.5f
        if (!moving) {
            // Standing still or GPS wobble: slide the shown speed down to zero.
            val slow = _state.value.speedMs * 0.5f
            _state.update { it.copy(gpsFix = true, accuracyM = acc, speedMs = if (slow < 0.3f) 0f else slow) }
            return
        }
        last = loc
        val e = elapsedNow()
        _state.update { cur ->
            val dist = cur.distanceM + d
            val sp = if (cur.speedMs <= 0f) raw else 0.6f * cur.speedMs + 0.4f * raw
            val splits = cur.splits.toMutableList()
            while (dist >= (splits.size + 1) * 1000f) {
                splits.add((e - kmMarkSec).coerceAtLeast(1))
                kmMarkSec = e
            }
            val reached = cur.goalReached || (cur.goal == RunGoal.DISTANCE && cur.goalValue > 0 && dist >= cur.goalValue)
            cur.copy(
                gpsFix = true,
                accuracyM = acc,
                distanceM = dist,
                speedMs = sp,
                maxSpeedMs = maxOf(cur.maxSpeedMs, sp),
                splits = splits,
                elapsedSec = e,
                goalReached = reached,
                points = cur.points + RunPoint(loc.latitude, loc.longitude),
            )
        }
    }

    /** Ends the run, builds the record and (for a real run) stores it in the history. */
    fun finish(ctx: Context) {
        val s0 = _state.value
        if (s0.status != RunStatus.RUNNING && s0.status != RunStatus.PAUSED) return
        if (s0.status == RunStatus.RUNNING) accumMs += SystemClock.elapsedRealtime() - resumedAt
        val sec = (accumMs / 1000L).toInt()
        val kmh = if (sec > 0) s0.distanceM / sec * 3.6f else 0f
        val rec = RunRecord(
            startedAt = s0.startedAt,
            durationSec = sec,
            distanceM = s0.distanceM,
            maxSpeedMs = s0.maxSpeedMs,
            goal = s0.goal,
            goalValue = s0.goalValue,
            walk = s0.walk,
            calories = caloriesFor(kmh, weightKg, sec),
            splits = s0.splits,
        )
        if (rec.distanceM >= 20f) RunStore.add(ctx.applicationContext, rec)
        last = null
        _state.update { it.copy(status = RunStatus.DONE, elapsedSec = sec, speedMs = 0f, record = rec) }
    }

    fun reset() {
        last = null
        accumMs = 0L
        kmMarkSec = 0
        _state.value = RunState()
    }
}

/** The last runs, kept as a small JSON list in private preferences. */
object RunStore {
    private const val PREFS = "myview_runs"
    private const val KEY = "runs"
    private const val MAX = 100

    fun all(ctx: Context): List<RunRecord> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val sp = o.optJSONArray("splits") ?: JSONArray()
                RunRecord(
                    startedAt = o.getLong("at"),
                    durationSec = o.getInt("sec"),
                    distanceM = o.getDouble("m").toFloat(),
                    maxSpeedMs = o.optDouble("max", 0.0).toFloat(),
                    goal = RunGoal.values().getOrElse(o.optInt("goal", 0)) { RunGoal.FREE },
                    goalValue = o.optInt("gv", 0),
                    walk = o.optBoolean("walk", false),
                    calories = o.optInt("kcal", 0),
                    splits = (0 until sp.length()).map { sp.getInt(it) },
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(ctx: Context, r: RunRecord) {
        val list = (listOf(r) + all(ctx)).take(MAX)
        save(ctx, list)
    }

    fun delete(ctx: Context, startedAt: Long) {
        save(ctx, all(ctx).filter { it.startedAt != startedAt })
    }

    private fun save(ctx: Context, list: List<RunRecord>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("at", r.startedAt)
                    .put("sec", r.durationSec)
                    .put("m", r.distanceM.toDouble())
                    .put("max", r.maxSpeedMs.toDouble())
                    .put("goal", r.goal.ordinal)
                    .put("gv", r.goalValue)
                    .put("walk", r.walk)
                    .put("kcal", r.calories)
                    .put("splits", JSONArray(r.splits)),
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
