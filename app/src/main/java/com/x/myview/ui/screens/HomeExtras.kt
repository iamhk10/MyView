package com.x.myview.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.myview.run.RunStatus
import com.x.myview.run.RunStore
import com.x.myview.run.RunTracker
import com.x.myview.run.distLabel
import com.x.myview.run.speedKmh
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.bounceClick
import com.x.myview.ui.components.staggerIn
import com.x.myview.util.formatDuration
import com.x.myview.util.formatMl
import com.x.myview.util.formatSteps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

// ---------------------------------------------------------------------------------------------
//  Small shared effects
// ---------------------------------------------------------------------------------------------

/** A soft band of light that sweeps across the item every few seconds. Put it before the background modifier. */
fun Modifier.shimmer(periodMs: Int = 5200): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    drawWithContent {
        drawContent()
        val w = size.width
        val cx = w * x
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                start = Offset(cx - w * 0.18f, 0f),
                end = Offset(cx + w * 0.18f, size.height),
            ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
//  Live run banner: shows on Today while a run or walk is going, tap to jump back to it
// ---------------------------------------------------------------------------------------------

@Composable
fun LiveRunBanner(modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val st by RunTracker.state.collectAsState()
    if (st.status != RunStatus.RUNNING && st.status != RunStatus.PAUSED) return
    val scheme = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "livePulse")
    val pulse by t.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary)))
            .bounceClick(onOpen)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .graphicsLayer { alpha = if (st.status == RunStatus.RUNNING) pulse else 1f }
                .background(Color.White, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (st.walk) "Walk" else "Run") + (if (st.status == RunStatus.PAUSED) " paused" else " in progress") + "  •  " + formatDuration(st.elapsedSec),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                distLabel(st.distanceM) + "  •  " + String.format(Locale.US, "%.1f km/h", speedKmh(st.speedMs)) + "  •  tap to open",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
    }
}

// ---------------------------------------------------------------------------------------------
//  Quick actions: a row you can swipe sideways
// ---------------------------------------------------------------------------------------------

class QuickAction(val icon: ImageVector, val label: String, val tint: Color, val container: Color, val onClick: () -> Unit)

@Composable
fun QuickActionsRow(actions: List<QuickAction>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(actions) { i, a ->
            Column(
                Modifier
                    .staggerIn(i)
                    .width(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(a.container, a.container.copy(alpha = 0.6f))))
                    .bounceClick(a.onClick)
                    .padding(vertical = 14.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(a.tint.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(a.icon, null, tint = a.tint, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(a.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Run and walk, this week
// ---------------------------------------------------------------------------------------------

@Composable
fun RunWeekMini(modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val st by RunTracker.state.collectAsState()
    // Reload when a run ends so the new run shows up at once.
    val history = remember(st.status) { RunStore.all(ctx) }
    val today = LocalDate.now()
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val perDay = remember(history, today) {
        days.map { d ->
            history.filter { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() == d }
                .sumOf { it.distanceM.toDouble() }.toFloat()
        }
    }
    val total = perDay.sum()
    val top = max(perDay.maxOrNull() ?: 0f, 1000f)
    val scheme = MaterialTheme.colorScheme
    val bar = scheme.primary
    val track = scheme.onSurface.copy(alpha = 0.08f)
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val progress by animateFloatAsState(if (grown) 1f else 0f, tween(900), label = "bars")

    MvCard(modifier, onClick = onOpen) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Run and walk", style = MaterialTheme.typography.labelLarge, color = scheme.primary, fontWeight = FontWeight.Medium)
                    Text(
                        if (total < 1000f) "${total.toInt()} m" else String.format(Locale.US, "%.1f km", total / 1000f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    if (history.isEmpty()) "Start your first run" else "last 7 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(54.dp)) {
                val n = perDay.size
                val gap = 8.dp.toPx()
                val bw = (size.width - gap * (n - 1)) / n
                perDay.forEachIndexed { i, v ->
                    val x = i * (bw + gap)
                    drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), CornerRadius(5.dp.toPx()))
                    if (v > 0f) {
                        val h = max(size.height * (v / top) * progress, 5.dp.toPx())
                        drawRoundRect(
                            bar.copy(alpha = if (i == n - 1) 1f else 0.6f),
                            Offset(x, size.height - h), Size(bw, h), CornerRadius(5.dp.toPx()),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { d ->
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Daily challenge: one goal per day, filled from what you already track
// ---------------------------------------------------------------------------------------------

private class Challenge(val title: String, val progress: Float, val detail: String)

@Composable
fun DailyChallengeCard(
    today: Long,
    stepsSupported: Boolean,
    steps: Int,
    waterMl: Int,
    waterGoal: Int,
    reps: Int,
    habitsDone: Int,
    habitsPlanned: Int,
    modifier: Modifier = Modifier,
) {
    val list = ArrayList<Challenge>()
    if (stepsSupported) list.add(Challenge("Walk 6,000 steps", steps / 6000f, "${formatSteps(steps)} of 6,000"))
    list.add(Challenge("Drink 2 litres of water", waterMl / 2000f, "${formatMl(waterMl)} of 2 L"))
    list.add(Challenge("Do 50 reps today", reps / 50f, "$reps of 50"))
    if (habitsPlanned > 0) list.add(Challenge("Finish every habit", habitsDone.toFloat() / habitsPlanned, "$habitsDone of $habitsPlanned"))
    if (stepsSupported) list.add(Challenge("Walk 10,000 steps", steps / 10000f, "${formatSteps(steps)} of 10,000"))
    list.add(Challenge("Reach your water goal", waterMl / max(waterGoal, 1).toFloat(), "${formatMl(waterMl)} of ${formatMl(waterGoal)}"))
    list.add(Challenge("Do 100 reps today", reps / 100f, "$reps of 100"))
    val c = list[Math.floorMod(today, list.size.toLong()).toInt()]
    val done = c.progress >= 1f
    val scheme = MaterialTheme.colorScheme
    val p by animateFloatAsState(c.progress.coerceIn(0f, 1f), tween(800), label = "challenge")

    MvCard(modifier) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(if (done) scheme.primary else scheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (done) Icons.Rounded.Check else com.x.myview.ui.components.iconFor("fire"),
                    null,
                    tint = if (done) scheme.onPrimary else scheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (done) "Challenge complete" else "Today’s challenge",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(scheme.onSurface.copy(alpha = 0.08f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(p)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(c.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
