package com.x.myview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.GradientCard
import com.x.myview.ui.components.LineChart
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.WeekBars
import com.x.myview.ui.components.bounceClick
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.components.rememberAnimatedInt
import com.x.myview.ui.components.rememberHaptic
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatSleep
import com.x.myview.util.formatWeight
import com.x.myview.util.kgToUnit
import com.x.myview.util.niceDate
import com.x.myview.util.oneLetterDay
import com.x.myview.util.unitToKg
import com.x.myview.util.weekStart
import java.util.Locale

private val StarGold = Color(0xFFFFC24A)

@Composable
private fun StarRow(quality: Int, onChange: (Int) -> Unit) {
    val haptic = rememberHaptic()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..5) {
            Icon(
                iconFor("star"),
                contentDescription = "$i stars",
                tint = if (i <= quality) StarGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                modifier = Modifier
                    .size(38.dp)
                    .bounceClick {
                        haptic()
                        onChange(if (quality == i) 0 else i)
                    },
            )
        }
    }
}

// ---------------------------------------------------------------- Sleep

@Composable
fun SleepScreen(vm: MainViewModel, onBack: () -> Unit) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val logs by vm.sleepLogs.collectAsState()
    var dayBack by rememberSaveable { mutableIntStateOf(0) }
    val day = today - dayBack
    val existing = logs.firstOrNull { it.day == day }
    var minutes by remember(day, existing?.minutes) { mutableIntStateOf(existing?.minutes ?: 450) }
    var quality by remember(day, existing?.quality) { mutableIntStateOf(existing?.quality ?: 0) }

    val byDay = remember(logs) { logs.associate { it.day to it.minutes } }
    val week = remember(today) { (today - 6..today).toList() }
    val weekValues = week.map { byDay[it] ?: 0 }
    val logged = weekValues.filter { it > 0 }
    val average = if (logged.isEmpty()) 0 else logged.sum() / logged.size
    val goal = settings.sleepGoalMin
    val color = itemColor(0)

    Column(Modifier.fillMaxSize()) {
        BackHeader("Sleep", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (back in 6 downTo 0) {
                    val d = today - back
                    val on = back == dayBack
                    val has = byDay.containsKey(d)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .bounceClick { dayBack = back },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                oneLetterDay(d),
                                fontWeight = FontWeight.Bold,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (has) color else Color.Transparent),
                        )
                    }
                }
            }

            GradientCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (dayBack == 0) "Last night" else niceDate(day),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(iconFor("sleep"), null, tint = color, modifier = Modifier.size(34.dp))
                    Text(formatSleep(minutes), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    ChipFlow {
                        ChoiceChip("-30m", false, { minutes = (minutes - 30).coerceAtLeast(30) })
                        ChoiceChip("-15m", false, { minutes = (minutes - 15).coerceAtLeast(30) })
                        ChoiceChip("+15m", false, { minutes = (minutes + 15).coerceAtMost(960) })
                        ChoiceChip("+30m", false, { minutes = (minutes + 30).coerceAtMost(960) })
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("How did you sleep?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    StarRow(quality) { quality = it }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.saveSleep(day, minutes, quality) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (existing == null) "Save sleep" else "Update sleep")
                    }
                    if (existing != null) {
                        TextButton(onClick = { vm.deleteSleep(day) }) { Text("Delete this entry") }
                    }
                }
            }

            SectionTitle("Last 7 days")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    WeekBars(
                        days = week,
                        values = weekValues,
                        color = color,
                        goal = goal,
                        valueLabel = { String.format(Locale.US, "%.1fh", it / 60f) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (logged.isEmpty()) "No sleep logged this week" else "Average: ${formatSleep(average)}  |  Goal: ${formatSleep(goal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle("Sleep goal")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hours per night", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Stepper(
                        formatSleep(goal),
                        { vm.updateSettings { it.copy(sleepGoalMin = (it.sleepGoalMin - 30).coerceAtLeast(240)) } },
                        { vm.updateSettings { it.copy(sleepGoalMin = (it.sleepGoalMin + 30).coerceAtMost(720)) } },
                    )
                }
            }

            if (logs.isNotEmpty()) {
                SectionTitle("History")
                logs.sortedByDescending { it.day }.take(10).forEachIndexed { i, l ->
                    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 3.dp).staggerIn(i)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(formatSleep(l.minutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    niceDate(l.day) + if (l.quality > 0) "  |  " + "★".repeat(l.quality) else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { vm.deleteSleep(l.day) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Weight

@Composable
fun WeightScreen(vm: MainViewModel, onBack: () -> Unit) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val logs by vm.weightLogs.collectAsState()
    val lb = settings.useLb
    val unit = if (lb) "lb" else "kg"
    val latest = logs.lastOrNull()
    val first = logs.firstOrNull()
    val goalKg = settings.weightGoalKg

    var text by remember(latest?.day, latest?.kg, lb) {
        mutableStateOf(latest?.let { String.format(Locale.US, "%.1f", kgToUnit(it.kg, lb)) } ?: "")
    }
    val parsed = text.toFloatOrNull()
    val parsedKg = parsed?.let { unitToKg(it, lb) }
    val valid = parsedKg != null && parsedKg in 20f..500f
    val color = itemColor(3)

    fun nudge(delta: Float) {
        val base = text.toFloatOrNull() ?: latest?.let { kgToUnit(it.kg, lb) } ?: if (lb) 150f else 70f
        text = String.format(Locale.US, "%.1f", (base + delta).coerceIn(0f, 999f))
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackHeader("Weight", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            GradientCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Current weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (latest == null) "-" else formatWeight(latest.kg, lb),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (latest != null && first != null && logs.size > 1) {
                        val change = kgToUnit(latest.kg, lb) - kgToUnit(first.kg, lb)
                        Text(
                            String.format(Locale.US, "%+.1f %s since %s", change, unit, niceDate(first.day)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (latest != null && goalKg > 0f) {
                        val toGo = kgToUnit(latest.kg, lb) - kgToUnit(goalKg, lb)
                        Text(
                            if (kotlin.math.abs(toGo) < 0.05f) "Goal reached!" else String.format(Locale.US, "%.1f %s to goal", kotlin.math.abs(toGo), unit),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            SectionTitle("Log today")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' }.take(6) },
                        label = { Text("Weight ($unit)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ChipFlow {
                        ChoiceChip("-1", false, { nudge(-1f) })
                        ChoiceChip("-0.1", false, { nudge(-0.1f) })
                        ChoiceChip("+0.1", false, { nudge(0.1f) })
                        ChoiceChip("+1", false, { nudge(1f) })
                    }
                    Button(
                        onClick = { if (parsedKg != null) vm.saveWeight(today, parsedKg) },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save for today") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Unit", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        ChipFlow {
                            ChoiceChip("kg", !lb, { vm.updateSettings { it.copy(useLb = false) } })
                            ChoiceChip("lb", lb, { vm.updateSettings { it.copy(useLb = true) } })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Goal weight", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val step = if (lb) 1f else 0.5f
                        Stepper(
                            if (goalKg <= 0f) "Not set" else formatWeight(goalKg, lb),
                            {
                                vm.updateSettings {
                                    val next = kgToUnit(it.weightGoalKg, lb) - step
                                    it.copy(weightGoalKg = if (next < 20f) 0f else unitToKg(next, lb))
                                }
                            },
                            {
                                vm.updateSettings {
                                    val base = if (it.weightGoalKg <= 0f) (latest?.kg ?: 70f) else it.weightGoalKg
                                    it.copy(weightGoalKg = unitToKg(kgToUnit(base, lb) + step, lb).coerceAtMost(500f))
                                }
                            },
                        )
                    }
                }
            }

            SectionTitle("Trend")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                val recent = logs.takeLast(30)
                if (recent.size < 2) {
                    Text(
                        "Log your weight on at least two days to see the trend.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LineChart(
                        points = recent.map { it.day to kgToUnit(it.kg, lb) },
                        color = color,
                        goal = if (goalKg > 0f) kgToUnit(goalKg, lb) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(12.dp),
                    )
                }
            }

            if (logs.isNotEmpty()) {
                SectionTitle("History")
                logs.sortedByDescending { it.day }.take(10).forEachIndexed { i, l ->
                    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 3.dp).staggerIn(i)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(formatWeight(l.kg, lb), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(niceDate(l.day), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { vm.deleteWeight(l.day) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Mood

@Composable
fun MoodScreen(vm: MainViewModel, onBack: () -> Unit) {
    val today by vm.today.collectAsState()
    val moods by vm.moodLogs.collectAsState()
    val todayLog = moods.firstOrNull { it.day == today }
    var mood by remember(todayLog?.mood) { mutableIntStateOf(todayLog?.mood ?: 0) }
    var note by remember(todayLog?.note) { mutableStateOf(todayLog?.note ?: "") }

    val byDay = remember(moods) { moods.associateBy { it.day } }
    val weekMoods = (today - 6..today).mapNotNull { byDay[it]?.mood }
    val average = if (weekMoods.isEmpty()) 0 else Math.round(weekMoods.average()).toInt()
    val start = weekStart(today) - 21

    Column(Modifier.fillMaxSize().imePadding()) {
        BackHeader("Mood diary", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            GradientCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How are you feeling today?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    MoodPicker(mood, { mood = it; vm.saveMood(today, it, note) })
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(200) },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                    )
                    Button(
                        onClick = { vm.saveMood(today, mood, note.trim()) },
                        enabled = mood > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (todayLog == null) "Save mood" else "Update mood") }
                    if (todayLog != null) {
                        TextButton(onClick = { vm.deleteMood(today); mood = 0; note = "" }) { Text("Clear today") }
                    }
                }
            }

            SectionTitle("Last 4 weeks")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.x.myview.util.DAY_NAMES.forEach {
                            Text(
                                it.take(1),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    for (w in 0 until 4) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (r in 0 until 7) {
                                val d = start + w * 7 + r
                                val m = byDay[d]?.mood ?: 0
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                d > today -> Color.Transparent
                                                m > 0 -> Moods.color(m).copy(alpha = 0.85f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (m > 0) Text(Moods.emoji(m), fontSize = 15.sp)
                                }
                            }
                        }
                    }
                    Text(
                        if (weekMoods.isEmpty()) "No mood logged this week" else "Weekly average: ${Moods.emoji(average)} ${Moods.label(average)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            val notes = moods.filter { it.note.isNotBlank() }.sortedByDescending { it.day }.take(10)
            if (notes.isNotEmpty()) {
                SectionTitle("Recent notes")
                notes.forEachIndexed { i, n ->
                    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 3.dp).staggerIn(i)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text(Moods.emoji(n.mood), fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(niceDate(n.day), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(n.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
