package com.x.myview.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.ui.components.bounceClick
import com.x.myview.ui.components.rememberHaptic

/** The five moods: 1 (awful) to 5 (great). */
object Moods {
    val emojis = listOf("😞", "😕", "😐", "🙂", "😄")
    val labels = listOf("Awful", "Bad", "Okay", "Good", "Great")
    val colors = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFA14A), Color(0xFFFFC24A), Color(0xFF7BDC8A), Color(0xFF3DD6C6),
    )

    fun emoji(mood: Int) = emojis[(mood - 1).coerceIn(0, 4)]
    fun label(mood: Int) = labels[(mood - 1).coerceIn(0, 4)]
    fun color(mood: Int) = colors[(mood - 1).coerceIn(0, 4)]
}

/** Row of five faces. [selected] is 1..5, or 0 for none. The chosen one grows with a spring. */
@Composable
fun MoodPicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (m in 1..5) {
            val on = m == selected
            val scale by animateFloatAsState(
                if (on) 1.18f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "mood$m",
            )
            val color = Moods.color(m)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (on) 0.28f else 0.10f))
                        .then(if (on) Modifier.border(2.dp, color, CircleShape) else Modifier)
                        .bounceClick {
                            haptic()
                            onSelect(m)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(Moods.emojis[m - 1], fontSize = 28.sp)
                }
                Text(
                    Moods.labels[m - 1],
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
