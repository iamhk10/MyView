package com.x.myview.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.x.myview.ui.components.FloatingNavBar
import com.x.myview.ui.components.NAV_BAR_HEIGHT
import com.x.myview.ui.components.NavTab
import com.x.myview.ui.components.appBackground
import com.x.myview.ui.screens.ExerciseDetailScreen
import com.x.myview.ui.screens.ExerciseEditScreen
import com.x.myview.ui.screens.HabitDetailScreen
import com.x.myview.ui.screens.HabitEditScreen
import com.x.myview.ui.screens.HabitsScreen
import com.x.myview.ui.screens.SettingsScreen
import com.x.myview.ui.screens.TodayScreen
import com.x.myview.ui.screens.WaterScreen
import com.x.myview.ui.screens.WorkoutScreen
import com.x.myview.ui.theme.LocalBottomInset
import com.x.myview.ui.theme.MyViewTheme

private val TABS = listOf(
    NavTab("Today", Icons.Rounded.Home),
    NavTab("Habits", Icons.Rounded.TaskAlt),
    NavTab("Workout", Icons.Rounded.FitnessCenter),
    NavTab("Water", Icons.Rounded.WaterDrop),
)

@Composable
fun MyViewRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    MyViewTheme(settings) { RootContent(vm) }
}

/**
 * Screens that sit on top of the tabs are named with a short key:
 * "settings", "habit:<id>", "habitedit:<id>" (0 = new), "ex:<id>", "exedit:<id>" (0 = new).
 */
@Composable
private fun RootContent(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = overlay != null) { overlay = null }
    BackHandler(enabled = overlay == null && tab != 0) { tab = 0 }

    val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val inset = NAV_BAR_HEIGHT + 28.dp + navBottom

    fun open(key: String) { overlay = key }
    fun close() { overlay = null }

    Box(Modifier.fillMaxSize().appBackground()) {
        val current = overlay
        if (current == null) {
            CompositionLocalProvider(LocalBottomInset provides inset) {
                Crossfade(tab, label = "tabs", modifier = Modifier.fillMaxSize()) { t ->
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        when (t) {
                            0 -> TodayScreen(vm, onOpen = ::open, onGoTab = { tab = it })
                            1 -> HabitsScreen(vm, onOpen = ::open)
                            2 -> WorkoutScreen(vm, onOpen = ::open)
                            else -> WaterScreen(vm)
                        }
                    }
                }
            }
            FloatingNavBar(
                tabs = TABS,
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp + navBottom),
            )
        } else {
            val parts = current.split(":")
            val id = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                when (parts[0]) {
                    "settings" -> SettingsScreen(vm, onBack = ::close)
                    "habit" -> HabitDetailScreen(vm, id, onBack = ::close, onEdit = { open("habitedit:$id") })
                    "habitedit" -> HabitEditScreen(vm, id, onBack = ::close)
                    "ex" -> ExerciseDetailScreen(vm, id, onBack = ::close, onEdit = { open("exedit:$id") })
                    "exedit" -> ExerciseEditScreen(vm, id, onBack = ::close)
                    else -> close()
                }
            }
        }
    }
}
