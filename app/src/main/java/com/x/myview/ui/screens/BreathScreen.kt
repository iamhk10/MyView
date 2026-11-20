package com.x.myview.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.rememberHaptic
import com.x.myview.ui.theme.ScriptFont
import com.x.myview.ui.theme.StepsTeal
import kotlinx.coroutines.delay

private class BreathPattern(val name: String, val note: String, val inhale: Int, val hold1: Int, val exhale: Int, val hold2: Int)

private val PATTERNS = listOf(
    BreathPattern("Box", "4 in, 4 hold, 4 out, 4 hold", 4, 4, 4, 4),
    BreathPattern("Calm", "5 in, 5 out", 5, 0, 5, 0),
    BreathPattern("Relax", "4 in, 7 hold, 8 out", 4, 7, 8, 0),
)

/** Guided breathing: a circle grows while you breathe in and shrinks while you breathe out. */
@Composable
fun BreathScreen(vm: MainViewModel, onBack: () -> Unit) {
    val today by vm.today.collectAsState()
    val sessions by vm.breathSessions.collectAsState()
    val haptic = rememberHaptic()
    val strong = rememberHaptic(strong = true)

    var pattern by remember { mutableIntStateOf(0) }
    var rounds by remember { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Ready") }
    var round by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.55f) }

    LaunchedEffect(running) {
        if (!running) {
            scale.animateTo(0.55f, tween(500))
            phase = if (done) "Well done" else "Ready"
            return@LaunchedEffect
        }
        val p = PATTERNS[pattern]
        for (r in 1..rounds) {
            round = r
            phase = "Breathe in"
            haptic()
            scale.animateTo(1f, tween(p.inhale * 1000, easing = LinearEasing))
            if (p.hold1 > 0) {
                phase = "Hold"
                delay(p.hold1 * 1000L)
            }
            phase = "Breathe out"
            haptic()
            scale.animateTo(0.55f, tween(p.exhale * 1000, easing = LinearEasing))
            if (p.hold2 > 0) {
                phase = "Hold"
                delay(p.hold2 * 1000L)
            }
        }
        vm.addBreathSession()
        strong()
        done = true
        running = false
    }

    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize()) {
        BackHeader("Breathe", onBack)
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
                        .padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(260.dp)) {
                            val maxR = size.minDimension / 2f
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val r = maxR * scale.value
                            drawCircle(primary.copy(alpha = 0.10f), r * 1.12f, c)
                            drawCircle(primary.copy(alpha = 0.16f), r * 0.98f, c)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(StepsTeal.copy(alpha = 0.85f), primary.copy(alpha = 0.75f)),
                                    center = c,
                                    radius = r * 0.85f,
                                ),
                                r * 0.82f, c,
                            )
                            drawCircle(primary.copy(alpha = 0.9f), r * 0.82f, c, style = Stroke(2.dp.toPx()))
                        }
                        Text(
                            phase,
                            fontFamily = ScriptFont,
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (running) "Round $round of $rounds" else "${PATTERNS[pattern].name}: ${PATTERNS[pattern].note}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    if (!running) {
                        ChipFlow {
                            PATTERNS.forEachIndexed { i, p -> ChoiceChip(p.name, pattern == i, { pattern = i }) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Rounds", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            listOf(3, 4, 6, 8).forEach { n -> ChoiceChip("$n", rounds == n, { rounds = n }) }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Button(
                        onClick = {
                            if (running) {
                                done = false
                                running = false
                            } else {
                                done = false
                                running = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (running) "Stop" else if (done) "Breathe again" else "Start") }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill("${sessions[today] ?: 0}", "Sessions today", Modifier.weight(1f))
                StatPill("${sessions.values.sum()}", "All time", Modifier.weight(1f))
            }
            Text(
                "Breathe through your nose, slow and easy. You feel a light tap at each change. A finished session earns XP.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
