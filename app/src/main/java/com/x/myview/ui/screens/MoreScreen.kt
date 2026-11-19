package com.x.myview.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.StepsTeal
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatSleep
import com.x.myview.util.formatSteps
import com.x.myview.util.formatWeight
import java.time.LocalDate

@Composable
fun MoreScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val steps by vm.steps.collectAsState()
    val sleep by vm.sleepLogs.collectAsState()
    val weights by vm.weightLogs.collectAsState()
    val moods by vm.moodLogs.collectAsState()

    val lastSleep = sleep.lastOrNull()
    val latestWeight = weights.lastOrNull()
    val moodToday = moods.firstOrNull { it.day == today }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset())) {
        item {
            ScreenHeader("More", "Body, mind and app settings")
        }
        item {
            MoreRow(
                Icons.Rounded.BarChart, MaterialTheme.colorScheme.primary, "Stats and history",
                "Day-by-day charts with highest and lowest", { onOpen("history:steps") }, Modifier.staggerIn(0),
            )
        }
        if (vm.stepsSupported) {
            item {
                MoreRow(iconFor("walk"), StepsTeal, "Steps", "${formatSteps(steps[today] ?: 0)} steps today", { onOpen("steps") }, Modifier.staggerIn(1))
            }
        }
        item {
            MoreRow(
                iconFor("sleep"), itemColor(0), "Sleep",
                if (lastSleep == null) "Log how long you slept" else "Last: ${formatSleep(lastSleep.minutes)}",
                { onOpen("sleep") }, Modifier.staggerIn(2),
            )
        }
        item {
            MoreRow(
                iconFor("heart"), itemColor(3), "Weight",
                if (latestWeight == null) "Track your weight trend" else "Latest: ${formatWeight(latestWeight.kg, settings.useLb)}",
                { onOpen("weight") }, Modifier.staggerIn(3),
            )
        }
        item {
            MoreRow(
                iconFor("spa"), itemColor(1), "Mood diary",
                if (moodToday == null) "How are you feeling?" else "Today: ${Moods.emoji(moodToday.mood)} ${Moods.label(moodToday.mood)}",
                { onOpen("mood") }, Modifier.staggerIn(4),
            )
        }
        item {
            MoreRow(iconFor("money"), itemColor(5), "Backup and restore", "Save your data to a file, or bring it back", { onOpen("backup") }, Modifier.staggerIn(5))
        }
        item {
            MoreRow(Icons.Rounded.Settings, itemColor(2), "Settings", "Theme, fonts, goals and reminders", { onOpen("settings") }, Modifier.staggerIn(6))
        }
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MvCard(modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BackupScreen(vm: MainViewModel, onBack: () -> Unit) {
    val message by vm.backupMessage.collectAsState()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Backup and restore", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Save a backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Creates one file with your habits, workouts, water, sleep, weight and mood. Keep it in Drive, send it to yourself, or copy it to a new phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { exportLauncher.launch("myview-backup-${LocalDate.now()}.json") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save backup file") }
                }
            }
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Restore from a backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Pick a MyView backup file. Everything currently in the app is replaced by the file's content.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Choose backup file") }
                }
            }
            if (message != null) {
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message ?: "", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { vm.clearBackupMessage() }) { Text("OK") }
                    }
                }
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "Settings and step history are not part of a backup. The file is plain text (JSON) and never leaves your phone unless you share it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val uri = pendingImport
    if (uri != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace all data?") },
            text = { Text("Your current habits, workouts and logs will be replaced by the backup. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    vm.importBackup(uri)
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
}
