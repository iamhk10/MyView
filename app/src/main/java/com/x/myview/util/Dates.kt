package com.x.myview.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

const val ALL_DAYS = 127

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** Monday = bit 0 ... Sunday = bit 6. */
fun dayBit(dow: DayOfWeek): Int = 1 shl (dow.value - 1)

fun isScheduled(days: Int, epochDay: Long): Boolean =
    days and dayBit(LocalDate.ofEpochDay(epochDay).dayOfWeek) != 0

/** First day of the week (Monday) containing [epochDay]. */
fun weekStart(epochDay: Long): Long {
    val dow = LocalDate.ofEpochDay(epochDay).dayOfWeek.value // 1 = Monday
    return epochDay - (dow - 1)
}

fun shortDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)

fun oneLetterDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

fun niceDate(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val dow = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val month = d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$dow, ${d.dayOfMonth} $month"
}

fun monthYear(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return d.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + d.year
}

val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun daysSummary(mask: Int): String = when {
    mask == ALL_DAYS -> "Every day"
    mask == 0b0011111 -> "Weekdays"
    mask == 0b1100000 -> "Weekends"
    else -> DAY_NAMES.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(", ")
}

/** 2500 -> "2.5 L", 250 -> "250 ml". */
fun formatMl(ml: Int): String =
    if (ml >= 1000) {
        val litres = ml / 1000f
        (if (ml % 1000 == 0) "${ml / 1000}" else String.format(Locale.US, "%.2f", litres).trimEnd('0').trimEnd('.')) + " L"
    } else "$ml ml"

/** 125 -> "2:05", 3700 -> "1:01:40". */
fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/** Clock time of a timestamp, e.g. "7:45 PM". */
fun timeOfDay(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
