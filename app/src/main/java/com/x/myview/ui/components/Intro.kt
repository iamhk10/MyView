package com.x.myview.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The opening animation plays once per app start, not again when the screen is only recreated. */
object IntroGate {
    @Volatile
    var played: Boolean = false
}

/**
 * Opening animation: the logo pops in, a ring draws around it, the name fades in, then the whole
 * screen lifts away to show the app. About 2 seconds. Touches are blocked while it plays.
 */
@Composable
fun LaunchIntro(onDone: () -> Unit) {
    val pop = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 170f)) }
            launch { ring.animateTo(1f, tween(1050, easing = FastOutSlowInEasing)) }
            launch {
                delay(380)
                title.animateTo(1f, tween(650, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
            }
        }
        delay(280)
        exit.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        onDone()
    }

    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val accent2 = scheme.tertiary
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                val s = 1f + 0.08f * exit.value
                scaleX = s
                scaleY = s
            }
            .background(scheme.background)
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.30f * pop.value), Color.Transparent),
                    center = Offset.Unspecified,
                    radius = 900f,
                ),
            )
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2f + 2.dp.toPx()
                    val sz = Size(size.width - 2 * inset, size.height - 2 * inset)
                    drawArc(
                        brush = Brush.sweepGradient(listOf(accent, accent2, accent)),
                        startAngle = -90f,
                        sweepAngle = 360f * ring.value,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = sz,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Image(
                    painterResource(R.drawable.logo_full),
                    contentDescription = null,
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            val s = 0.55f + 0.45f * pop.value
                            scaleX = s
                            scaleY = s
                            alpha = pop.value.coerceIn(0f, 1f)
                            rotationZ = (1f - pop.value) * -14f
                        }
                        .clip(RoundedCornerShape(32.dp)),
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "MyView",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 24.dp.toPx()
                },
            )
            Text(
                "Move. Drink. Grow.",
                fontSize = 14.sp,
                letterSpacing = 3.sp,
                color = scheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 34.dp.toPx()
                },
            )
        }
    }
}
