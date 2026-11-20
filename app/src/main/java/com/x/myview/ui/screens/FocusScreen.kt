package com.x.myview.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.WeekBars
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.FireOrange
import com.x.myview.ui.theme.NumberFont
import kotlinx.coroutines.delay

/** A countdown for deep work. It keeps running if you leave the screen, and the minutes are saved per day. */
@Composable
fun FocusScreen(vm: MainViewModel, onBack: () -> Unit) {
    val today by vm.today.collectAsState()
    val end by vm.focusEnd.collectAsState()
    val planned by vm.focusPlanned.collectAsState()
    val focus by vm.focusMinutes.collectAsState()

    var minutes by rememberSaveable { mutableIntStateOf(planned.coerceIn(5, 180)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var finished by remember { mutableStateOf(false) }
    val running = end > 0L

    LaunchedEffect(end) {
        while (end > 0L) {
            now = System.currentTimeMillis()
            if (vm.finishFocusIfDue()) {
                finished = true
                break
            }
            delay(500)
        }
    }

    // Keep the screen on while the timer runs.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val totalMs = (if (running) planned else minutes) * 60_000L
    val remainingMs = if (running) (end - now).coerceIn(0L, totalMs) else totalMs
    val progress = if (running && totalMs > 0) 1f - remainingMs.toFloat() / totalMs else 0f
    val secs = ((remainingMs + 999) / 1000).toInt()
    val clock = String.format("%02d:%02d", secs / 60, secs % 60)

    val week = remember(today) { (today - 6..today).toList() }
    val weekValues = week.map { focus[it] ?: 0 }
    val todayMin = focus[today] ?: 0

    Column(Modifier.fillMaxSize()) {
        BackHeader("Focus timer", onBack)
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
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressRing(progress, size = 240.dp, stroke = 16.dp, color = FireOrange) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(clock, fontSize = 48.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
                            Text(
                                if (running) "stay with it" else if (finished) "session done" else "ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (running) {
                        OutlinedButton(onClick = { vm.cancelFocus() }, modifier = Modifier.fillMaxWidth()) { Text("Give up") }
                    } else {
                        ChipFlow {
                            listOf(15, 25, 45, 60).forEach { m ->
                                ChoiceChip("$m min", minutes == m, { minutes = m })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Stepper(
                            "$minutes min",
                            { minutes = (minutes - 5).coerceAtLeast(5) },
                            { minutes = (minutes + 5).coerceAtMost(180) },
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                finished = false
                                vm.startFocus(minutes)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (finished) "Start another" else "Start focus") }
                    }
                    if (finished && !running) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Great work. Those minutes are saved.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill("$todayMin min", "Today", Modifier.weight(1f))
                StatPill("${weekValues.sum()} min", "Last 7 days", Modifier.weight(1f))
                StatPill("${focus.values.sum()} min", "All time", Modifier.weight(1f))
            }
            SectionTitle("Last 7 days")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).staggerIn(1)) {
                Column(Modifier.padding(14.dp)) {
                    WeekBars(
                        days = week,
                        values = weekValues,
                        color = FireOrange,
                        goal = 0,
                        valueLabel = { "$it" },
                    )
                }
            }
            Text(
                "The timer keeps counting if you leave this screen. If it ends while the app is closed, the minutes are saved the next time you open this screen.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
