package com.x.myview.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.domain.waterByDay
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.WaterWave
import com.x.myview.ui.components.WeekBars
import com.x.myview.ui.components.rememberAnimatedInt
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.WaterBlue
import com.x.myview.ui.theme.bottomInset
import com.x.myview.util.formatMl
import com.x.myview.util.niceDate
import com.x.myview.util.timeOfDay

@Composable
fun WaterScreen(vm: MainViewModel) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val logs by vm.waterLogs.collectAsState()
    var showCustom by remember { mutableStateOf(false) }

    val byDay = remember(logs) { waterByDay(logs) }
    val total = byDay[today] ?: 0
    val goal = settings.waterGoalMl
    val todayLogs = remember(logs, today) { logs.filter { it.day == today }.sortedByDescending { it.createdAt } }
    val week = remember(today) { (today - 6..today).toList() }
    val weekValues = week.map { byDay[it] ?: 0 }
    val activeDays = weekValues.count { it > 0 }
    val avg = if (activeDays == 0) 0 else weekValues.sum() / activeDays

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset())) {
        item { ScreenHeader("Water", niceDate(today)) }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
                        WaterWave(
                            progress = total.toFloat() / goal.coerceAtLeast(1),
                            color = WaterBlue,
                            modifier = Modifier.size(168.dp),
                        )
                        ProgressRing(total.toFloat() / goal.coerceAtLeast(1), size = 196.dp, stroke = 12.dp, color = WaterBlue) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatMl(rememberAnimatedInt(total)), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "of ${formatMl(goal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (total >= goal) "Goal reached. Well done!" else "${formatMl(goal - total)} to go",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { vm.addWater(settings.glassMl) }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ ${settings.glassMl} ml")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { vm.addWater(150) }, modifier = Modifier.weight(1f)) { Text("150") }
                        FilledTonalButton(onClick = { vm.addWater(500) }, modifier = Modifier.weight(1f)) { Text("500") }
                        FilledTonalButton(onClick = { showCustom = true }, modifier = Modifier.weight(1f)) { Text("Custom") }
                    }
                }
            }
        }

        item { SectionTitle("Last 7 days") }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    WeekBars(
                        days = week,
                        values = weekValues,
                        color = WaterBlue,
                        goal = goal,
                        valueLabel = { if (it >= 1000) String.format("%.1fL", it / 1000f) else "$it" },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (activeDays == 0) "No data yet" else "Average on active days: ${formatMl(avg)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionTitle("Today's drinks") }
        if (todayLogs.isEmpty()) {
            item { EmptyHint("Nothing logged yet. Have a glass of water.", null) { } }
        }
        itemsIndexed(todayLogs, key = { _, l -> l.id }) { i, l ->
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 3.dp).staggerIn(i)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatMl(l.ml),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        timeOfDay(l.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { vm.deleteWater(l.id) }) { Text("Remove") }
                }
            }
        }
    }

    if (showCustom) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("Add water") },
            text = { NumberField(text, { text = it }, "Amount in ml", Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ml = text.toIntOrNull() ?: 0
                        if (ml in 1..5000) vm.addWater(ml)
                        showCustom = false
                    },
                    enabled = (text.toIntOrNull() ?: 0) in 1..5000,
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } },
        )
    }
}
