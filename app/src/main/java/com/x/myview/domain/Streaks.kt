package com.x.myview.domain

import com.x.myview.util.isScheduled

/**
 * Streak counted over scheduled days only. Today not being done yet does not break the streak,
 * because there is still time to do it.
 */
fun currentStreak(days: Int, done: Set<Long>, today: Long): Int {
    if (days == 0) return 0
    var d = today
    if (isScheduled(days, d) && d !in done) d -= 1
    var streak = 0
    var guard = 0
    while (guard++ < 4000) {
        if (isScheduled(days, d)) {
            if (d in done) streak++ else break
        }
        d -= 1
    }
    return streak
}

fun bestStreak(days: Int, done: Set<Long>, today: Long): Int {
    if (days == 0 || done.isEmpty()) return 0
    var best = 0
    var run = 0
    var d = done.min()
    while (d <= today) {
        if (isScheduled(days, d)) {
            if (d in done) {
                run++
                if (run > best) best = run
            } else if (d != today) {
                run = 0
            }
        }
        d++
    }
    return best
}

/** Share of scheduled days (from [from] to [today]) that were done, 0..1. */
fun completionRate(days: Int, done: Set<Long>, from: Long, today: Long): Float {
    var scheduled = 0
    var hit = 0
    var d = from
    while (d <= today) {
        if (isScheduled(days, d)) {
            scheduled++
            if (d in done) hit++
        }
        d++
    }
    return if (scheduled == 0) 0f else hit.toFloat() / scheduled
}
