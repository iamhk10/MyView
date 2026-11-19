package com.x.myview.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.ui.theme.LocalIsDark
import com.x.myview.ui.theme.onColorFor
import com.x.myview.util.oneLetterDay

/** Big page title with an optional line below it and actions on the right. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

/** Soft card with a light edge. Squeezes a little when pressed if [onClick] is given. */
@Composable
fun MvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val dark = LocalIsDark.current
    val scheme = MaterialTheme.colorScheme
    val press = if (onClick != null) Modifier.bounceClick(onClick) else Modifier
    Box(
        modifier
            .then(press)
            .fillMaxWidth()
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.82f), shape)
            .background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.07f else 0.45f), Color.Transparent)),
                shape,
            )
            .border(
                0.8.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.22f else 0.9f), Color.White.copy(alpha = if (dark) 0.04f else 0.15f)),
                ),
                shape,
            )
            .clip(shape),
    ) { content() }
}

/** Highlight card tinted with the accent colour, used for the main summary on the Today screen. */
@Composable
fun GradientCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val scheme = MaterialTheme.colorScheme
    val dark = LocalIsDark.current
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = if (dark) 0.42f else 0.55f),
                        scheme.tertiary.copy(alpha = if (dark) 0.20f else 0.30f),
                        scheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    ),
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.40f else 0.95f), Color.White.copy(alpha = 0.05f))),
                shape,
            )
            .clip(shape),
    ) { content() }
}

/** Circular progress ring with a glowing, gradient arc and content in the middle. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        tween(900, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val inset = s / 2f
            val arc = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
            if (animated > 0.001f) {
                val sweep = 360f * animated
                val brush = Brush.sweepGradient(
                    0f to color.copy(alpha = 0.45f),
                    animated.coerceAtLeast(0.05f) to color,
                    1f to color,
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                )
                rotate(-90f) {
                    drawArc(color.copy(alpha = 0.16f), 0f, sweep, false, topLeft, arc, style = Stroke(s * 1.7f, cap = StrokeCap.Round))
                    drawArc(brush, 0f, sweep, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** Thin horizontal progress bar. */
@Composable
fun MiniBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color))),
        )
    }
}

/**
 * Seven-day bar chart. [days] are epoch days, [values] the amount per day. The last day is drawn in full
 * colour, the others softer. A dashed line marks [goal].
 */
@Composable
fun WeekBars(
    days: List<Long>,
    values: List<Int>,
    color: Color,
    goal: Int,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 130.dp,
    valueLabel: (Int) -> String = { it.toString() },
) {
    val maxV = maxOf(goal, values.maxOrNull() ?: 0, 1)
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEachIndexed { i, _ ->
                    val v = values.getOrElse(i) { 0 }
                    val frac by animateFloatAsState(
                        v.toFloat() / maxV,
                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                        label = "bar$i",
                    )
                    val last = i == days.lastIndex
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (v > 0) {
                            Text(
                                valueLabel(v),
                                fontSize = 9.sp,
                                maxLines = 1,
                                color = onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(((chartHeight - 18.dp) * frac).coerceAtLeast(if (v > 0) 4.dp else 2.dp))
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (v > 0) Brush.verticalGradient(listOf(color.copy(alpha = if (last) 1f else 0.7f), color.copy(alpha = if (last) 0.7f else 0.4f)))
                                    else Brush.verticalGradient(listOf(onSurface.copy(alpha = 0.10f), onSurface.copy(alpha = 0.10f))),
                                ),
                        )
                    }
                }
            }
            if (goal > 0) {
                Canvas(Modifier.matchParentSize()) {
                    val y = size.height - (size.height - 18.dp.toPx()) * goal / maxV
                    drawLine(
                        color.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, d ->
                Text(
                    oneLetterDay(d),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == days.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Round tick button used for habits. Pops with a spring and a small vibration when ticked. */
@Composable
fun CheckCircle(
    checked: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(240), label = "checkFill")
    val pop by animateFloatAsState(
        if (checked) 1f else 0.4f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkPop",
    )
    val haptic = rememberHaptic()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f + 0.86f * fill))
            .bounceClick {
                haptic()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (fill > 0.05f) {
            Icon(
                iconFor("check"),
                contentDescription = "Done",
                tint = onColorFor(color),
                modifier = Modifier
                    .size(size * 0.55f)
                    .graphicsLayer {
                        scaleX = pop
                        scaleY = pop
                    },
            )
        }
    }
}

/** Compact dashboard tile: icon, big value, label and an optional progress bar. */
@Composable
fun StatTile(
    icon: ImageVector,
    color: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    MvCard(modifier, onClick) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                MiniBar(progress, color, height = 5.dp)
            }
        }
    }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
