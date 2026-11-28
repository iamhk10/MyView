package com.x.myview.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.x.myview.data.AppLook
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.x.myview.ui.components.Backdrop
import com.x.myview.ui.components.LocalBackdrop
import com.x.myview.ui.components.backdropSource
import com.x.myview.ui.theme.LocalGlass
import com.x.myview.ui.components.BENTO_DOCK_SPACE
import com.x.myview.ui.components.BentoDock
import com.x.myview.ui.components.HEALTH_BOTTOM_SPACE
import com.x.myview.ui.components.HealthFab
import com.x.myview.ui.components.HealthNavBar
import com.x.myview.ui.components.bounceClick
import com.x.myview.ui.components.hairline
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.theme.LocalIsDark
import com.x.myview.util.formatMl
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x.myview.ui.components.ConfettiBurst
import com.x.myview.run.RunTracker
import com.x.myview.ui.components.FloatingNavBar
import com.x.myview.ui.components.IntroGate
import com.x.myview.ui.components.LaunchIntro
import com.x.myview.ui.components.NAV_BAR_HEIGHT
import com.x.myview.ui.components.NavTab
import com.x.myview.ui.components.appBackground
import com.x.myview.ui.components.rememberHaptic
import com.x.myview.ui.screens.AwardsScreen
import com.x.myview.ui.screens.BackupScreen
import com.x.myview.ui.screens.BreathScreen
import com.x.myview.ui.screens.ExerciseDetailScreen
import com.x.myview.ui.screens.ExerciseEditScreen
import com.x.myview.ui.screens.FocusScreen
import com.x.myview.ui.screens.HabitDetailScreen
import com.x.myview.ui.screens.HabitEditScreen
import com.x.myview.ui.screens.HabitsScreen
import com.x.myview.ui.screens.HistoryScreen
import com.x.myview.ui.screens.LibraryDetailScreen
import com.x.myview.ui.screens.LibraryScreen
import com.x.myview.ui.screens.MoodScreen
import com.x.myview.ui.screens.MoreScreen
import com.x.myview.ui.screens.PlanScreen
import com.x.myview.ui.screens.RunScreen
import com.x.myview.ui.screens.PlansScreen
import com.x.myview.ui.screens.SettingsScreen
import com.x.myview.ui.screens.SleepScreen
import com.x.myview.ui.screens.StepsScreen
import com.x.myview.ui.screens.TodayScreen
import com.x.myview.ui.screens.WaterScreen
import com.x.myview.ui.screens.WeightScreen
import com.x.myview.ui.screens.WorkoutScreen
import com.x.myview.ui.theme.LocalBottomInset
import com.x.myview.ui.theme.MyViewTheme

private val TABS = listOf(
    NavTab("Today", Icons.Rounded.Home),
    NavTab("Habits", Icons.Rounded.TaskAlt),
    NavTab("Workout", Icons.Rounded.FitnessCenter),
    NavTab("Water", Icons.Rounded.WaterDrop),
    NavTab("More", Icons.Rounded.MoreHoriz),
)

/** The four dock icons of the Bento look. Water has no icon of its own: it opens from Today, More and the + sheet. */
private val DOCK_TABS = listOf(
    NavTab("Today", Icons.Rounded.Home),
    NavTab("Habits", Icons.Rounded.TaskAlt),
    NavTab("Workout", Icons.Rounded.FitnessCenter),
    NavTab("More", Icons.Rounded.GridView),
)
private val DOCK_TO_TAB = intArrayOf(0, 1, 2, 4)

/**
 * Health look: four tabs like Google Health. Fitness opens the workout tab (2), Sleep the sleep tab (5),
 * Health the More tab (4). Habits (1) and Water (3) are pages inside Today, so Today stays selected.
 */
private val HEALTH_TABS = listOf(
    NavTab("Today", Icons.Rounded.Home),
    NavTab("Fitness", Icons.Rounded.DirectionsRun),
    NavTab("Sleep", Icons.Rounded.Bedtime),
    NavTab("Health", Icons.Rounded.Favorite),
)
private val HEALTH_TO_TAB = intArrayOf(0, 2, 5, 4)

private fun healthPos(tab: Int): Int = when (tab) {
    2 -> 1
    5 -> 2
    4 -> 3
    else -> 0
}

@Composable
fun MyViewRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    MyViewTheme(settings) { RootContent(vm) }
}

