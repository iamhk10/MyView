package com.x.myview.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.KIND_REPS
import com.x.myview.data.db.KIND_TIME
import com.x.myview.domain.exerciseByDay
import com.x.myview.domain.exerciseTotal
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.CircleIconButton
import com.x.myview.ui.components.EXERCISE_ICON_KEYS
import com.x.myview.ui.components.MiniBar
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.WeekBars
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatDuration
import com.x.myview.util.timeOfDay
import kotlinx.coroutines.delay

private fun amountText(e: ExerciseEntity, v: Int): String =
    if (e.kind == KIND_TIME) formatDuration(v) else "$v"

@Composable
fun WorkoutScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val today by vm.today.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val logs by vm.exerciseLogs.collectAsState()

    val totalReps = remember(exercises, logs, today) {
        exercises.filter { it.kind == KIND_REPS }.sumOf { exerciseTotal(logs, it.id, today) }
    }
    val totalSeconds = remember(exercises, logs, today) {
        exercises.filter { it.kind == KIND_TIME }.sumOf { exerciseTotal(logs, it.id, today) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset())) {
        item {
            ScreenHeader("Workout", "Tap an exercise to count") {
                CircleIconButton(Icons.Rounded.Add, "Add exercise", { onOpen("exedit:0") })
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill("$totalReps", "Reps today", Modifier.weight(1f))
                StatPill(formatDuration(totalSeconds), "Timed today", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Exercises") }
        items(exercises, key = { it.id }) { e ->
            val total = exerciseTotal(logs, e.id, today)
            val color = itemColor(if (total >= e.dailyGoal) 4 else 6)
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpen("ex:${e.id}") }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(e.icon, color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        MiniBar(total.toFloat() / e.dailyGoal.coerceAtLeast(1), color)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(amountText(e, total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "/ ${amountText(e, e.dailyGoal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(vm: MainViewModel, id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val today by vm.today.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val logs by vm.exerciseLogs.collectAsState()
    val e = exercises.firstOrNull { it.id == id }

    Column(Modifier.fillMaxSize()) {
        BackHeader(e?.name ?: "Exercise", onBack) {
            CircleIconButton(Icons.Rounded.Edit, "Edit", onEdit)
        }
        if (e == null) return@Column
        val color = itemColor(6)
        val byDay = remember(logs, id) { exerciseByDay(logs, id) }
        val todayTotal = byDay[today] ?: 0
        val best = byDay.values.maxOrNull() ?: 0
        val week = remember(today) { (today - 6..today).toList() }
        val todayLogs = remember(logs, today, id) {
            logs.filter { it.exerciseId == id && it.day == today }.sortedByDescending { it.createdAt }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressRing(todayTotal.toFloat() / e.dailyGoal.coerceAtLeast(1), size = 120.dp, stroke = 11.dp, color = color) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(amountText(e, todayTotal), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "of ${amountText(e, e.dailyGoal)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Best day: ${amountText(e, best)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (e.kind == KIND_REPS) {
                RepCounter(onSave = { vm.addExerciseLog(id, it) })
            } else {
                Stopwatch(onSave = { vm.addExerciseLog(id, it) })
            }

            SectionTitle("Today's sets")
            if (todayLogs.isEmpty()) {
                EmptyHint("Nothing saved yet today.", null) { }
            } else {
                todayLogs.forEach { l ->
                    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                amountText(e, l.amount),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                timeOfDay(l.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { vm.deleteExerciseLog(l.id) }) { Text("Remove") }
                        }
                    }
                }
            }

            SectionTitle("Last 7 days")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Box(Modifier.padding(14.dp)) {
                    WeekBars(
                        days = week,
                        values = week.map { byDay[it] ?: 0 },
                        color = color,
                        goal = e.dailyGoal,
                        valueLabel = { if (e.kind == KIND_TIME) formatDuration(it) else it.toString() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepCounter(onSave: (Int) -> Unit) {
    var count by rememberSaveable { mutableIntStateOf(0) }
    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("This set", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                RoundAction(Icons.Rounded.Remove, "Minus one") { if (count > 0) count-- }
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { count++ },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$count",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
                RoundAction(Icons.Rounded.Add, "Plus one") { count++ }
            }
            Text(
                "Tap the big circle for each rep",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("+5", false, { count += 5 })
                ChoiceChip("+10", false, { count += 10 })
                ChoiceChip("Reset", false, { count = 0 })
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onSave(count)
                    count = 0
                },
                enabled = count > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save set") }
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Stopwatch(onSave: (Int) -> Unit) {
    var accMs by rememberSaveable { mutableLongStateOf(0L) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) } // 0 = not running
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startedAt) {
        while (startedAt != 0L) {
            tick = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val running = startedAt != 0L
    val elapsedMs = accMs + if (running) (tick - startedAt).coerceAtLeast(0L) else 0L
    val seconds = (elapsedMs / 1000).toInt()

    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                formatDuration(seconds),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Icons.Rounded.Refresh, "Reset") {
                    accMs = 0L
                    startedAt = 0L
                }
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (running) {
                                accMs += (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
                                startedAt = 0L
                            } else {
                                val now = SystemClock.elapsedRealtime()
                                tick = now
                                startedAt = now
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (running) "Pause" else "Start",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onSave(seconds)
                    accMs = 0L
                    startedAt = 0L
                },
                enabled = seconds > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save time") }
        }
    }
}

@Composable
fun ExerciseEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val exercises by vm.exercises.collectAsState()
    val existing = exercises.firstOrNull { it.id == id }

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var kind by remember(existing?.id) { mutableIntStateOf(existing?.kind ?: KIND_REPS) }
    var icon by remember(existing?.id) { mutableStateOf(existing?.icon ?: "pushup") }
    var goalText by remember(existing?.id) {
        mutableStateOf(
            when {
                existing == null -> "20"
                existing.kind == KIND_TIME -> (existing.dailyGoal / 60).coerceAtLeast(1).toString()
                else -> existing.dailyGoal.toString()
            },
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val color = itemColor(6)

    Column(Modifier.fillMaxSize().imePadding()) {
        BackHeader(if (id == 0L) "New exercise" else "Edit exercise", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Exercise name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (existing == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Counted by", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip("Reps", kind == KIND_REPS, { kind = KIND_REPS })
                        ChoiceChip("Time", kind == KIND_TIME, { kind = KIND_TIME })
                    }
                }
            }
            NumberField(
                goalText,
                { goalText = it },
                if (kind == KIND_TIME) "Daily goal (minutes)" else "Daily goal (reps)",
                Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Icon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconPicker(EXERCISE_ICON_KEYS, icon, color) { icon = it }
            }
            Spacer(Modifier.height(4.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val goalNumber = goalText.toIntOrNull() ?: 0
            Button(
                onClick = {
                    val goal = if (kind == KIND_TIME) goalNumber * 60 else goalNumber
                    val base = existing ?: ExerciseEntity(
                        name = "", kind = kind, icon = icon, dailyGoal = goal, sortOrder = exercises.size,
                    )
                    vm.saveExercise(base.copy(name = name.trim(), icon = icon, dailyGoal = goal))
                    onBack()
                },
                enabled = name.isNotBlank() && goalNumber > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete exercise")
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${existing.name}\"?") },
            text = { Text("All its saved sets will be removed too.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteExercise(existing.id)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
