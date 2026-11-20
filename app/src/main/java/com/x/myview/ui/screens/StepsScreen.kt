package com.x.myview.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.WeekBars
import com.x.myview.ui.theme.StepsTeal
import com.x.myview.util.formatSteps
import java.util.Locale

@Composable
fun StepsScreen(vm: MainViewModel, onBack: () -> Unit, onHistory: () -> Unit = {}) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val steps by vm.steps.collectAsState()
    var granted by remember { mutableStateOf(vm.stepsPermissionGranted()) }
    val ask = rememberPermissionRequest(android.Manifest.permission.ACTIVITY_RECOGNITION) { ok ->
        granted = ok
        if (ok) vm.startSteps()
    }

    val goal = settings.stepGoal
    val todaySteps = steps[today] ?: 0
    val week = remember(today) { (today - 6..today).toList() }
    val weekValues = week.map { steps[it] ?: 0 }
    val km = todaySteps * 0.00076
    val kcal = (todaySteps * 0.04).toInt()

    Column(Modifier.fillMaxSize()) {
        BackHeader("Steps", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            if (!granted) {
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Allow step counting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Android needs the Physical activity permission to read your phone's step sensor. Nothing leaves your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = ask, modifier = Modifier.fillMaxWidth()) { Text("Allow") }
                    }
                }
            }
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressRing(todaySteps.toFloat() / goal.coerceAtLeast(1), size = 170.dp, stroke = 14.dp, color = StepsTeal) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatSteps(todaySteps), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "of ${formatSteps(goal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (todaySteps >= goal) "Goal reached. Well done!" else "${formatSteps(goal - todaySteps)} steps to go",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill(String.format(Locale.US, "%.2f km", km), "Distance (approx.)", Modifier.weight(1f))
                StatPill("$kcal", "Calories (approx.)", Modifier.weight(1f))
                StatPill("${(todaySteps * 100 / goal.coerceAtLeast(1)).coerceAtMost(999)}%", "Of goal", Modifier.weight(1f))
            }
            SectionTitle("Last 7 days")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    WeekBars(
                        days = week,
                        values = weekValues,
                        color = StepsTeal,
                        goal = goal,
                        valueLabel = { if (it >= 1000) String.format(Locale.US, "%.1fk", it / 1000f) else "$it" },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Total this week: ${formatSteps(weekValues.sum())}  |  Best day: ${formatSteps(weekValues.maxOrNull() ?: 0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) { Text("Full history, highest and lowest") }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    "Steps are counted by your phone's own sensor, even when the app is closed. With \"Live step counting\" on (Settings) the count and widget update within seconds; otherwise MyView adds them up every ~15 minutes. Counting starts from the moment you first allow the permission. Distance and calories are rough estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
