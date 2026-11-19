package com.x.myview.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
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
import com.x.myview.ui.components.FloatingNavBar
import com.x.myview.ui.components.NAV_BAR_HEIGHT
import com.x.myview.ui.components.NavTab
import com.x.myview.ui.components.appBackground
import com.x.myview.ui.components.rememberHaptic
import com.x.myview.ui.screens.BackupScreen
import com.x.myview.ui.screens.ExerciseDetailScreen
import com.x.myview.ui.screens.ExerciseEditScreen
import com.x.myview.ui.screens.HabitDetailScreen
import com.x.myview.ui.screens.HabitEditScreen
import com.x.myview.ui.screens.HabitsScreen
import com.x.myview.ui.screens.MoodScreen
import com.x.myview.ui.screens.MoreScreen
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

@Composable
fun MyViewRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    MyViewTheme(settings) { RootContent(vm) }
}

/**
 * Screens that sit on top of the tabs are named with a short key: "settings", "steps", "sleep", "weight",
 * "mood", "backup", "habit:<id>", "habitedit:<id>" (0 = new), "ex:<id>", "exedit:<id>" (0 = new).
 */
@Composable
private fun RootContent(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    val celebrate by vm.celebrate.collectAsState()
    val strongHaptic = rememberHaptic(strong = true)
    val firstCelebrate = remember { celebrate }
    LaunchedEffect(celebrate) { if (celebrate > 0 && celebrate != firstCelebrate) strongHaptic() }

    BackHandler(enabled = overlay != null) { overlay = null }
    BackHandler(enabled = overlay == null && tab != 0) { tab = 0 }

    val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val inset = NAV_BAR_HEIGHT + 30.dp + navBottom

    Box(
        Modifier
            .fillMaxSize()
            .appBackground(),
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
                TabsHost(vm, tab, { tab = it }, { overlay = it }, inset, navBottom)
            } else {
                OverlayHost(vm, current, open = { overlay = it }, close = { overlay = null })
            }
        }
        ConfettiBurst(celebrate)
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
) {
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomInset provides inset) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(320)) { dir * it / 6 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(240)) { -dir * it / 6 } + fadeOut(tween(160)))
                },
                label = "tabs",
            ) { t ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    when (t) {
                        0 -> TodayScreen(vm, onOpen = onOpen, onGoTab = onTab)
                        1 -> HabitsScreen(vm, onOpen = onOpen)
                        2 -> WorkoutScreen(vm, onOpen = onOpen)
                        3 -> WaterScreen(vm)
                        else -> MoreScreen(vm, onOpen = onOpen)
                    }
                }
            }
        }
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
            "steps" -> StepsScreen(vm, onBack = close)
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
