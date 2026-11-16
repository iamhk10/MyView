package com.x.myview.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.myview.data.ThemeMode
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.theme.ACCENTS
import com.x.myview.util.formatMl

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()

    Column(Modifier.fillMaxSize()) {
        BackHeader("Settings", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            SectionTitle("Theme")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip("Auto", s.themeMode == ThemeMode.SYSTEM, { vm.updateSettings { it.copy(themeMode = ThemeMode.SYSTEM) } })
                        ChoiceChip("Light", s.themeMode == ThemeMode.LIGHT, { vm.updateSettings { it.copy(themeMode = ThemeMode.LIGHT) } })
                        ChoiceChip("Dark", s.themeMode == ThemeMode.DARK, { vm.updateSettings { it.copy(themeMode = ThemeMode.DARK) } })
                        ChoiceChip("AMOLED", s.themeMode == ThemeMode.AMOLED, { vm.updateSettings { it.copy(themeMode = ThemeMode.AMOLED) } })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ACCENTS.forEachIndexed { i, a ->
                            val on = i == s.accentIndex && !(s.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(a.color)
                                    .then(
                                        if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier,
                                    )
                                    .clickable { vm.updateSettings { it.copy(accentIndex = i, dynamicColor = false) } },
                            )
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SwitchRow("Wallpaper colours", "Use your phone's colours (Android 12+)", s.dynamicColor) { v ->
                            vm.updateSettings { it.copy(dynamicColor = v) }
                        }
                    }
                    SwitchRow("Liquid glass", "Translucent floating bar and buttons", s.glass) { v ->
                        vm.updateSettings { it.copy(glass = v) }
                    }
                }
            }

            SectionTitle("Water")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepperRow("Daily goal", formatMl(s.waterGoalMl),
                        { vm.updateSettings { it.copy(waterGoalMl = (it.waterGoalMl - 250).coerceAtLeast(500)) } },
                        { vm.updateSettings { it.copy(waterGoalMl = (it.waterGoalMl + 250).coerceAtMost(8000)) } })
                    StepperRow("One glass", "${s.glassMl} ml",
                        { vm.updateSettings { it.copy(glassMl = (it.glassMl - 50).coerceAtLeast(50)) } },
                        { vm.updateSettings { it.copy(glassMl = (it.glassMl + 50).coerceAtMost(1500)) } })
                }
            }

            SectionTitle("About")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MyView", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Version 0.1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Habits, workouts and water in one place. Every feature is free. Your data stays on your phone; the app has no internet access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Developer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "ΣTΣЯПITY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Stepper(value, onMinus, onPlus)
    }
}
