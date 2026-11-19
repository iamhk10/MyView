package com.x.myview.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.x.myview.ui.theme.LocalHapticsOn
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Click with a small springy squeeze while pressed. */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Fades and slides an item up shortly after it first appears. Later items start a little later. */
fun Modifier.staggerIn(index: Int): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, 8) * 55L)
        shown = true
    }
    val progress by animateFloatAsState(if (shown) 1f else 0f, tween(420), label = "stagger")
    this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 26.dp.toPx()
    }
}

/** Counts up (or down) smoothly to [target]. */
@Composable
fun rememberAnimatedInt(target: Int, millis: Int = 800): Int {
    val value by animateIntAsState(target, tween(millis), label = "count")
    return value
}

/** Returns a function that gives a short vibration, if the haptics setting is on. */
@Composable
fun rememberHaptic(strong: Boolean = false): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val on = LocalHapticsOn.current
    return {
        if (on) haptic.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
    }
}

/** Water that gently waves inside a circle. [progress] is how full it is, 0..1. */
@Composable
fun WaterWave(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        0f,
        (2 * PI).toFloat(),
        infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val level by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000), label = "level")
    Canvas(modifier.clip(CircleShape)) {
        if (level < 0.005f) return@Canvas
        val w = size.width
        val h = size.height
        val top = h * (1f - level)
        fun layer(offset: Float, amp: Float, alpha: Float) {
            val path = Path()
            path.moveTo(0f, h)
            path.lineTo(0f, top)
            var x = 0f
            while (x <= w) {
                path.lineTo(x, top + sin(x / w * 2f * PI.toFloat() * 1.3f + phase + offset) * amp)
                x += 6f
            }
            path.lineTo(w, h)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.65f)), startY = top, endY = h))
        }
        layer(0f, h * 0.035f, 0.40f)
        layer(2.1f, h * 0.028f, 0.62f)
    }
}

private val CONFETTI = listOf(
    Color(0xFFFF6FB5), Color(0xFFFFC24A), Color(0xFF3DD6C6), Color(0xFFB69CFF), Color(0xFF7BDC8A), Color(0xFF6EA8FF),
)

private class Piece(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float)

/** A short burst of confetti from the upper middle of the screen whenever [trigger] changes (and is above 0). */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    val t = remember { Animatable(1f) }
    val pieces = remember(trigger) {
        val r = Random(trigger)
        List(48) {
            Piece(
                angle = r.nextFloat() * 2f * PI.toFloat(),
                speed = 0.25f + r.nextFloat() * 0.75f,
                color = CONFETTI[r.nextInt(CONFETTI.size)],
                size = 7f + r.nextFloat() * 8f,
                spin = r.nextFloat() * 6f - 3f,
            )
        }
    }
    val firstTrigger = remember { trigger }
    LaunchedEffect(trigger) {
        // Skip the value that was already there when this screen was created (e.g. after a rotation).
        if (trigger > 0 && trigger != firstTrigger) {
            t.snapTo(0f)
            t.animateTo(1f, tween(1700, easing = LinearOutSlowInEasing))
        }
    }
    if (t.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val p = t.value
        val center = Offset(size.width / 2f, size.height * 0.30f)
        val reach = size.minDimension * 0.75f
        pieces.forEach { piece ->
            val dist = piece.speed * reach * p
            val x = center.x + cos(piece.angle) * dist
            val y = center.y + sin(piece.angle) * dist + p * p * size.height * 0.28f
            val alpha = (1f - p * p).coerceIn(0f, 1f)
            rotate(degrees = piece.spin * p * 360f, pivot = Offset(x, y)) {
                drawRect(
                    piece.color.copy(alpha = alpha),
                    topLeft = Offset(x - piece.size / 2f, y - piece.size / 2f),
                    size = Size(piece.size, piece.size * 0.55f),
                )
            }
        }
    }
}

/**
 * Simple line chart over days. [points] are (epoch day, value) sorted by day. A soft fill sits under the line
 * and an optional dashed [goal] line is drawn when it is inside the value range.
 */
@Composable
fun LineChart(
    points: List<Pair<Long, Float>>,
    color: Color,
    modifier: Modifier = Modifier,
    goal: Float? = null,
) {
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val reveal by animateFloatAsState(if (started) 1f else 0f, tween(900), label = "reveal")
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val pad = 10.dp.toPx()
        val minX = points.first().first.toFloat()
        val maxX = points.last().first.toFloat().coerceAtLeast(minX + 1f)
        var minY = points.minOf { it.second }
        var maxY = points.maxOf { it.second }
        if (goal != null) {
            minY = minOf(minY, goal)
            maxY = maxOf(maxY, goal)
        }
        val span = (maxY - minY).coerceAtLeast(0.5f)
        minY -= span * 0.15f
        maxY += span * 0.15f
        fun px(d: Long) = pad + (d - minX) / (maxX - minX) * (size.width - 2 * pad)
        fun py(v: Float) = pad + (1f - (v - minY) / (maxY - minY)) * (size.height - 2 * pad)

        for (i in 0..3) {
            val y = pad + i * (size.height - 2 * pad) / 3f
            drawLine(grid, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        }
        if (goal != null) {
            val gy = py(goal)
            drawLine(
                color.copy(alpha = 0.6f), Offset(pad, gy), Offset(size.width - pad, gy),
                strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
            )
        }
        val line = Path()
        val fill = Path()
        points.forEachIndexed { i, (d, v) ->
            val x = px(d)
            val y = py(v)
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height - pad)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(px(points.last().first), size.height - pad)
        fill.close()
        clipRect(right = size.width * reveal) {
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0f))))
            drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        val last = points.last()
        drawCircle(color.copy(alpha = 0.25f * reveal), 9.dp.toPx(), Offset(px(last.first), py(last.second)))
        drawCircle(color.copy(alpha = reveal), 5.dp.toPx(), Offset(px(last.first), py(last.second)))
    }
}
