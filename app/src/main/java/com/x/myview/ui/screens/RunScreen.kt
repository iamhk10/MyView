package com.x.myview.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.x.myview.run.GeminiClient
import com.x.myview.run.RunGoal
import com.x.myview.run.RunPoint
import com.x.myview.run.RunRecord
import com.x.myview.run.RunService
import com.x.myview.run.RunState
import com.x.myview.run.RunStatus
import com.x.myview.run.RunStore
import com.x.myview.run.RunTracker
import com.x.myview.run.buildCoachPrompt
import com.x.myview.run.kmText
import com.x.myview.run.offlineInsight
import com.x.myview.run.paceText
import com.x.myview.run.paceTextSec
import com.x.myview.run.speedKmh
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.SectionTitle
import com.x.myview.util.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

@Composable
fun RunScreen(vm: MainViewModel, onBack: () -> Unit) {
    val st by RunTracker.state.collectAsState()
    val weights by vm.weightLogs.collectAsState()
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val view = LocalView.current
    val kg = weights.maxByOrNull { it.day }?.kg ?: 65f
    val active = st.status == RunStatus.RUNNING || st.status == RunStatus.PAUSED
    var confirmStop by remember { mutableStateOf(false) }

    DisposableEffect(active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }

    Column(Modifier.fillMaxSize()) {
        BackHeader(
            when (st.status) {
                RunStatus.IDLE -> "Run and walk"
                RunStatus.DONE -> "Run complete"
                else -> if (st.walk) "Walking" else "Running"
            },
            onBack,
        )
        when (st.status) {
            RunStatus.IDLE -> RunSetup(kg, settings.geminiKey)
            RunStatus.DONE -> {
                val rec = st.record
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                ) {
                    if (rec != null) {
                        val before = remember(rec.startedAt) { RunStore.all(ctx).filter { it.startedAt != rec.startedAt } }
                        RunResultBody(rec, st.points, settings.geminiKey, before)
                    }
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Button(onClick = { RunTracker.reset() }, modifier = Modifier.fillMaxWidth()) { Text("New run") }
                    }
                }
            }
            else -> RunLive(
                st,
                onToggle = { if (st.status == RunStatus.RUNNING) RunTracker.pause() else RunTracker.resume() },
                onFinish = { confirmStop = true },
            )
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Finish this run?") },
            text = { Text("Your run is saved to the history.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    RunTracker.finish(ctx)
                    RunService.stop(ctx)
                }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Keep going") } },
        )
    }
}

// ---------------------------------------------------------------------------------------------
//  Setup
// ---------------------------------------------------------------------------------------------

