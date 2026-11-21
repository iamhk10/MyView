package com.x.myview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x.myview.data.AppLook
import com.x.myview.ui.theme.LocalGlass
import com.x.myview.ui.theme.LocalIsDark
import com.x.myview.ui.theme.LocalLook

/** Floating surface: a calm card normally, translucent with a bright edge in liquid-glass mode. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    flat: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalGlass.current
    val dark = LocalIsDark.current
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerHigh
    val look = if (LocalLook.current == AppLook.HEALTH) {
        // Health: a plain tonal fill, no border and no shadow.
        Modifier.background(scheme.primaryContainer, shape)
    } else if (LocalLook.current == AppLook.BENTO) {
        // Bento: translucent slate with a brighter hairline and a soft drop shadow (the floating dock).
        val bentoBase = if (dark) Color(0xFF16191F).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.80f)
        Modifier
            .shadow(if (flat) 0.dp else 18.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .background(bentoBase, shape)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFF0F172A).copy(alpha = 0.10f), shape)
    } else if (glass) {
        Modifier
            .background(base.copy(alpha = if (dark) 0.55f else 0.60f), shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.16f else 0.55f),
                        Color.White.copy(alpha = 0.03f),
                    ),
                ),
                shape,
            )
            .border(
                0.8.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.55f else 0.95f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape,
            )
    } else if (flat) {
        Modifier
            .background(base.copy(alpha = 0.9f), shape)
            .border(0.5.dp, scheme.outlineVariant.copy(alpha = 0.5f), shape)
    } else {
        Modifier
            .shadow(8.dp, shape, clip = false)
            .background(base.copy(alpha = 0.97f), shape)
            .border(0.5.dp, scheme.outlineVariant.copy(alpha = 0.5f), shape)
    }
    Box(modifier.then(look).clip(shape)) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface) { content() }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = if (LocalLook.current == AppLook.BENTO || LocalLook.current == AppLook.HEALTH) 40.dp else 44.dp,
    tint: Color = if (LocalLook.current == AppLook.HEALTH) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
) {
    GlassSurface(modifier.size(size), CircleShape, flat = LocalLook.current == AppLook.BENTO) {
        Box(
            Modifier
                .size(size)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/**
 * Base colour with soft glows of the accent colour. The Midnight look adds a violet glow low on the
 * screen, a silver one top left and a fine dot grid. One draw pass, no blur.
 */
@Composable
fun Modifier.appBackground(): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.primary
    val second = MaterialTheme.colorScheme.tertiary
    val dark = LocalIsDark.current
    val midnight = LocalLook.current == AppLook.MIDNIGHT
    val strength = if (dark) (if (midnight) 0.22f else 0.30f) else 0.22f
    val strength2 = if (dark) (if (midnight) 0.26f else 0.16f) else 0.12f
    if (LocalLook.current == AppLook.BENTO || LocalLook.current == AppLook.HEALTH) return this.drawBehind { drawRect(bg) }
    return this.drawBehind {
        drawRect(bg)
        drawRect(
            Brush.radialGradient(
                colors = listOf(tint.copy(alpha = strength), Color.Transparent),
                center = Offset(size.width * (if (midnight) 0.15f else 0.5f), 0f),
                radius = size.width * 1.15f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(second.copy(alpha = strength2), Color.Transparent),
                center = Offset(size.width * (if (midnight) 0.6f else 1.0f), size.height * (if (midnight) 1.05f else 0.95f)),
                radius = size.width * 1.0f,
            ),
        )
        if (midnight && dark) {
            val step = 26.dp.toPx()
            val points = ArrayList<Offset>()
            var y = step / 2f
            while (y < size.height) {
                var x = step / 2f
                while (x < size.width) {
                    points.add(Offset(x, y))
                    x += step
                }
                y += step
            }
            drawPoints(points, PointMode.Points, Color.White.copy(alpha = 0.055f), 1.6.dp.toPx(), StrokeCap.Round)
        }
    }
}
