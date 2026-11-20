package com.x.myview.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.domain.AwardsResult
import com.x.myview.domain.computeAwards
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.GradientCard
import com.x.myview.ui.components.MiniBar
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.NumberFont
import com.x.myview.ui.theme.itemColor

/** Level, experience points and badges worked out from everything logged so far. */
@Composable
fun rememberAwards(vm: MainViewModel): AwardsResult {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val habits by vm.habits.collectAsState()
    val habitDone by vm.habitDone.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val exerciseLogs by vm.exerciseLogs.collectAsState()
    val waterLogs by vm.waterLogs.collectAsState()
    val steps by vm.steps.collectAsState()
    val sleep by vm.sleepLogs.collectAsState()
    val moods by vm.moodLogs.collectAsState()
    val focus by vm.focusMinutes.collectAsState()
    val breath by vm.breathSessions.collectAsState()
    return remember(today, settings, habits, habitDone, exercises, exerciseLogs, waterLogs, steps, sleep, moods, focus, breath) {
        computeAwards(
            habits, habitDone, exercises, exerciseLogs, waterLogs, steps,
            sleep.size, moods.size, focus, breath, settings, today,
        )
    }
}

@Composable
fun AwardsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val awards = rememberAwards(vm)
    val unlocked = awards.badges.count { it.done }

    Column(Modifier.fillMaxSize()) {
        BackHeader("Level and awards", onBack)
        LazyColumn(
            Modifier
                .weight(1f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                GradientCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProgressRing(awards.levelProgress, size = 110.dp, stroke = 12.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${awards.level}", fontSize = 36.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${awards.xp} XP", style = MaterialTheme.typography.titleLarge, fontFamily = NumberFont, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${awards.xpLevelSize - awards.xpInLevel} XP to level ${awards.level + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$unlocked of ${awards.badges.size} badges unlocked",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Earn XP from ticking habits, reaching water and step goals, workouts, sleep and mood logs, focus minutes and breathing.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { SectionTitle("Badges") }
            itemsIndexed(awards.badges, key = { _, b -> b.title }) { i, b ->
                val color = itemColor(i)
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).staggerIn(i)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (b.done) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (b.done) iconFor(b.icon) else Icons.Rounded.Lock,
                                null,
                                tint = if (b.done) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(b.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!b.done) MiniBar(b.progress, color, Modifier.padding(top = 6.dp), height = 4.dp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (b.done) "Done" else "${b.current} / ${b.target}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (b.done) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
