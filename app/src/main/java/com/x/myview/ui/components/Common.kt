package com.x.myview.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.util.oneLetterDay

/** Page title with an optional line below it and actions on the right. */
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
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
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Plain content card (not floating). */
@Composable
fun MvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
    val clickMod = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(modifier.then(clickMod)) { content() }
}

/** Circular progress ring with content in the middle. */
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
        tween(700, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val inset = s / 2f
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(s, cap = StrokeCap.Round))
            if (animated > 0.001f) {
                drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arc, style = Stroke(s, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Thin horizontal progress bar. */
@Composable
fun MiniBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "bar")
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
                .background(color),
        )
    }
}

/**
 * Seven-day bar chart. [days] are epoch days, [values] the amount per day.
 * The bar of the last day is drawn in full colour, the others a bit softer. A dashed line marks [goal].
 */
@Composable
fun WeekBars(
    days: List<Long>,
    values: List<Int>,
    color: Color,
    goal: Int,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 120.dp,
    valueLabel: (Int) -> String = { it.toString() },
) {
    val maxV = maxOf(goal, values.maxOrNull() ?: 0, 1)
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { i, _ ->
                val v = values.getOrElse(i) { 0 }
                val frac by animateFloatAsState(v.toFloat() / maxV, tween(600), label = "bar$i")
                val last = i == days.lastIndex
                Column(
                    Modifier
                        .weight(1f)
                        .height(chartHeight),
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (v > 0) color.copy(alpha = if (last) 1f else 0.55f) else onSurface.copy(alpha = 0.10f)),
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

/** A small round tick button used for habits. */
@Composable
fun CheckCircle(
    checked: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(220), label = "checkFill")
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f + 0.86f * fill))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (fill > 0.05f) {
            androidx.compose.material3.Icon(
                iconFor("check"),
                contentDescription = "Done",
                tint = onColorForSimple(color),
                modifier = Modifier
                    .size(size * 0.55f),
            )
        }
    }
}

private fun onColorForSimple(c: Color): Color = com.x.myview.ui.theme.onColorFor(c)

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
