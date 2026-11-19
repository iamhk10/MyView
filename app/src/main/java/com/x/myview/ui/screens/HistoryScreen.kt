package com.x.myview.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.KIND_REPS
import com.x.myview.domain.scheduledHabits
import com.x.myview.domain.waterByDay
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.LineChart
import com.x.myview.ui.components.MiniBar
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.theme.FireOrange
import com.x.myview.ui.theme.StepsTeal
import com.x.myview.ui.theme.WaterBlue
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatMl
import com.x.myview.util.formatSleep
import com.x.myview.util.formatSteps
import com.x.myview.util.formatWeight
import com.x.myview.util.niceDate
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** One thing that can be charted: what was recorded per day, its goal and how to print a value. */
private class Metric(
    val key: String,
    val label: String,
    val color: Color,
    /** epoch day -> value, only for days that have something recorded. */
    val values: Map<Long, Float>,
    val goal: Float?,
    val format: (Float) -> String,
    val showTotal: Boolean,
    val goalWord: String,
    /** Bars start at zero (false for weight, where only the changes matter). */
    val zeroBased: Boolean = true,
)

private val GOLD = Color(0xFFFFC857)
private val LOW_RED = Color(0xFFFF7A7A)

private fun shortDate(day: Long): String {
    val d = LocalDate.ofEpochDay(day)
    return "${d.dayOfMonth} ${d.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())}"
}

/**
 * Per-day history of everything that is tracked, as bars or a line, with the highest and lowest day,
 * the average and a day-by-day list. [start] is the metric to open first.
 */
