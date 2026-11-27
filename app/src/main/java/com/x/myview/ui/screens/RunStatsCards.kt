package com.x.myview.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.myview.run.RunRecord
import com.x.myview.run.kmText
import com.x.myview.run.speedKmh
import com.x.myview.ui.components.MvCard
import com.x.myview.util.formatDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

/** Last 7 days as bars (km per day) with totals for the week. */
@Composable
fun RunWeekCard(history: List<RunRecord>) {
    val today = LocalDate.now()
    val days = remember(history, today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val perDay = remember(history, today) {
        days.map { d -> history.filter { dayOf(it.startedAt) == d }.sumOf { it.distanceM.toDouble() }.toFloat() }
    }
    val week = remember(history, today) { history.filter { dayOf(it.startedAt) in days } }
    val totalM = week.sumOf { it.distanceM.toDouble() }.toFloat()
    val totalSec = week.sumOf { it.durationSec }
    val top = max(perDay.maxOrNull() ?: 0f, 1000f)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Last 7 days", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeekFigure(kmText(totalM), "km")
                WeekFigure(week.size.toString(), if (week.size == 1) "activity" else "activities")
                WeekFigure(formatDuration(totalSec), "time")
            }
            Canvas(Modifier.fillMaxWidth().height(84.dp)) {
                val n = perDay.size
                val gap = 10.dp.toPx()
                val bw = (size.width - gap * (n - 1)) / n
                perDay.forEachIndexed { i, v ->
                    val x = i * (bw + gap)
                    drawRoundRect(trackColor, Offset(x, 0f), Size(bw, size.height), CornerRadius(6.dp.toPx()))
                    if (v > 0f) {
                        val h = max(size.height * (v / top), 6.dp.toPx())
                        drawRoundRect(barColor, Offset(x, size.height - h), Size(bw, h), CornerRadius(6.dp.toPx()))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { d ->
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekFigure(value: String, unit: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Personal bests across everything saved. */
@Composable
fun RunBestsCard(history: List<RunRecord>) {
    if (history.isEmpty()) return
    val longest = history.maxByOrNull { it.distanceM }
    // A speed record only counts for runs of at least 500 m, so a short GPS blip cannot win.
    val fastest = history.filter { it.distanceM >= 500f }.maxByOrNull { it.avgSpeedMs }
    val longTime = history.maxByOrNull { it.durationSec }
    val bestKm = history.flatMap { it.splits }.minOrNull()
    val lifetime = history.sumOf { it.distanceM.toDouble() }.toFloat()

    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Personal bests", fontWeight = FontWeight.SemiBold)
            if (longest != null) BestRow("Longest distance", kmText(longest.distanceM) + " km")
            if (fastest != null) BestRow("Fastest average", String.format(Locale.US, "%.1f km/h", speedKmh(fastest.avgSpeedMs)))
            if (bestKm != null) BestRow("Fastest kilometre", "${bestKm / 60}:${String.format(Locale.US, "%02d", bestKm % 60)} /km")
            if (longTime != null) BestRow("Longest time", formatDuration(longTime.durationSec))
            BestRow("Lifetime total", kmText(lifetime) + " km")
        }
    }
}

@Composable
private fun BestRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
