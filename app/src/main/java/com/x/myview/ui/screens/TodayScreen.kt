package com.x.myview.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import com.x.myview.ui.components.liquidGlass
import com.x.myview.ui.theme.LocalGlass
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import com.x.myview.data.db.KIND_REPS
import com.x.myview.ui.components.TonalCard
import com.x.myview.ui.theme.healthHeroColor
import com.x.myview.ui.theme.healthTone
import com.x.myview.ui.theme.onColorFor
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.ui.graphics.Color
import com.x.myview.data.AppLook
import com.x.myview.data.ThemeMode
import com.x.myview.ui.components.CheckCircle
import com.x.myview.ui.components.bounceClick
import com.x.myview.ui.components.hairline
import com.x.myview.ui.theme.LocalIsDark
import com.x.myview.ui.theme.LocalLook
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.data.db.KIND_TIME
import com.x.myview.domain.currentStreak
import com.x.myview.domain.exerciseTotal
import com.x.myview.domain.quoteFor
import com.x.myview.domain.scheduledHabits
import com.x.myview.domain.waterTotal
import com.x.myview.domain.workoutProgress
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.CircleIconButton
import com.x.myview.ui.components.GradientCard
import com.x.myview.ui.components.MiniBar
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.ProgressRing
import com.x.myview.ui.components.ScreenHeader
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.StatTile
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.components.rememberAnimatedInt
import com.x.myview.ui.components.staggerIn
import com.x.myview.ui.theme.NumberFont
import com.x.myview.ui.theme.statFont
import com.x.myview.ui.theme.QuoteFont
import com.x.myview.ui.theme.ScriptFont
import com.x.myview.ui.theme.StepsTeal
import com.x.myview.ui.theme.WaterBlue
import com.x.myview.ui.theme.bottomInset
import com.x.myview.ui.theme.itemColor
import com.x.myview.util.formatDuration
import com.x.myview.util.formatMl
import com.x.myview.util.formatSleep
import com.x.myview.util.formatSteps
import com.x.myview.util.niceDate
import java.time.LocalTime

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Hey, night owl"
}

private fun scoreMessage(score: Float, goals: Int): String = when {
    goals == 0 -> "Add a habit or log some water to begin."
    score >= 1f -> "Every goal done. Brilliant!"
    score >= 0.66f -> "Almost there, keep going."
    score >= 0.33f -> "Good progress today."
    score > 0f -> "A good start. One step at a time."
    else -> "A fresh day. Let's get started."
}