/**
 * Screens that sit on top of the tabs are named with a short key: "settings", "steps", "sleep", "weight",
 * "mood", "backup", "history:<metric>", "focus", "breathe", "awards", "habit:<id>", "habitedit:<id>" (0 = new), "ex:<id>", "exedit:<id>" (0 = new), "library", "lib:<key>", "plans", "plan:<n>".
 */
@Composable
private fun RootContent(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    // The opening animation plays once per app start, and not when a run is already going (you want that screen fast).
    var showIntro by remember {
        val play = !IntroGate.played && !RunTracker.isActive()
        IntroGate.played = true
        mutableStateOf(play)
    }
    val celebrate by vm.celebrate.collectAsState()
    val strongHaptic = rememberHaptic(strong = true)
    val firstCelebrate = remember { celebrate }
    LaunchedEffect(celebrate) { if (celebrate > 0 && celebrate != firstCelebrate) strongHaptic() }

    BackHandler(enabled = overlay != null) { overlay = null }
    BackHandler(enabled = overlay == null && tab != 0) { tab = 0 }

    // Launcher shortcuts (long-press the app icon) arrive as a pending route.
    val pending by PendingRoute.route.collectAsState()
    LaunchedEffect(pending) {
        val r = pending ?: return@LaunchedEffect
        if (r.startsWith("tab:")) {
            val n = r.removePrefix("tab:").toIntOrNull()
            if (n != null && n in 0..5) {
                tab = n
                overlay = null
            }
        } else if (r.isNotBlank()) {
            overlay = r
        }
        PendingRoute.route.value = null
    }

    val settings by vm.settings.collectAsState()
    val bento = settings.look == AppLook.BENTO
    val health = settings.look == AppLook.HEALTH
    val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val inset = (if (health) HEALTH_BOTTOM_SPACE else if (bento) BENTO_DOCK_SPACE else NAV_BAR_HEIGHT + 30.dp) + navBottom

    Box(
        Modifier
            .fillMaxSize()
            .appBackground(),
        contentAlignment = Alignment.TopCenter,
    ) {
      // Bento keeps the app in a phone-width column (max 430dp), centred on tablets and wide screens.
      Box(
        Modifier
            .fillMaxHeight()
            .widthIn(max = if (bento) 430.dp else Dp.Infinity)
            .fillMaxWidth(),
      ) {
        AnimatedContent(
            targetState = overlay,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val transform = if (initialState == null && targetState != null) {
                    (slideInHorizontally(tween(340)) { it / 4 } + fadeIn(tween(300))) togetherWith fadeOut(tween(220))
                } else if (targetState == null) {
                    fadeIn(tween(300)) togetherWith (slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(240)))
                } else {
                    fadeIn(tween(260)) togetherWith fadeOut(tween(200))
                }
                transform.apply { targetContentZIndex = if (targetState == null) -1f else 1f }
            },
            label = "overlay",
        ) { current ->
            if (current == null) {
                TabsHost(vm, tab, { tab = it }, { overlay = it }, inset, navBottom, bento, health)
            } else {
                OverlayHost(vm, current, open = { overlay = it }, close = { overlay = null })
            }
        }
        ConfettiBurst(celebrate)
      }
      if (showIntro) LaunchIntro { showIntro = false }
    }
}

