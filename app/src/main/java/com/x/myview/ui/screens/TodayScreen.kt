package com.x.myview.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.data.db.KIND_TIME
import com.x.myview.domain.currentStreak
import com.x.myview.domain.exerciseTotal
import com.x.myview.domain.scheduledHabits
import com.x.myview.domain.waterTotal
import com.x.myview.domain.workoutProgress
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.CircleIconButton
import com.x.myview.ui.components.GradientCard
import com.x.myview.ui.components.MiniBar
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.StatTile
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.components.rememberAnimatedInt
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.StepsTeal
import com.x.myview.ui.theme.WaterBlue
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatDuration
import com.x.myview.util.formatMl
import com.x.myview.util.formatSleep
import com.x.myview.util.formatSteps
import com.x.myview.util.niceDate
import java.time.LocalTime

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Hey, night owl"
}

private fun scoreMessage(score: Float, goals: Int): String = when {
    goals == 0 -> "Add a habit or log some water to begin."
    score >= 1f -> "Every goal done. Brilliant!"
    score >= 0.66f -> "Almost there, keep going."
    score >= 0.33f -> "Good progress today."
    score > 0f -> "A good start. One step at a time."
    else -> "A fresh day. Let's get started."
}

@Composable
fun TodayScreen(vm: MainViewModel, onOpen: (String) -> Unit, onGoTab: (Int) -> Unit) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val habits by vm.habits.collectAsState()
    val done by vm.habitDone.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val exLogs by vm.exerciseLogs.collectAsState()
    val water by vm.waterLogs.collectAsState()
    val steps by vm.steps.collectAsState()
    val sleepLogs by vm.sleepLogs.collectAsState()
    val moods by vm.moodLogs.collectAsState()

    val stepsToday = steps[today] ?: 0
    var stepsGranted by remember { mutableStateOf(vm.stepsPermissionGranted()) }
    val askSteps = rememberPermissionRequest(android.Manifest.permission.ACTIVITY_RECOGNITION) { ok ->
        stepsGranted = ok
        if (ok) vm.startSteps()
    }

    val planned = remember(habits, today) { scheduledHabits(habits, today) }
    val doneCount = planned.count { today in (done[it.id] ?: emptySet()) }
    val habitProgress = if (planned.isEmpty()) 0f else doneCount.toFloat() / planned.size
    val waterMl = remember(water, today) { waterTotal(water, today) }
    val waterProgress = waterMl.toFloat() / settings.waterGoalMl.coerceAtLeast(1)
    val workout = remember(exercises, exLogs, today) { workoutProgress(exercises, exLogs, today) }
    val workedOut = remember(exercises, exLogs, today) { exercises.filter { exerciseTotal(exLogs, it.id, today) > 0 } }
    val stepsProgress = stepsToday.toFloat() / settings.stepGoal.coerceAtLeast(1)
    val sleepToday = sleepLogs.firstOrNull { it.day == today }
    val moodToday = moods.firstOrNull { it.day == today }

    // The daily score is the average of every goal that applies today (each capped at 100%).
    val goals = buildList {
        if (planned.isNotEmpty()) add(habitProgress.coerceAtMost(1f))
        add(waterProgress.coerceAtMost(1f))
        add(workout.coerceAtMost(1f))
        if (vm.stepsSupported && stepsGranted) add(stepsProgress.coerceAtMost(1f))
    }
    val score = if (goals.isEmpty()) 0f else goals.average().toFloat()
    val completeGoals = goals.count { it >= 1f }
    val scorePercent = rememberAnimatedInt((score * 100).toInt())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomInset()),
    ) {
        item {
            ScreenHeader(greeting(), niceDate(today)) {
                CircleIconButton(Icons.Rounded.Settings, "Settings", { onOpen("settings") })
            }
        }

        item {
            GradientCard(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(0),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressRing(score, size = 118.dp, stroke = 13.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$scorePercent%", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Daily score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            scoreMessage(score, goals.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "$completeGoals of ${goals.size} goals complete",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(1),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    Icons.Rounded.TaskAlt, MaterialTheme.colorScheme.primary,
                    "$doneCount/${planned.size}", "Habits", Modifier.weight(1f),
                    progress = habitProgress, onClick = { onGoTab(1) },
                )
                StatTile(
                    iconFor("water"), WaterBlue,
                    formatMl(rememberAnimatedInt(waterMl)), "Water", Modifier.weight(1f),
                    progress = waterProgress, onClick = { onGoTab(3) },
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    iconFor("fitness"), itemColor(6),
                    if (workedOut.isEmpty()) "-" else "${workedOut.size} done", "Workout", Modifier.weight(1f),
                    progress = workout, onClick = { onGoTab(2) },
                )
                if (vm.stepsSupported) {
                    StatTile(
                        iconFor("walk"), StepsTeal,
                        formatSteps(rememberAnimatedInt(stepsToday)), "Steps", Modifier.weight(1f),
                        progress = stepsProgress, onClick = { onOpen("steps") },
                    )
                } else {
                    StatTile(
                        iconFor("sleep"), itemColor(0),
                        if (sleepToday == null) "-" else formatSleep(sleepToday.minutes), "Sleep", Modifier.weight(1f),
                        progress = (sleepToday?.minutes ?: 0).toFloat() / settings.sleepGoalMin.coerceAtLeast(1),
                        onClick = { onOpen("sleep") },
                    )
                }
            }
        }

        if (vm.stepsSupported && !stepsGranted) {
            item {
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Allow step counting to see your steps here.",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(onClick = askSteps) { Text("Allow") }
                    }
                }
            }
        }

        item { SectionTitle("How are you feeling?") }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    MoodPicker(moodToday?.mood ?: 0, { m -> vm.saveMood(today, m, moodToday?.note ?: "") })
                    TextButton(onClick = { onOpen("mood") }, modifier = Modifier.align(Alignment.End)) {
                        Text(if (moodToday == null) "Open mood diary" else "Add a note")
                    }
                }
            }
        }

        item { SectionTitle("Habits") }
        if (planned.isEmpty()) {
            item {
                EmptyHint(
                    if (habits.isEmpty()) "No habits yet. Add your first one." else "Nothing planned for today.",
                    if (habits.isEmpty()) "Add habit" else null,
                ) { onOpen("habitedit:0") }
            }
        } else {
            items(planned, key = { "habit-${it.id}" }) { h ->
                val days = done[h.id] ?: emptySet()
                HabitRow(
                    habit = h,
                    done = today in days,
                    streak = currentStreak(h.days, days, today),
                    scheduledToday = true,
                    onToggle = { vm.toggleHabit(h.id, today, today !in days) },
                    onClick = { onOpen("habit:${h.id}") },
                )
            }
        }

        item { SectionTitle("Water") }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onGoTab(3) }) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatMl(waterMl)} of ${formatMl(settings.waterGoalMl)}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FilledTonalButton(onClick = { vm.addWater(settings.glassMl) }) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${settings.glassMl} ml")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MiniBar(waterProgress, WaterBlue)
                }
            }
        }

        item { SectionTitle("Workout") }
        if (workedOut.isEmpty()) {
            item { EmptyHint("No exercise logged today.", "Start workout") { onGoTab(2) } }
        } else {
            items(workedOut, key = { "ex-${it.id}" }) { e ->
                val total = exerciseTotal(exLogs, e.id, today)
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpen("ex:${e.id}") }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(e.icon, itemColor(6))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            MiniBar(total.toFloat() / e.dailyGoal.coerceAtLeast(1), itemColor(6), Modifier.padding(top = 6.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (e.kind == KIND_TIME) formatDuration(total) else "$total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHint(text: String, action: String?, onAction: () -> Unit) {
    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) {
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