@Composable
fun TodayScreen(vm: MainViewModel, onOpen: (String) -> Unit, onGoTab: (Int) -> Unit) {
    val today by vm.today.collectAsState()
    val settings by vm.settings.collectAsState()
    val habits by vm.habits.collectAsState()
    val done by vm.habitDone.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val exLogs by vm.exerciseLogs.collectAsState()
    val water by vm.waterLogs.collectAsState()
    val steps by vm.steps.collectAsState()
    val sleepLogs by vm.sleepLogs.collectAsState()
    val moods by vm.moodLogs.collectAsState()

    val stepsToday = steps[today] ?: 0
    var stepsGranted by remember { mutableStateOf(vm.stepsPermissionGranted()) }
    val askSteps = rememberPermissionRequest(android.Manifest.permission.ACTIVITY_RECOGNITION) { ok ->
        stepsGranted = ok
        if (ok) vm.startSteps()
    }

    val planned = remember(habits, today) { scheduledHabits(habits, today) }
    val doneCount = planned.count { today in (done[it.id] ?: emptySet()) }
    val habitProgress = if (planned.isEmpty()) 0f else doneCount.toFloat() / planned.size
    val waterMl = remember(water, today) { waterTotal(water, today) }
    val waterProgress = waterMl.toFloat() / settings.waterGoalMl.coerceAtLeast(1)
    val workout = remember(exercises, exLogs, today) { workoutProgress(exercises, exLogs, today) }
    val workedOut = remember(exercises, exLogs, today) { exercises.filter { exerciseTotal(exLogs, it.id, today) > 0 } }
    val stepsProgress = stepsToday.toFloat() / settings.stepGoal.coerceAtLeast(1)
    val sleepToday = sleepLogs.firstOrNull { it.day == today }
    val moodToday = moods.firstOrNull { it.day == today }

    // The daily score is the average of every goal that applies today (each capped at 100%).
    val goals = buildList {
        if (planned.isNotEmpty()) add(habitProgress.coerceAtMost(1f))
        add(waterProgress.coerceAtMost(1f))
        add(workout.coerceAtMost(1f))
        if (vm.stepsSupported && stepsGranted) add(stepsProgress.coerceAtMost(1f))
    }
    val score = if (goals.isEmpty()) 0f else goals.average().toFloat()
    val completeGoals = goals.count { it >= 1f }
    val scorePercent = rememberAnimatedInt((score * 100).toInt())
    val awards = rememberAwards(vm)
    var quoteShift by remember { mutableIntStateOf(0) }

    val bento = LocalLook.current == AppLook.BENTO
    val health = LocalLook.current == AppLook.HEALTH
    val isDark = LocalIsDark.current
    val moodBlock: LazyListScope.() -> Unit = {
        item { SectionTitle("How are you feeling?") }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    MoodPicker(moodToday?.mood ?: 0, { m -> vm.saveMood(today, m, moodToday?.note ?: "") })
                    TextButton(onClick = { onOpen("mood") }, modifier = Modifier.align(Alignment.End)) {
                        Text(if (moodToday == null) "Open mood diary" else "Add a note")
                    }
                }
            }
        }
    }

    if (health) {
        val scheme = MaterialTheme.colorScheme
        val heroColor = healthHeroColor(isDark)
        val glassOn = LocalGlass.current
        // With liquid glass the page background (colour blobs) shows through, so the sheet has no fill.
        val sheetColor = if (glassOn) Color.Transparent else scheme.background
        val teal = healthTone("teal")
        val orange = healthTone("orange")
        val purple = healthTone("purple")
        val blue = healthTone("blue")
        val weekDays = remember(today) { (today - 6..today).toList() }
        val weekSteps = weekDays.map { steps[it] ?: 0 }
        val stepsLeft = (settings.stepGoal - stepsToday).coerceAtLeast(0)
        val habitsLeft = planned.size - doneCount
        val repsToday = remember(exercises, exLogs, today) {
            exercises.filter { it.kind == KIND_REPS }.sumOf { exerciseTotal(exLogs, it.id, today) }
        }
        val coachText = when {
            vm.stepsSupported && stepsGranted && stepsLeft > 0 ->
                "You\u2019re ${formatSteps(stepsLeft)} steps from your goal. A short walk gets you there."
            habitsLeft > 0 -> "$habitsLeft habit${if (habitsLeft == 1) "" else "s"} left today. Small steps add up."
            else -> scoreMessage(score, goals.size)
        }
        val padH = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)

        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(sheetColor),
            contentPadding = PaddingValues(bottom = bottomInset()),
        ) {
            // Tinted top area: title and three progress rings.
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(heroColor)
                        .statusBarsPadding()
                        .padding(bottom = 24.dp),
                ) {
                    ScreenHeader(greeting(), niceDate(today)) {
                        CircleIconButton(Icons.Rounded.Settings, "Settings", { onOpen("settings") })
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        if (vm.stepsSupported) {
                            HeroRing(stepsProgress, formatSteps(rememberAnimatedInt(stepsToday)), "Steps", teal.accent) { onOpen("steps") }
                        } else {
                            HeroRing(workout, "$repsToday", "Workout", orange.accent) { onGoTab(2) }
                        }
                        HeroRing(habitProgress, "$doneCount/${planned.size}", "Habits", purple.accent) { onGoTab(1) }
                        HeroRing(waterProgress, formatMl(rememberAnimatedInt(waterMl)), "Water", blue.accent) { onGoTab(3) }
                    }
                }
            }
            // The sheet starts here: the tinted area is cut with rounded corners, so it looks like the sheet slides over it.
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val r = 32.dp.toPx()
                            val area = Rect(0f, 0f, size.width, size.height)
                            val path = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(area)
                                addRoundRect(
                                    RoundRect(
                                        Rect(0f, 0f, size.width, size.height + r * 2f),
                                        CornerRadius(r),
                                        CornerRadius(r),
                                        CornerRadius.Zero,
                                        CornerRadius.Zero,
                                    ),
                                )
                            }
                            drawPath(path, heroColor)
                        }
                        .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 5.dp),
                ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .staggerIn(0)
                                .clip(RoundedCornerShape(28.dp))
                                .shimmer()
                                .background(Brush.horizontalGradient(listOf(purple.container, blue.container))),
                        ) {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = purple.accent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Coach", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = purple.accent)
                                    Spacer(Modifier.weight(1f))
                                    Text("Score $scorePercent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(coachText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                }
            }
            item { LiveRunBanner(padH.staggerIn(1)) { onOpen("run") } }
            item {
                QuickActionsRow(
                    listOf(
                        QuickAction(iconFor("run"), "Run", teal.accent, teal.container) { onOpen("run") },
                        QuickAction(iconFor("bolt"), "Plans", orange.accent, orange.container) { onOpen("plans") },
                        QuickAction(iconFor("fitness"), "Library", purple.accent, purple.container) { onOpen("library") },
                        QuickAction(iconFor("timer"), "Focus", blue.accent, blue.container) { onOpen("focus") },
                        QuickAction(iconFor("spa"), "Breathe", teal.accent, teal.container) { onOpen("breathe") },
                        QuickAction(iconFor("star"), "Awards", orange.accent, orange.container) { onOpen("awards") },
                    ),
                )
            }
            if (vm.stepsSupported && !stepsGranted) {
                item {
                    TonalCard(scheme.tertiaryContainer, padH) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Allow step counting to see your steps here.",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onTertiaryContainer,
                            )
                            FilledTonalButton(onClick = askSteps) { Text("Allow") }
                        }
                    }
                }
            }

            item { SectionTitle("Fitness") }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .staggerIn(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (vm.stepsSupported) {
                        TonalCard(teal.container, Modifier.weight(1f).fillMaxHeight(), onClick = { onOpen("steps") }) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Steps", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = teal.accent)
                                Text(
                                    formatSteps(rememberAnimatedInt(stepsToday)),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(10.dp))
                                val maxV = maxOf(settings.stepGoal, weekSteps.maxOrNull() ?: 0, 1)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    weekSteps.forEachIndexed { i, v ->
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight((v.toFloat() / maxV).coerceIn(0.08f, 1f))
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(teal.accent.copy(alpha = if (i == weekSteps.lastIndex) 1f else 0.35f)),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        TonalCard(purple.container, Modifier.weight(1f).fillMaxHeight(), onClick = { onOpen("sleep") }) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Sleep", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = purple.accent)
                                Text(
                                    if (sleepToday == null) "-" else formatSleep(sleepToday.minutes),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    TonalCard(orange.container, Modifier.weight(1f).fillMaxHeight(), onClick = { onGoTab(2) }) {
                        Column(Modifier.fillMaxHeight().padding(16.dp)) {
                            Text("Workout", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = orange.accent)
                            Text(
                                if (repsToday > 0) "$repsToday reps" else if (workedOut.isEmpty()) "Not yet" else "${workedOut.size} done",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                            Text(
                                workedOut.firstOrNull()?.name ?: "Tap to start",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Spacer(Modifier.weight(1f))
                            Spacer(Modifier.height(10.dp))
                            MiniBar(workout, orange.accent, height = 6.dp)
                        }
                    }
                }
            }

            item { RunWeekMini(padH.staggerIn(2)) { onOpen("run") } }
            item {
                DailyChallengeCard(
                    today, vm.stepsSupported && stepsGranted, stepsToday, waterMl, settings.waterGoalMl,
                    repsToday, doneCount, planned.size, padH.staggerIn(3),
                )
            }
            item { SectionTitle("Water") }
            item {
                TonalCard(blue.container, padH.staggerIn(3), onClick = { onGoTab(3) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${formatMl(waterMl)} of ${formatMl(settings.waterGoalMl)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(10.dp))
                            MiniBar(waterProgress, blue.accent, height = 7.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(blue.accent)
                                .bounceClick { vm.addWater(settings.glassMl) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = onColorFor(blue.accent), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${settings.glassMl} ml", color = onColorFor(blue.accent), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { SectionTitle("Habits") }
            item {
                MvCard(padH.staggerIn(4)) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        val shown = planned.sortedBy { today in (done[it.id] ?: emptySet()) }.take(5)
                        if (shown.isEmpty()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (habits.isEmpty()) "No habits yet. Add your first one." else "Nothing planned for today.",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                                if (habits.isEmpty()) TextButton(onClick = { onOpen("habitedit:0") }) { Text("Add habit") }
                            }
                        }
                        shown.forEach { h ->
                            val days = done[h.id] ?: emptySet()
                            val isDone = today in days
                            val streak = currentStreak(h.days, days, today)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpen("habit:${h.id}") }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HealthCheck(isDone) { vm.toggleHabit(h.id, today, !isDone) }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    h.name,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    color = if (isDone) scheme.onSurfaceVariant else scheme.onSurface,
                                )
                                if (streak > 0) {
                                    Text("\uD83D\uDD25 $streak", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (planned.isNotEmpty()) {
                            TextButton(onClick = { onGoTab(1) }, modifier = Modifier.padding(start = 12.dp)) {
                                Text(if (planned.size > 5) "View all ${planned.size} habits" else "Manage habits")
                            }
                        }
                    }
                }
            }

            moodBlock()

            item {
                MvCard(padH.staggerIn(5), onClick = { quoteShift += 1 }) {
                    Crossfade(quoteFor(today, quoteShift), label = "quote") { q ->
                        Text(
                            "\u201C$q\u201D",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomInset()),
    ) {
        item {
            if (bento) {
                ScreenHeader(greeting(), niceDate(today)) {
                    // One 40dp round button: sun / moon switches between light and dark.
                    CircleIconButton(
                        if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        if (isDark) "Switch to light mode" else "Switch to dark mode",
                        {
                            vm.updateSettings {
                                it.copy(themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
                            }
                        },
                    )
                }
            } else {
            ScreenHeader(greeting(), niceDate(today), titleFont = ScriptFont, titleSize = 38.sp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            .clickable { onOpen("awards") }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "Lv ${awards.level}",
                            fontFamily = statFont(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    CircleIconButton(Icons.Rounded.Settings, "Settings", { onOpen("settings") })
                }
            }
            }
        }
        item { LiveRunBanner(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { onOpen("run") } }

        item {
            GradientCard(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(0),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressRing(score, size = 118.dp, stroke = 13.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$scorePercent%", fontSize = 32.sp, fontFamily = statFont(), fontWeight = FontWeight.Bold)
                            Text("score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Daily score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            scoreMessage(score, goals.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "$completeGoals of ${goals.size} goals complete",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (bento) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .staggerIn(1),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatTile(
                    Icons.Rounded.TaskAlt, MaterialTheme.colorScheme.primary,
                    "$doneCount/${planned.size}", "Habits", Modifier.weight(1f).fillMaxHeight(),
                    progress = habitProgress, onClick = { onGoTab(1) },
                )
                // Water split card: tap opens the Water tab, the round + adds one glass.
                MvCard(Modifier.weight(1f).fillMaxHeight(), onClick = { onGoTab(3) }) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(iconFor("water"), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .bounceClick { vm.addWater(settings.glassMl) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Add, "Add water", tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            formatMl(rememberAnimatedInt(waterMl)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            maxLines = 1,
                        )
                        Text(
                            "Water",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(10.dp))
                        MiniBar(waterProgress, MaterialTheme.colorScheme.onSurface, height = 5.dp)
                    }
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .staggerIn(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatTile(
                    iconFor("fitness"), itemColor(6),
                    if (workedOut.isEmpty()) "-" else "${workedOut.size} done", "Workout", Modifier.weight(1f).fillMaxHeight(),
                    progress = workout, onClick = { onGoTab(2) },
                )
                if (vm.stepsSupported) {
                    StatTile(
                        iconFor("walk"), StepsTeal,
                        formatSteps(rememberAnimatedInt(stepsToday)), "Steps", Modifier.weight(1f).fillMaxHeight(),
                        progress = stepsProgress, onClick = { onOpen("steps") },
                    )
                } else {
                    StatTile(
                        iconFor("sleep"), itemColor(0),
                        if (sleepToday == null) "-" else formatSleep(sleepToday.minutes), "Sleep", Modifier.weight(1f).fillMaxHeight(),
                        progress = (sleepToday?.minutes ?: 0).toFloat() / settings.sleepGoalMin.coerceAtLeast(1),
                        onClick = { onOpen("sleep") },
                    )
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(1),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickTile(iconFor("timer"), itemColor(6), "Focus", Modifier.weight(1f)) { onOpen("focus") }
                QuickTile(iconFor("spa"), StepsTeal, "Breathe", Modifier.weight(1f)) { onOpen("breathe") }
                QuickTile(iconFor("star"), itemColor(5), "Awards", Modifier.weight(1f)) { onOpen("awards") }
            }
        }
        if (vm.stepsSupported && !stepsGranted) {
            item {
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Allow step counting to see your steps here.",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(onClick = askSteps) { Text("Allow") }
                    }
                }
            }
        }

        item { SectionTitle("Today\u2019s activity") }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    val shownHabits = planned.sortedBy { today in (done[it.id] ?: emptySet()) }.take(5)
                    if (shownHabits.isEmpty() && workedOut.isEmpty()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (habits.isEmpty()) "Nothing here yet. Add your first habit." else "Nothing planned for today.",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (habits.isEmpty()) TextButton(onClick = { onOpen("habitedit:0") }) { Text("Add habit") }
                        }
                    }
                    shownHabits.forEachIndexed { idx, h ->
                        val days = done[h.id] ?: emptySet()
                        val isDone = today in days
                        val streak = currentStreak(h.days, days, today)
                        if (idx > 0) ActivityDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen("habit:${h.id}") }
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckCircle(isDone, MaterialTheme.colorScheme.primary, { vm.toggleHabit(h.id, today, !isDone) }, size = 30.dp)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                h.name,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            if (streak > 0) {
                                Text(
                                    "\uD83D\uDD25 $streak",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    workedOut.take(3).forEachIndexed { idx, e ->
                        val total = exerciseTotal(exLogs, e.id, today)
                        if (idx > 0 || shownHabits.isNotEmpty()) ActivityDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen("ex:${e.id}") }
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(iconFor(e.icon), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                e.name,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                if (e.kind == KIND_TIME) formatDuration(total) else "$total",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (planned.size > 5) {
                        TextButton(onClick = { onGoTab(1) }, modifier = Modifier.padding(start = 8.dp)) {
                            Text("View all ${planned.size} habits")
                        }
                    }
                }
            }
        }
            moodBlock()
        item {
            MvCard(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(2),
                onClick = { quoteShift += 1 },
            ) {
                Crossfade(quoteFor(today, quoteShift), label = "quote") { q ->
                    Text(
                        "\u201C$q\u201D",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        fontFamily = QuoteFont,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                }
            }
        }
        } else {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(1),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickTile(iconFor("timer"), itemColor(6), "Focus", Modifier.weight(1f)) { onOpen("focus") }
                QuickTile(iconFor("spa"), StepsTeal, "Breathe", Modifier.weight(1f)) { onOpen("breathe") }
                QuickTile(iconFor("star"), itemColor(5), "Awards", Modifier.weight(1f)) { onOpen("awards") }
            }
        }
        item {
            MvCard(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(2),
                onClick = { quoteShift += 1 },
            ) {
                Crossfade(quoteFor(today, quoteShift), label = "quote") { q ->
                    Text(
                        "\u201C$q\u201D",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        fontFamily = QuoteFont,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    Icons.Rounded.TaskAlt, MaterialTheme.colorScheme.primary,
                    "$doneCount/${planned.size}", "Habits", Modifier.weight(1f),
                    progress = habitProgress, onClick = { onGoTab(1) },
                )
                StatTile(
                    iconFor("water"), WaterBlue,
                    formatMl(rememberAnimatedInt(waterMl)), "Water", Modifier.weight(1f),
                    progress = waterProgress, onClick = { onGoTab(3) },
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .staggerIn(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    iconFor("fitness"), itemColor(6),
                    if (workedOut.isEmpty()) "-" else "${workedOut.size} done", "Workout", Modifier.weight(1f),
                    progress = workout, onClick = { onGoTab(2) },
                )
                if (vm.stepsSupported) {
                    StatTile(
                        iconFor("walk"), StepsTeal,
                        formatSteps(rememberAnimatedInt(stepsToday)), "Steps", Modifier.weight(1f),
                        progress = stepsProgress, onClick = { onOpen("steps") },
                    )
                } else {
                    StatTile(
                        iconFor("sleep"), itemColor(0),
                        if (sleepToday == null) "-" else formatSleep(sleepToday.minutes), "Sleep", Modifier.weight(1f),
                        progress = (sleepToday?.minutes ?: 0).toFloat() / settings.sleepGoalMin.coerceAtLeast(1),
                        onClick = { onOpen("sleep") },
                    )
                }
            }
        }

        if (vm.stepsSupported && !stepsGranted) {
            item {
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Allow step counting to see your steps here.",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(onClick = askSteps) { Text("Allow") }
                    }
                }
            }
        }

            moodBlock()
        item { SectionTitle("Habits") }
        if (planned.isEmpty()) {
            item {
                EmptyHint(
                    if (habits.isEmpty()) "No habits yet. Add your first one." else "Nothing planned for today.",
                    if (habits.isEmpty()) "Add habit" else null,
                ) { onOpen("habitedit:0") }
            }
        } else {
            items(planned, key = { "habit-${it.id}" }) { h ->
                val days = done[h.id] ?: emptySet()
                HabitRow(
                    habit = h,
                    done = today in days,
                    streak = currentStreak(h.days, days, today),
                    scheduledToday = true,
                    onToggle = { vm.toggleHabit(h.id, today, today !in days) },
                    onClick = { onOpen("habit:${h.id}") },
                )
            }
        }

        item { SectionTitle("Water") }
        item {
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onGoTab(3) }) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatMl(waterMl)} of ${formatMl(settings.waterGoalMl)}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FilledTonalButton(onClick = { vm.addWater(settings.glassMl) }) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${settings.glassMl} ml")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MiniBar(waterProgress, WaterBlue)
                }
            }
        }

        item { SectionTitle("Workout") }
        if (workedOut.isEmpty()) {
            item { EmptyHint("No exercise logged today.", "Start workout") { onGoTab(2) } }
        } else {
            items(workedOut, key = { "ex-${it.id}" }) { e ->
                val total = exerciseTotal(exLogs, e.id, today)
                MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpen("ex:${e.id}") }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(e.icon, itemColor(6))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            MiniBar(total.toFloat() / e.dailyGoal.coerceAtLeast(1), itemColor(6), Modifier.padding(top = 6.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (e.kind == KIND_TIME) formatDuration(total) else "$total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun EmptyHint(text: String, action: String?, onAction: () -> Unit) {
    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) {
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MvCard(modifier, onClick = onClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Thin hairline between rows inside a Bento list card. */
@Composable
private fun ActivityDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(hairline(LocalIsDark.current)),
    )
}

/** One of the three rings in the tinted top area of the Health Today tab. */
@Composable
private fun HeroRing(
    progress: Float,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier.bounceClick(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(progress, size = 100.dp, stroke = 11.dp, color = color, track = color.copy(alpha = 0.2f)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

/** Small rounded suggestion chip (Focus, Breathe, Awards). */
@Composable
private fun ToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .then(
                if (LocalGlass.current) {
                    Modifier.liquidGlass(shape, scheme.surfaceContainer.copy(alpha = 0.55f), blurred = false)
                } else {
                    Modifier
                        .background(scheme.surfaceContainer, shape)
                        .border(1.dp, scheme.outlineVariant, shape)
                },
            )
            .bounceClick(onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Rounded-square tick box (Material style) for habits. */
@Composable
private fun HealthCheck(checked: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(26.dp)
            .clip(shape)
            .background(if (checked) scheme.primary else androidx.compose.ui.graphics.Color.Transparent, shape)
            .border(2.dp, if (checked) scheme.primary else scheme.outline, shape)
            .bounceClick(onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, "Done", tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
    }
}