@Composable
private fun RunSetup(kg: Float, geminiKey: String) {
    val ctx = LocalContext.current
    var walk by rememberSaveable { mutableStateOf(false) }
    var goalIdx by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(20) }
    var distM by rememberSaveable { mutableIntStateOf(5000) }
    var refresh by remember { mutableIntStateOf(0) }
    var viewing by remember { mutableStateOf<RunRecord?>(null) }
    val goal = RunGoal.values()[goalIdx]
    val history = remember(refresh) { RunStore.all(ctx) }

    val begin: () -> Unit = {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsOn = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        if (!gpsOn) {
            Toast.makeText(ctx, "Turn on Location (GPS) first", Toast.LENGTH_LONG).show()
            try {
                ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        } else {
            val value = when (goal) {
                RunGoal.TIME -> minutes * 60
                RunGoal.DISTANCE -> distM
                RunGoal.FREE -> 0
            }
            RunTracker.configure(goal, value, walk, kg)
            RunTracker.begin()
            RunService.start(ctx)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            begin()
        } else {
            Toast.makeText(ctx, "Precise location permission is needed to track a run", Toast.LENGTH_LONG).show()
        }
    }

    val shown = viewing
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        if (shown != null) {
            RunResultBody(shown, emptyList(), geminiKey, history.filter { it.startedAt != shown.startedAt })
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        RunStore.delete(ctx, shown.startedAt)
                        refresh++
                        viewing = null
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Delete") }
                Button(onClick = { viewing = null }, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            return@Column
        }

        SectionTitle("Activity")
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            ChipFlow {
                ChoiceChip("Run", !walk, { walk = false })
                ChoiceChip("Walk", walk, { walk = true })
            }
        }

        SectionTitle("Target")
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            ChipFlow {
                ChoiceChip("Free run", goal == RunGoal.FREE, { goalIdx = RunGoal.FREE.ordinal })
                ChoiceChip("Timer", goal == RunGoal.TIME, { goalIdx = RunGoal.TIME.ordinal })
                ChoiceChip("Distance", goal == RunGoal.DISTANCE, { goalIdx = RunGoal.DISTANCE.ordinal })
            }
        }
        when (goal) {
            RunGoal.TIME -> {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ChipFlow {
                        listOf(10, 15, 20, 30, 45, 60).forEach { m -> ChoiceChip("$m min", minutes == m, { minutes = m }) }
                    }
                }
                Stepper("$minutes min", { minutes = (minutes - 5).coerceAtLeast(5) }, { minutes = (minutes + 5).coerceAtMost(180) })
            }
            RunGoal.DISTANCE -> {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ChipFlow {
                        listOf(1, 2, 3, 5, 10).forEach { k -> ChoiceChip("$k km", distM == k * 1000, { distM = k * 1000 }) }
                    }
                }
                Stepper(
                    String.format(Locale.US, "%.1f km", distM / 1000f),
                    { distM = (distM - 500).coerceAtLeast(500) },
                    { distM = (distM + 500).coerceAtMost(42000) },
                )
            }
            RunGoal.FREE -> Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    "No target. Run as long as you like and press Finish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Button(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine) {
                        begin()
                    } else {
                        val perms = ArrayList<String>()
                        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        launcher.launch(perms.toTypedArray())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (walk) "Start walk" else "Start run") }
        }
        Box(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Uses GPS, so start outdoors. Tracking keeps going with the screen off.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (history.isNotEmpty()) {
            SectionTitle("Recent")
            history.take(8).forEach { r ->
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { viewing = r }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                String.format(Locale.US, "%s km  •  %s", kmText(r.distanceM), formatDuration(r.durationSec)),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(r.startedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            String.format(Locale.US, "%.1f km/h", speedKmh(r.avgSpeedMs)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(onClick = onMinus) { Text("-") }
        Text(
            label,
            Modifier.width(120.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedButton(onClick = onPlus) { Text("+") }
    }
}

// ---------------------------------------------------------------------------------------------
//  Live
// ---------------------------------------------------------------------------------------------

@Composable
private fun RunTile(label: String, value: String, unit: String, modifier: Modifier = Modifier, big: Boolean = false) {
    MvCard(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = if (big) 44.sp else 24.sp, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RunLive(st: RunState, onToggle: () -> Unit, onFinish: () -> Unit) {
    val avgMs = if (st.elapsedSec > 0) st.distanceM / st.elapsedSec else 0f
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val gpsText = when {
            st.status == RunStatus.PAUSED -> "Paused"
            !st.gpsFix -> "Searching for GPS... go outside"
            else -> "GPS good (about ${st.accuracyM.toInt()} m)"
        }
        Text(
            gpsText,
            style = MaterialTheme.typography.labelLarge,
            color = if (st.gpsFix || st.status == RunStatus.PAUSED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(formatDuration(st.elapsedSec), fontSize = 72.sp, fontWeight = FontWeight.Bold)

        if (st.goal != RunGoal.FREE && st.goalValue > 0) {
            val p = if (st.goal == RunGoal.TIME) st.elapsedSec.toFloat() / st.goalValue else st.distanceM / st.goalValue
            LinearProgressIndicator(progress = { p.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                if (st.goal == RunGoal.TIME) "Target ${formatDuration(st.goalValue)}"
                else String.format(Locale.US, "Target %.1f km", st.goalValue / 1000f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RunTile("DISTANCE", kmText(st.distanceM), "km", Modifier.weight(1f), big = true)
            RunTile("SPEED", String.format(Locale.US, "%.1f", speedKmh(st.speedMs)), "km/h", Modifier.weight(1f), big = true)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RunTile("PACE", paceText(st.speedMs), "min/km", Modifier.weight(1f))
            RunTile("AVG", String.format(Locale.US, "%.1f", speedKmh(avgMs)), "km/h", Modifier.weight(1f))
            RunTile("MAX", String.format(Locale.US, "%.1f", speedKmh(st.maxSpeedMs)), "km/h", Modifier.weight(1f))
        }
        if (st.points.size >= 2) {
            Spacer(Modifier.height(10.dp))
            MvCard(Modifier.fillMaxWidth()) {
                RunRoute(st.points, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(170.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onToggle, modifier = Modifier.weight(1f)) {
                Text(if (st.status == RunStatus.RUNNING) "Pause" else "Resume")
            }
            OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Finish") }
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Result (after a run, or when opening one from the history)
// ---------------------------------------------------------------------------------------------

@Composable
private fun RunResultBody(rec: RunRecord, points: List<RunPoint>, key: String, before: List<RunRecord>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var aiText by remember(rec.startedAt) { mutableStateOf<String?>(null) }
    var aiErr by remember(rec.startedAt) { mutableStateOf<String?>(null) }
    var loading by remember(rec.startedAt) { mutableStateOf(false) }
    val review = remember(rec.startedAt) { offlineInsight(rec, before) }

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(rec.startedAt)) +
                (if (rec.walk) "  •  Walk" else "  •  Run"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RunTile("DISTANCE", kmText(rec.distanceM), "km", Modifier.weight(1f), big = true)
            RunTile("TIME", formatDuration(rec.durationSec), "", Modifier.weight(1f), big = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RunTile("AVG SPEED", String.format(Locale.US, "%.1f", speedKmh(rec.avgSpeedMs)), "km/h", Modifier.weight(1f))
            RunTile("PACE", paceText(rec.avgSpeedMs), "min/km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RunTile("MAX SPEED", String.format(Locale.US, "%.1f", speedKmh(rec.maxSpeedMs)), "km/h", Modifier.weight(1f))
            RunTile("CALORIES", rec.calories.toString(), "kcal", Modifier.weight(1f))
        }
        if (points.size >= 2) {
            MvCard(Modifier.fillMaxWidth()) {
                RunRoute(points, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(190.dp))
            }
        }
        if (rec.splits.isNotEmpty()) {
            MvCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Kilometre splits", fontWeight = FontWeight.SemiBold)
                    val best = rec.splits.minOrNull() ?: 0
                    rec.splits.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("Km ${i + 1}", Modifier.weight(1f))
                            Text(
                                paceTextSec(s) + " /km",
                                fontWeight = if (s == best) FontWeight.Bold else FontWeight.Normal,
                                color = if (s == best) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
        MvCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick review", fontWeight = FontWeight.SemiBold)
                Text(review, style = MaterialTheme.typography.bodyMedium)
            }
        }
        MvCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI coach (online)", fontWeight = FontWeight.SemiBold)
                val text = aiText
                if (text != null) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                } else if (key.isBlank()) {
                    Text(
                        "Add your Gemini key in Settings > AI coach to get a personal review in Hinglish.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(
                        onClick = {
                            loading = true
                            aiErr = null
                            scope.launch {
                                val r = GeminiClient.ask(key, buildCoachPrompt(rec, before))
                                loading = false
                                r.onSuccess { aiText = it }
                                r.onFailure { aiErr = it.message ?: "Something went wrong." }
                            }
                        },
                        enabled = !loading,
                    ) { Text(if (loading) "Asking the coach..." else "Ask AI coach") }
                    aiErr?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedButton(
                    onClick = {
                        try {
                            val q = if (rec.walk) "how to improve walking pace and stamina" else "how to improve running pace and stamina for beginners"
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
                        } catch (_: Exception) {
                            Toast.makeText(ctx, "No browser found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Search tips on Google") }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Route drawing
// ---------------------------------------------------------------------------------------------

@Composable
private fun RunRoute(points: List<RunPoint>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val lat0 = points.sumOf { it.lat } / points.size
        val k = cos(Math.toRadians(lat0))
        val xs = points.map { it.lon * k }
        val ys = points.map { it.lat }
        val minX = xs.minOrNull() ?: 0.0
        val maxX = xs.maxOrNull() ?: 0.0
        val minY = ys.minOrNull() ?: 0.0
        val maxY = ys.maxOrNull() ?: 0.0
        val spanX = max(maxX - minX, 1e-6)
        val spanY = max(maxY - minY, 1e-6)
        val pad = 18.dp.toPx()
        val sc = min((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY).toFloat()
        val ox = (size.width - (spanX * sc).toFloat()) / 2f
        val oy = (size.height - (spanY * sc).toFloat()) / 2f
        fun pt(i: Int) = Offset(ox + ((xs[i] - minX) * sc).toFloat(), size.height - oy - ((ys[i] - minY) * sc).toFloat())
        val path = Path()
        path.moveTo(pt(0).x, pt(0).y)
        for (i in 1 until points.size) path.lineTo(pt(i).x, pt(i).y)
        drawPath(path, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color.copy(alpha = 0.45f), radius = 8.dp.toPx(), center = pt(0))
        drawCircle(color, radius = 6.dp.toPx(), center = pt(points.size - 1))
    }
}
