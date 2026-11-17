package com.x.myview.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x.myview.domain.currentStreak
import com.x.myview.domain.exerciseTotal
import com.x.myview.domain.scheduledHabits
import com.x.myview.domain.waterTotal
import com.x.myview.domain.workoutProgress
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.CircleIconButton
import com.x.myview.ui.components.MiniBar
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.theme.WaterBlue
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatDuration
import com.x.myview.util.formatMl
import com.x.myview.util.formatSteps
import com.x.myview.ui.theme.StepsTeal
import com.x.myview.util.niceDate
import com.x.myview.data.db.KIND_TIME

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

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset())) {
        item {
            ScreenHeader("Today", niceDate(today)) {
                CircleIconButton(Icons.Rounded.Settings, "Settings", { onOpen("settings") })
            }
        }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (vm.stepsSupported) {
                        SummaryRing("Steps", formatSteps(stepsToday), stepsToday.toFloat() / settings.stepGoal.coerceAtLeast(1), StepsTeal)
                    }
                    SummaryRing("Habits", "$doneCount/${planned.size}", habitProgress, MaterialTheme.colorScheme.primary)
                    SummaryRing("Water", formatMl(waterMl), waterProgress, WaterBlue)
                    SummaryRing("Workout", if (workedOut.isEmpty()) "-" else "${workedOut.size} done", workout, Color(0xFFFF9A3D))
                }
            }
        }

        if (vm.stepsSupported) {
            item {
                MvCard(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    onClick = { onOpen("steps") },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${formatSteps(stepsToday)} steps",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!stepsGranted) {
                                androidx.compose.material3.FilledTonalButton(onClick = askSteps) { Text("Allow") }
                            } else {
                                Text(
                                    "goal ${formatSteps(settings.stepGoal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        MiniBar(stepsToday.toFloat() / settings.stepGoal.coerceAtLeast(1), StepsTeal)
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
                        androidx.compose.material3.FilledTonalButton(onClick = { vm.addWater(settings.glassMl) }) {
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
                        IconBadge(e.icon, itemColor(1))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            MiniBar(total.toFloat() / e.dailyGoal.coerceAtLeast(1), itemColor(1), Modifier.padding(top = 6.dp))
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
private fun SummaryRing(label: String, value: String, progress: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(progress, size = 76.dp, stroke = 8.dp, color = color) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                androidx.compose.material3.TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
