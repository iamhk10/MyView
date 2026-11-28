package com.x.myview.run

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun speedKmh(ms: Float): Float = ms * 3.6f

/** "5:42" for a speed in m/s, or "--:--" when standing still. */
fun paceText(ms: Float): String {
    if (ms < 0.3f) return "--:--"
    val sec = (1000f / ms).roundToInt().coerceAtMost(99 * 60 + 59)
    return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
}

fun paceTextSec(secPerKm: Int): String =
    String.format(Locale.US, "%d:%02d", secPerKm / 60, secPerKm % 60)

fun kmText(m: Float): String = String.format(Locale.US, "%.2f", m / 1000f)

/** Value and unit for a distance: metres below 1 km (so sprints read right), kilometres above. */
fun distParts(m: Float): Pair<String, String> =
    if (m < 1000f) m.toInt().toString() to "m" else kmText(m) to "km"

fun distLabel(m: Float): String = distParts(m).let { it.first + " " + it.second }

/** A distance target: 50 m, 400 m, 1 km, 2.5 km. */
fun goalDistText(m: Int): String = when {
    m < 1000 -> "$m m"
    m % 1000 == 0 -> "${m / 1000} km"
    else -> String.format(Locale.US, "%.1f km", m / 1000f)
}

/** Calories from an average speed: MET (effort) x body weight x hours. */
fun caloriesFor(kmh: Float, kg: Float, seconds: Int): Int {
    val met = when {
        kmh < 4f -> 2.8f
        kmh < 5.5f -> 3.5f
        kmh < 6.5f -> 6.0f
        kmh < 8f -> 8.3f
        kmh < 9.7f -> 9.8f
        kmh < 11f -> 10.5f
        kmh < 12.8f -> 11.8f
        else -> 12.8f
    }
    return (met * kg * seconds / 3600f).roundToInt()
}

/** Works without internet: a short written review of one run. */
fun offlineInsight(rec: RunRecord, history: List<RunRecord>): String {
    val kmh = speedKmh(rec.avgSpeedMs)
    val km = rec.distanceM / 1000f
    val lines = ArrayList<String>()
    val what = if (rec.walk) "walk" else "run"
    lines.add(
        String.format(Locale.US, "You covered %s in %s at %.1f km/h (pace %s /km).", distLabel(rec.distanceM), clock(rec.durationSec), kmh, paceText(rec.avgSpeedMs)),
    )
    lines.add(
        if (rec.walk) {
            when {
                kmh < 4f -> "That is an easy, relaxed walk: good for recovery."
                kmh < 5.5f -> "A brisk walk, the pace most health guidelines aim for."
                else -> "A fast walk. You are close to jogging speed."
            }
        } else {
            when {
                kmh >= 13f -> "That is a very fast pace. Make sure you recover well."
                kmh >= 10f -> "A strong, quick pace."
                kmh >= 8f -> "A good steady running pace."
                kmh >= 6.5f -> "An easy jogging pace, great for building stamina."
                else -> "A very easy pace, fine for a warm-up or recovery day."
            }
        },
    )
    when (rec.goal) {
        RunGoal.TIME -> if (rec.goalValue > 0) {
            lines.add(if (rec.durationSec >= rec.goalValue) "Time goal of ${clock(rec.goalValue)} completed." else "You finished ${(rec.durationSec * 100 / rec.goalValue)}% of your time goal.")
        }
        RunGoal.DISTANCE -> if (rec.goalValue > 0) {
            lines.add(if (rec.distanceM >= rec.goalValue) "Distance goal of ${goalDistText(rec.goalValue)} completed." else "You finished ${(rec.distanceM * 100 / rec.goalValue).toInt()}% of your distance goal.")
        }
        RunGoal.FREE -> {}
    }
    if (rec.splits.size >= 2) {
        val first = rec.splits.first()
        val lastKm = rec.splits.last()
        val fastest = rec.splits.indices.minByOrNull { rec.splits[it] } ?: 0
        lines.add("Fastest kilometre: km ${fastest + 1} (${paceTextSec(rec.splits[fastest])}).")
        val diff = lastKm - first
        lines.add(
            when {
                diff <= -10 -> "Negative split: your last km was ${abs(diff)} s faster than the first. Very well paced."
                diff >= 15 -> "Your last km was $diff s slower than the first. Start a little easier next time."
                else -> "Very even pacing from start to finish."
            },
        )
    }
    val similar = history.filter { it.walk == rec.walk && it.distanceM >= rec.distanceM * 0.5f && it.avgSpeedMs > 0f }.take(3)
    if (similar.isNotEmpty() && rec.avgSpeedMs > 0f) {
        val base = similar.map { it.avgSpeedMs }.average().toFloat()
        val change = ((rec.avgSpeedMs - base) / base * 100f).roundToInt()
        lines.add(
            when {
                change >= 3 -> "$change% faster than your last ${similar.size} ${what}s."
                change <= -3 -> "${-change}% slower than your last ${similar.size} ${what}s. A rest day or sleep may help."
                else -> "About the same pace as your last ${similar.size} ${what}s."
            },
        )
    }
    lines.add("About ${rec.calories} kcal burned.")
    lines.add(
        if (km < 2f) "Tip: add about 10% distance each week to build up safely."
        else "Tip: drink water, stretch your calves and thighs, and keep tomorrow light."
    )
    return lines.joinToString("\n") { "• $it" }
}

private fun clock(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/** The question sent to the AI coach: the numbers only, no name and no location. */
fun buildCoachPrompt(rec: RunRecord, history: List<RunRecord>): String {
    val sb = StringBuilder()
    sb.append("You are a friendly running coach inside a fitness app. Reply in Hinglish (Hindi in Roman script mixed with simple English). ")
    sb.append("Keep it under 150 words: 4 or 5 short bullet points with a quick review of this session, one thing done well, one thing to improve, and a simple plan for the next session. No medical claims.\n\n")
    sb.append(if (rec.walk) "Activity: walk\n" else "Activity: run\n")
    sb.append(String.format(Locale.US, "Distance: %.2f km\nTime: %s\nAverage speed: %.1f km/h\nAverage pace: %s min/km\nMax speed: %.1f km/h\nCalories (estimate): %d\n", rec.distanceM / 1000f, clock(rec.durationSec), speedKmh(rec.avgSpeedMs), paceText(rec.avgSpeedMs), speedKmh(rec.maxSpeedMs), rec.calories))
    if (rec.splits.isNotEmpty()) sb.append("Km splits (min/km): ").append(rec.splits.joinToString(", ") { paceTextSec(it) }).append('\n')
    when (rec.goal) {
        RunGoal.TIME -> sb.append("Goal: run for ${rec.goalValue / 60} minutes\n")
        RunGoal.DISTANCE -> sb.append("Goal: ${goalDistText(rec.goalValue)}\n")
        RunGoal.FREE -> sb.append("Goal: free run\n")
    }
    val prev = history.take(5)
    if (prev.isNotEmpty()) {
        sb.append("Previous sessions (newest first):\n")
        prev.forEach { sb.append(String.format(Locale.US, "- %.2f km at %.1f km/h in %s\n", it.distanceM / 1000f, speedKmh(it.avgSpeedMs), clock(it.durationSec))) }
    }
    return sb.toString()
}
