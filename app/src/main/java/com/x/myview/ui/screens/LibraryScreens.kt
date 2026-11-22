package com.x.myview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.KIND_REPS
import com.x.myview.data.db.KIND_TIME
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.ExAnim
import com.x.myview.ui.components.ExerciseAnimData
import com.x.myview.ui.components.ExerciseFigure
import com.x.myview.ui.components.ExerciseGuideCard
import com.x.myview.ui.components.LIBRARY
import com.x.myview.ui.components.LIB_CATEGORIES
import com.x.myview.ui.components.LibItem
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.PLANS
import com.x.myview.ui.components.Plan
import com.x.myview.ui.components.REST_SECONDS
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.exerciseKeyFor
import com.x.myview.ui.components.libItem
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatDuration
import kotlinx.coroutines.delay

/** Small rounded picture box with a (still or moving) exercise figure inside. */
@Composable
private fun FigureBox(anim: ExAnim?, accent: Color, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        if (anim != null) {
            ExerciseFigure(anim, accent, Modifier.fillMaxSize(), playing = false, phase0 = 0.38f)
        }
    }
}

private fun amountLabel(item: LibItem?, amount: Int): String =
    if (item?.timed == true) "${amount}s" else "$amount reps"

// ---------------------------------------------------------------------------------------------
//  Library
// ---------------------------------------------------------------------------------------------