@Composable
fun HistoryScreen(vm: MainViewModel, start: String, onBack: () -> Unit) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val steps by vm.steps.collectAsState()
    val waterLogs by vm.waterLogs.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val exerciseLogs by vm.exerciseLogs.collectAsState()
    val habits by vm.habits.collectAsState()
    val habitDone by vm.habitDone.collectAsState()
    val sleep by vm.sleepLogs.collectAsState()
    val weights by vm.weightLogs.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    var metricKey by rememberSaveable { mutableStateOf(start) }
    var rangeDays by rememberSaveable { mutableIntStateOf(30) }
    var asLine by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }

    val metrics = remember(steps, waterLogs, exercises, exerciseLogs, habits, habitDone, sleep, weights, settings, today, primary) {
        buildMetrics(
            vm.stepsSupported, steps, waterLogs.let { waterByDay(it) }, exercises.filter { it.kind == KIND_REPS }.map { it.id }.toSet(),
            exerciseLogs.groupBy { it.day }, habits, habitDone, sleep.associate { it.day to it.minutes },
            weights.associate { it.day to it.kg }, settings, today, primary,
        )
    }
    val metric = metrics.firstOrNull { it.key == metricKey } ?: metrics.first()
    LaunchedEffect(metric.key, rangeDays) { selected = null }

    val days = remember(today, rangeDays) { (today - rangeDays + 1..today).toList() }
    val recorded = days.filter { metric.values.containsKey(it) }
    val highest = recorded.maxByOrNull { metric.values.getValue(it) }
    val lowest = recorded.minByOrNull { metric.values.getValue(it) }
    val average = if (recorded.isEmpty()) 0f else recorded.map { metric.values.getValue(it) }.average().toFloat()
    val total = recorded.sumOf { metric.values.getValue(it).toDouble() }.toFloat()
    val goalHits = metric.goal?.let { g -> recorded.count { metric.values.getValue(it) >= g } } ?: 0
    val maxValue = recorded.maxOfOrNull { metric.values.getValue(it) } ?: 1f

    Column(Modifier.fillMaxSize()) {
        BackHeader("Stats and history", onBack)
        LazyColumn(
            Modifier
                .weight(1f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipFlow {
                        metrics.forEach { m -> ChoiceChip(m.label, m.key == metric.key, { metricKey = m.key }) }
                    }
                    ChipFlow {
                        ChoiceChip("7 days", rangeDays == 7, { rangeDays = 7 })
                        ChoiceChip("30 days", rangeDays == 30, { rangeDays = 30 })
                        ChoiceChip("90 days", rangeDays == 90, { rangeDays = 90 })
                        ChoiceChip("Bars", !asLine, { asLine = false })
                        ChoiceChip("Line", asLine, { asLine = true })
                    }
                }
            }
            item {
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        val sel = selected
                        Text(
                            if (sel == null) "Tap or drag on the chart to see a day"
                            else "${niceDate(sel)}:  " + (metric.values[sel]?.let(metric.format) ?: "nothing recorded"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (sel == null) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (sel == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (asLine) {
                            val points = recorded.map { it to metric.values.getValue(it) }
                            if (points.size < 2) {
                                Text(
                                    "Record at least two days to see a line.",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .padding(top = 70.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                key(metric.key, rangeDays) {
                                    LineChart(points, metric.color, Modifier.fillMaxWidth().height(170.dp), metric.goal)
                                }
                            }
                        } else {
                            HistoryBars(
                                days = days,
                                values = metric.values,
                                color = metric.color,
                                goal = metric.goal,
                                zeroBased = metric.zeroBased,
                                highest = highest,
                                lowest = lowest,
                                selected = selected,
                                animKey = metric.key + rangeDays,
                                onSelect = { selected = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(shortDate(days.first()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(shortDate(days.last()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                if (recorded.isEmpty()) {
                    Text(
                        "Nothing recorded for ${metric.label.lowercase(Locale.getDefault())} in these ${days.size} days yet.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill(metric.format(metric.values.getValue(highest!!)), "Highest  ·  ${shortDate(highest)}", Modifier.weight(1f))
                            StatPill(metric.format(metric.values.getValue(lowest!!)), "Lowest  ·  ${shortDate(lowest)}", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill(metric.format(average), "Average", Modifier.weight(1f))
                            if (metric.showTotal) {
                                StatPill(metric.format(total), "Total", Modifier.weight(1f))
                            } else {
                                StatPill("${recorded.size} / ${days.size}", "Days recorded", Modifier.weight(1f))
                            }
                        }
                        if (metric.goal != null && metric.goalWord.isNotEmpty()) {
                            Text(
                                "${metric.goalWord}: $goalHits of ${recorded.size} recorded days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
            item { SectionTitle("Day by day") }
            items(days.reversed()) { d ->
                val v = metric.values[d]
                val tag = when {
                    v == null -> null
                    d == highest && highest != lowest -> "Highest"
                    d == lowest && highest != lowest -> "Lowest"
                    else -> null
                }
                DayRow(
                    label = if (d == today) "Today" else niceDate(d),
                    bold = d == today,
                    value = v?.let(metric.format),
                    progress = if (v == null || maxValue <= 0f) 0f else (v / maxValue).coerceIn(0f, 1f),
                    color = metric.color,
                    tag = tag,
                    tagColor = if (tag == "Highest") GOLD else LOW_RED,
                )
            }
        }
    }
}

@Composable
private fun DayRow(
    label: String,
    bold: Boolean,
    value: String?,
    progress: Float,
    color: Color,
    tag: String?,
    tagColor: Color,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            )
            if (tag != null) {
                Text(tag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tagColor)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                value ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(5.dp))
        MiniBar(progress, color, Modifier.fillMaxWidth(), height = 4.dp)
    }
}

/**
 * One bar per day for any number of days. The highest day gets a gold dot, the lowest a red dot, days
 * with nothing recorded a thin stub. Tap or drag to pick a day.
 */
@Composable
private fun HistoryBars(
    days: List<Long>,
    values: Map<Long, Float>,
    color: Color,
    goal: Float?,
    zeroBased: Boolean,
    highest: Long?,
    lowest: Long?,
    selected: Long?,
    animKey: String,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = days.size
    val onSurface = MaterialTheme.colorScheme.onSurface
    val grow = remember { Animatable(0f) }
    LaunchedEffect(animKey) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(650))
    }

    val recorded = days.mapNotNull { values[it] }
    val dataMax = recorded.maxOrNull() ?: 0f
    val dataMin = recorded.minOrNull() ?: 0f
    var lo = 0f
    var hi = maxOf(dataMax, goal ?: 0f, 1f)
    if (!zeroBased && recorded.isNotEmpty()) {
        val top = maxOf(dataMax, goal ?: dataMax)
        val bottom = minOf(dataMin, goal ?: dataMin)
        val span = (top - bottom).coerceAtLeast(1f)
        lo = bottom - span * 0.4f
        hi = top + span * 0.1f
    }

    Canvas(
        modifier
            .pointerInput(days) {
                detectTapGestures { o ->
                    onSelect(days[(o.x / size.width * n).toInt().coerceIn(0, n - 1)])
                }
            }
            .pointerInput(days) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> onSelect(days[(o.x / size.width * n).toInt().coerceIn(0, n - 1)]) },
                    onHorizontalDrag = { change, _ ->
                        onSelect(days[(change.position.x / size.width * n).toInt().coerceIn(0, n - 1)])
                    },
                )
            },
    ) {
        val w = size.width
        val h = size.height
        val top = 14.dp.toPx()
        val plotH = h - top
        val slot = w / n
        val barW = (slot * 0.72f).coerceAtLeast(1.5f)
        val radius = CornerRadius(minOf(barW / 2f, 6.dp.toPx()))
        val range = (hi - lo).coerceAtLeast(0.0001f)

        days.forEachIndexed { i, d ->
            val v = values[d]
            val x = i * slot + (slot - barW) / 2f
            if (v == null) {
                drawRoundRect(onSurface.copy(alpha = 0.10f), Offset(x, h - 2.dp.toPx()), Size(barW, 2.dp.toPx()), radius)
            } else {
                val frac = ((v - lo) / range).coerceIn(0.02f, 1f) * grow.value
                val bh = (plotH * frac).coerceAtLeast(3.dp.toPx())
                val a = when (d) {
                    highest -> 1f
                    lowest -> 0.55f
                    else -> 0.78f
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(color.copy(alpha = a), color.copy(alpha = a * 0.55f)),
                        startY = h - bh,
                        endY = h,
                    ),
                    topLeft = Offset(x, h - bh),
                    size = Size(barW, bh),
                    cornerRadius = radius,
                )
                if (d == selected) {
                    drawRoundRect(
                        onSurface.copy(alpha = 0.9f), Offset(x, h - bh), Size(barW, bh), radius,
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
                val dotCenter = Offset(x + barW / 2f, h - bh - 6.dp.toPx())
                if (d == highest) drawCircle(GOLD, 3.dp.toPx(), dotCenter)
                if (d == lowest && lowest != highest) drawCircle(LOW_RED, 3.dp.toPx(), dotCenter)
            }
        }
        if (goal != null && goal >= lo && goal <= hi) {
            val y = h - plotH * (goal - lo) / range
            drawLine(
                color.copy(alpha = 0.6f), Offset(0f, y), Offset(w, y),
                strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
            )
        }
    }
}

private fun buildMetrics(
    stepsSupported: Boolean,
    steps: Map<Long, Int>,
    water: Map<Long, Int>,
    repExerciseIds: Set<Long>,
    logsByDay: Map<Long, List<com.x.myview.data.db.ExerciseLogEntity>>,
    habits: List<HabitEntity>,
    habitDone: Map<Long, Set<Long>>,
    sleepMinutes: Map<Long, Int>,
    weightKg: Map<Long, Float>,
    settings: com.x.myview.data.AppSettings,
    today: Long,
    primary: Color,
): List<Metric> {
    val list = ArrayList<Metric>()
    if (stepsSupported) {
        list += Metric(
            "steps", "Steps", StepsTeal,
            steps.filterValues { it > 0 }.mapValues { it.value.toFloat() },
            settings.stepGoal.toFloat(), { formatSteps(it.toInt()) }, true, "Goal reached",
        )
    }
    list += Metric(
        "water", "Water", WaterBlue,
        water.filterValues { it > 0 }.mapValues { it.value.toFloat() },
        settings.waterGoalMl.toFloat(), { formatMl(it.toInt()) }, true, "Goal reached",
    )
    val reps = HashMap<Long, Float>()
    for ((day, logs) in logsByDay) {
        val sum = logs.filter { it.exerciseId in repExerciseIds }.sumOf { it.amount }
        if (sum > 0) reps[day] = sum.toFloat()
    }
    list += Metric("workout", "Workout", FireOrange, reps, null, { "${it.toInt()} reps" }, true, "")
    val habitPct = HashMap<Long, Float>()
    for (d in today - 89..today) {
        val endMs = LocalDate.ofEpochDay(d + 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val planned = scheduledHabits(habits, d).filter { it.createdAt < endMs }
        if (planned.isEmpty()) continue
        val done = planned.count { d in (habitDone[it.id] ?: emptySet()) }
        habitPct[d] = done * 100f / planned.size
    }
    list += Metric("habits", "Habits", primary, habitPct, 100f, { "${it.toInt()}%" }, false, "All habits done")
    list += Metric(
        "sleep", "Sleep", itemColor(0),
        sleepMinutes.filterValues { it > 0 }.mapValues { it.value.toFloat() },
        settings.sleepGoalMin.toFloat(), { formatSleep(it.toInt()) }, false, "Sleep goal met",
    )
    list += Metric(
        "weight", "Weight", itemColor(3), weightKg,
        if (settings.weightGoalKg > 0f) settings.weightGoalKg else null,
        { formatWeight(it, settings.useLb) }, false, "", zeroBased = false,
    )
    return list
}
