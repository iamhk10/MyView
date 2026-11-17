package com.x.myview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.myview.data.db.HabitEntity
import com.x.myview.domain.bestStreak
import com.x.myview.domain.completionRate
import com.x.myview.domain.currentStreak
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.CheckCircle
import com.x.myview.ui.components.CircleIconButton
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.ALL_DAYS
import com.x.myview.util.daysSummary
import com.x.myview.util.isScheduled
import com.x.myview.util.monthYear
import com.x.myview.util.weekStart
import com.x.myview.ui.components.HABIT_ICON_KEYS

@Composable
fun HabitsScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val today by vm.today.collectAsState()
    val habits by vm.habits.collectAsState()
    val done by vm.habitDone.collectAsState()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset())) {
        item {
            ScreenHeader("Habits", "${habits.size} habits") {
                CircleIconButton(Icons.Rounded.Add, "Add habit", { onOpen("habitedit:0") })
            }
        }
        if (habits.isEmpty()) {
            item { EmptyHint("Build a streak. Add your first habit.", "Add habit") { onOpen("habitedit:0") } }
        }
        items(habits, key = { it.id }) { h ->
            val days = done[h.id] ?: emptySet()
            HabitRow(
                habit = h,
                done = today in days,
                streak = currentStreak(h.days, days, today),
                scheduledToday = isScheduled(h.days, today),
                onToggle = { vm.toggleHabit(h.id, today, today !in days) },
                onClick = { onOpen("habit:${h.id}") },
            )
        }
    }
}

@Composable
fun HabitDetailScreen(vm: MainViewModel, id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val today by vm.today.collectAsState()
    val habits by vm.habits.collectAsState()
    val doneMap by vm.habitDone.collectAsState()
    val habit = habits.firstOrNull { it.id == id }

    Column(Modifier.fillMaxSize()) {
        BackHeader(habit?.name ?: "Habit", onBack) {
            CircleIconButton(Icons.Rounded.Edit, "Edit", onEdit)
        }
        if (habit == null) return@Column
        val color = itemColor(habit.colorIndex)
        val done = doneMap[id] ?: emptySet()
        val streak = currentStreak(habit.days, done, today)
        val best = bestStreak(habit.days, done, today)
        val rate = completionRate(habit.days, done, today - 29, today)

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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill("$streak", "Current streak", Modifier.weight(1f))
                StatPill("$best", "Best streak", Modifier.weight(1f))
                StatPill("${(rate * 100).toInt()}%", "Last 30 days", Modifier.weight(1f))
            }
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(habit.icon, color, size = 52)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(daysSummary(habit.days), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (!isScheduled(habit.days, today)) "Rest day today"
                            else if (today in done) "Done today" else "Not done yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isScheduled(habit.days, today)) {
                        CheckCircle(today in done, color, { vm.toggleHabit(id, today, today !in done) }, size = 48.dp)
                    }
                }
            }
            SectionTitle("Last 12 weeks")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        monthYear(today),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Heatmap(habit, done, today, color)
                }
            }
            SectionTitle("Edit past days")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Forgot to tick a day? Tap it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (back in 6 downTo 0) {
                            val d = today - back
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    com.x.myview.util.oneLetterDay(d),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                if (isScheduled(habit.days, d)) {
                                    CheckCircle(d in done, color, { vm.toggleHabit(id, d, d !in done) }, size = 36.dp)
                                } else {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 12 weeks as a grid: one column per week, Monday at the top. */
@Composable
private fun Heatmap(habit: HabitEntity, done: Set<Long>, today: Long, color: Color) {
    val start = weekStart(today) - 11 * 7
    val off = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (w in 0 until 12) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until 7) {
                    val d = start + w * 7 + r
                    val c = when {
                        d > today -> Color.Transparent
                        d in done -> color
                        isScheduled(habit.days, d) -> off.copy(alpha = 0.12f)
                        else -> off.copy(alpha = 0.04f)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(c),
                    )
                }
            }
        }
    }
}

@Composable
fun HabitEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val habits by vm.habits.collectAsState()
    val existing = habits.firstOrNull { it.id == id }

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var icon by remember(existing?.id) { mutableStateOf(existing?.icon ?: "check") }
    var colorIndex by remember(existing?.id) { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var days by remember(existing?.id) { mutableIntStateOf(existing?.days ?: ALL_DAYS) }
    var reminderMin by remember(existing?.id) { mutableIntStateOf(existing?.reminderMin ?: -1) }
    var confirmDelete by remember { mutableStateOf(false) }
    val color = itemColor(colorIndex)
    val askNotifications = rememberNotificationRequest()

    Column(Modifier.fillMaxSize().imePadding()) {
        BackHeader(if (id == 0L) "New habit" else "Edit habit", onBack)
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
                label = { Text("Habit name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Icon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconPicker(HABIT_ICON_KEYS, icon, color) { icon = it }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Colour", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ColorPicker(colorIndex) { colorIndex = it }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Repeat on", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                DayPicker(days, color) { days = it }
                Text(
                    if (days == 0) "Pick at least one day" else daysSummary(days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Reminder",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = reminderMin >= 0,
                        onCheckedChange = { on ->
                            if (on) {
                                reminderMin = 20 * 60
                                askNotifications()
                            } else {
                                reminderMin = -1
                            }
                        },
                    )
                }
                if (reminderMin >= 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            com.x.myview.util.formatClock(reminderMin),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        TimeStepper(reminderMin) { reminderMin = it }
                    }
                }
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
            Button(
                onClick = {
                    val base = existing ?: HabitEntity(
                        name = "", icon = icon, colorIndex = colorIndex, days = days,
                        createdAt = System.currentTimeMillis(),
                        sortOrder = habits.size,
                    )
                    vm.saveHabit(base.copy(name = name.trim(), icon = icon, colorIndex = colorIndex, days = days, reminderMin = reminderMin))
                    onBack()
                },
                enabled = name.isNotBlank() && days != 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete habit")
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${existing.name}\"?") },
            text = { Text("Its streak and history will be removed too.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteHabit(existing.id)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