@Composable
private fun TabsHost(
    vm: MainViewModel,
    tab: Int,
    onTab: (Int) -> Unit,
    onOpen: (String) -> Unit,
    inset: Dp,
    navBottom: Dp,
    bento: Boolean,
    health: Boolean,
) {
    var showAdd by remember { mutableStateOf(false) }
    // Liquid glass (Health look): the tabs are recorded as a backdrop that the bar and button blur.
    val glass = health && LocalGlass.current
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { Backdrop(layer) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (glass) Modifier.backdropSource(backdrop).appBackground() else Modifier)) {
        CompositionLocalProvider(LocalBottomInset provides inset) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val dir = if (health) {
                        if (healthPos(targetState) >= healthPos(initialState)) 1 else -1
                    } else if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(320)) { dir * it / 6 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(240)) { -dir * it / 6 } + fadeOut(tween(160)))
                },
                label = "tabs",
            ) { t ->
                Box(
                    Modifier
                        .fillMaxSize()
                        // The Health Today tab draws its tinted top area under the status bar itself.
                        .then(if (health && t == 0) Modifier else Modifier.statusBarsPadding()),
                ) {
                    when (t) {
                        0 -> TodayScreen(vm, onOpen = onOpen, onGoTab = onTab)
                        1 -> HabitsScreen(vm, onOpen = onOpen)
                        2 -> WorkoutScreen(vm, onOpen = onOpen)
                        3 -> WaterScreen(vm, onOpen = onOpen)
                        5 -> SleepScreen(vm, onBack = { onTab(0) }, asTab = true)
                        else -> MoreScreen(vm, onOpen = onOpen)
                    }
                }
            }
        }
        }
        if (health) {
            // Only these two sit outside the recorded content, so only they may read the backdrop.
            // (Glass inside the content would draw the layer into itself and crash.)
            CompositionLocalProvider(LocalBackdrop provides (if (glass) backdrop else null)) {
                HealthFab(
                    "Quick add",
                    { showAdd = true },
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 72.dp + 16.dp + navBottom),
                )
                HealthNavBar(
                    tabs = HEALTH_TABS,
                    selected = healthPos(tab),
                    onSelect = { onTab(HEALTH_TO_TAB[it]) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        } else if (bento) {
            BentoDock(
                tabs = DOCK_TABS,
                selected = DOCK_TO_TAB.indexOf(tab),
                onSelect = { onTab(DOCK_TO_TAB[it]) },
                onAdd = { showAdd = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 18.dp + navBottom),
            )
        } else {
            FloatingNavBar(
                tabs = TABS,
                selected = tab,
                onSelect = onTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp + navBottom),
            )
        }
    }
    if (showAdd) {
        QuickAddSheet(vm, onDismiss = { showAdd = false }, onOpen = onOpen, onGoTab = onTab)
    }
}

private class QuickAction(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val run: () -> Unit)

/** The centre "+" of the Bento dock: the most common things to log, one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(vm: MainViewModel, onDismiss: () -> Unit, onOpen: (String) -> Unit, onGoTab: (Int) -> Unit) {
    val settings by vm.settings.collectAsState()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dark = LocalIsDark.current
    val actions = listOf(
        QuickAction(iconFor("water"), "Water ${formatMl(settings.glassMl)}") { vm.addWater(settings.glassMl) },
        QuickAction(Icons.Rounded.Add, "New habit") { onOpen("habitedit:0") },
        QuickAction(Icons.Rounded.FitnessCenter, "Workout") { onGoTab(2) },
        QuickAction(iconFor("heart"), "Mood") { onOpen("mood") },
        QuickAction(iconFor("sleep"), "Sleep") { onOpen("sleep") },
        QuickAction(iconFor("fitness"), "Weight") { onOpen("weight") },
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            Text("Quick add", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Log something in one tap",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            actions.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { a ->
                        val shape = RoundedCornerShape(22.dp)
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape)
                                .border(1.dp, hairline(dark), shape)
                                .bounceClick {
                                    onDismiss()
                                    a.run()
                                }
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(a.icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(a.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayHost(vm: MainViewModel, key: String, open: (String) -> Unit, close: () -> Unit) {
    val parts = key.split(":")
    val id = parts.getOrNull(1)?.toLongOrNull() ?: 0L
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        when (parts[0]) {
            "settings" -> SettingsScreen(vm, onBack = close)
            "steps" -> StepsScreen(vm, onBack = close, onHistory = { open("history:steps") })
            "focus" -> FocusScreen(vm, onBack = close)
            "breathe" -> BreathScreen(vm, onBack = close)
            "awards" -> AwardsScreen(vm, onBack = close)
            "run" -> RunScreen(vm, onBack = close)
            "library" -> LibraryScreen(vm, onBack = close, onOpen = open)
            "lib" -> LibraryDetailScreen(vm, parts.getOrNull(1) ?: "", onBack = close, onOpen = open)
            "plans" -> PlansScreen(vm, onBack = close, onOpen = open)
            "plan" -> PlanScreen(vm, id.toInt(), onBack = close)
            "history" -> HistoryScreen(vm, start = parts.getOrNull(1) ?: "steps", onBack = close)
            "sleep" -> SleepScreen(vm, onBack = close)
            "weight" -> WeightScreen(vm, onBack = close)
            "mood" -> MoodScreen(vm, onBack = close)
            "backup" -> BackupScreen(vm, onBack = close)
            "habit" -> HabitDetailScreen(vm, id, onBack = close, onEdit = { open("habitedit:$id") })
            "habitedit" -> HabitEditScreen(vm, id, onBack = close)
            "ex" -> ExerciseDetailScreen(vm, id, onBack = close, onEdit = { open("exedit:$id") })
            "exedit" -> ExerciseEditScreen(vm, id, onBack = close)
            else -> LaunchedEffect(key) { close() }
        }
    }
}