@Composable
fun LibraryScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val exercises by vm.exercises.collectAsState()
    var cat by rememberSaveable { mutableStateOf("All") }
    val accent = itemColor(6)
    val shown = remember(cat) { if (cat == "All") LIBRARY else LIBRARY.filter { it.category == cat } }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Exercise library", onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = bottomInset())) {
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ChipFlow {
                        ChoiceChip("All (${LIBRARY.size})", cat == "All", { cat = "All" })
                        LIB_CATEGORIES.forEach { c -> ChoiceChip(c, cat == c, { cat = c }) }
                    }
                }
            }
            items(shown, key = { it.key }) { item ->
                val have = exercises.any { exerciseKeyFor(it.name) == item.key }
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpen("lib:${item.key}") }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FigureBox(ExerciseAnimData.get(item.key), accent, 76)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                item.category + " • " + item.level + (if (item.timed) " • Timed" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (have) {
                            Icon(Icons.Rounded.Check, "In your list", tint = accent, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryDetailScreen(vm: MainViewModel, key: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val exercises by vm.exercises.collectAsState()
    val item = libItem(key)
    val accent = itemColor(6)
    Column(Modifier.fillMaxSize()) {
        BackHeader(item?.name ?: "Exercise", onBack)
        if (item == null) return@Column
        val mine = exercises.firstOrNull { exerciseKeyFor(it.name) == item.key }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            ExerciseGuideCard(item.name, accent, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        item.category + " • " + item.level + " • " +
                            (if (item.timed) "goal ${formatDuration(item.goal)}" else "goal ${item.goal} reps"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (mine == null) {
                        Button(
                            onClick = {
                                vm.saveExercise(
                                    ExerciseEntity(
                                        name = item.name,
                                        kind = if (item.timed) KIND_TIME else KIND_REPS,
                                        icon = item.icon,
                                        dailyGoal = item.goal,
                                        sortOrder = exercises.size,
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Add to my exercises") }
                    } else {
                        OutlinedButton(onClick = { onOpen("ex:${mine.id}") }, modifier = Modifier.fillMaxWidth()) {
                            Text("In your list: open and count")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Plans (routines) and the guided player
// ---------------------------------------------------------------------------------------------

@Composable
fun PlansScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val accent = itemColor(6)
    Column(Modifier.fillMaxSize()) {
        BackHeader("Workout plans", onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = bottomInset())) {
            items(PLANS, key = { it.id }) { p ->
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).staggerIn(p.id), onClick = { onOpen("plan:${p.id}") }) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(p.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.steps.take(5).forEach { st -> FigureBox(ExerciseAnimData.get(st.key), accent, 48) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${p.steps.size} exercises • about ${p.minutes} min • ${p.level}",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

private class Slot(val key: String, val amount: Int, val timed: Boolean, val set: Int, val sets: Int)

@Composable
fun PlanScreen(vm: MainViewModel, id: Int, onBack: () -> Unit) {
    val plan = PLANS.firstOrNull { it.id == id }
    var running by rememberSaveable { mutableStateOf(false) }
    val accent = itemColor(6)
    Column(Modifier.fillMaxSize()) {
        BackHeader(plan?.title ?: "Plan", { if (running) running = false else onBack() })
        if (plan == null) return@Column
        if (!running) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        "${plan.subtitle} • about ${plan.minutes} min • rest $REST_SECONDS s between sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SectionTitle("Exercises")
                plan.steps.forEach { st ->
                    val item = libItem(st.key)
                    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            FigureBox(ExerciseAnimData.get(st.key), accent, 60)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item?.name ?: st.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${st.sets} sets × ${amountLabel(item, st.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(onClick = { running = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start workout")
                    }
                }
            }
        } else {
            PlanRunner(vm, plan, onExit = { running = false })
        }
    }
}

@Composable
private fun PlanRunner(vm: MainViewModel, plan: Plan, onExit: () -> Unit) {
    val accent = itemColor(6)
    val exercises by vm.exercises.collectAsState()
    val slots = remember(plan) {
        plan.steps.flatMap { st ->
            val timed = libItem(st.key)?.timed == true
            (1..st.sets).map { Slot(st.key, st.amount, timed, it, st.sets) }
        }
    }
    var idx by rememberSaveable { mutableIntStateOf(0) }
    var resting by rememberSaveable { mutableStateOf(false) }
    var left by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var logged by rememberSaveable { mutableStateOf(false) }
    val slot = slots[idx.coerceIn(0, slots.lastIndex)]

    val completeWork: () -> Unit = {
        if (idx >= slots.lastIndex) {
            finished = true
        } else {
            resting = true
        }
    }
    val advance: () -> Unit = {
        if (idx < slots.lastIndex) {
            idx += 1
        }
        resting = false
    }

    LaunchedEffect(idx, resting, finished) {
        if (finished) return@LaunchedEffect
        if (resting) {
            left = REST_SECONDS
            while (left > 0) {
                delay(1000)
                left -= 1
            }
            advance()
        } else if (slot.timed) {
            left = slot.amount
            while (left > 0) {
                delay(1000)
                left -= 1
            }
            completeWork()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (finished) {
            // ---- Summary ----
            val totals = remember(plan) {
                val m = LinkedHashMap<String, Int>()
                plan.steps.forEach { st -> m[st.key] = (m[st.key] ?: 0) + st.amount * st.sets }
                m
            }
            Spacer(Modifier.height(20.dp))
            Text("Workout complete", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${slots.size} sets of ${plan.steps.size} exercises. Great job!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            MvCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    totals.forEach { (k, total) ->
                        val item = libItem(k)
                        Row(Modifier.fillMaxWidth()) {
                            Text(item?.name ?: k, Modifier.weight(1f))
                            Text(
                                if (item?.timed == true) formatDuration(total) else "$total reps",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    totals.forEach { (k, total) ->
                        val item = libItem(k)
                        val want = if (item?.timed == true) KIND_TIME else KIND_REPS
                        val ex = exercises.firstOrNull { exerciseKeyFor(it.name) == k && it.kind == want }
                        if (ex != null) vm.addExerciseLog(ex.id, total)
                    }
                    logged = true
                },
                enabled = !logged,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (logged) "Saved to your exercises" else "Save to my exercises") }
            Text(
                "Only exercises that are already in your list are saved.",
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onExit) { Text("Done") }
            return@Column
        }

        val item = libItem(slot.key)
        val progress = idx.toFloat() / slots.size
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            "Set ${idx + 1} of ${slots.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        if (resting) {
            val next = slots[(idx + 1).coerceAtMost(slots.lastIndex)]
            val nextItem = libItem(next.key)
            Spacer(Modifier.height(30.dp))
            Text("Rest", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(left), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(10.dp))
            Text("Next: ${nextItem?.name ?: next.key}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Set ${next.set} of ${next.sets} • ${amountLabel(nextItem, next.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FigureBox(ExerciseAnimData.get(next.key), accent, 120)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { left += 10 }, modifier = Modifier.weight(1f)) { Text("+10 s") }
                Button(onClick = { advance() }, modifier = Modifier.weight(1f)) { Text("Skip rest") }
            }
        } else {
            val anim = ExerciseAnimData.get(slot.key)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accent.copy(alpha = 0.10f)),
            ) {
                if (anim != null) ExerciseFigure(anim, accent, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(14.dp))
            Text(item?.name ?: slot.key, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "Set ${slot.set} of ${slot.sets}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            if (slot.timed) {
                Text(formatDuration(left), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { completeWork() }, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
            } else {
                Text("${slot.amount} reps", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { completeWork() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onExit) { Text("Stop workout") }
    }
}
