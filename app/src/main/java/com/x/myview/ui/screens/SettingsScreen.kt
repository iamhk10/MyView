package com.x.myview.ui.screens

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.myview.R
import com.x.myview.data.AppLook
import com.x.myview.data.DisplayFont
import com.x.myview.data.FontOption
import com.x.myview.data.TextSize
import com.x.myview.data.ThemeMode
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.SectionTitle
import com.x.myview.ui.components.bounceClick
import com.x.myview.ui.theme.ACCENTS
import com.x.myview.util.formatClock
import com.x.myview.util.formatMl
import com.x.myview.util.formatSleep
import com.x.myview.util.formatSteps

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val askNotifications = rememberNotificationRequest()
    val wallpaperOn = s.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChipFlow {
                        ChoiceChip("Auto", s.themeMode == ThemeMode.SYSTEM, { vm.updateSettings { it.copy(themeMode = ThemeMode.SYSTEM) } })
                        ChoiceChip("Light", s.themeMode == ThemeMode.LIGHT, { vm.updateSettings { it.copy(themeMode = ThemeMode.LIGHT) } })
                        ChoiceChip("Dark", s.themeMode == ThemeMode.DARK, { vm.updateSettings { it.copy(themeMode = ThemeMode.DARK) } })
                        ChoiceChip("AMOLED", s.themeMode == ThemeMode.AMOLED, { vm.updateSettings { it.copy(themeMode = ThemeMode.AMOLED) } })
                    }
                    Text("Look", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ChipFlow {
                        AppLook.values().forEach { l ->
                            ChoiceChip(l.label, s.look == l, { vm.updateSettings { it.copy(look = l) } })
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ACCENTS.chunked(5).forEachIndexed { rowIndex, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEachIndexed { i, a ->
                                    val index = rowIndex * 5 + i
                                    val on = index == s.accentIndex && !wallpaperOn
                                    Box(
                                        Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(a.color)
                                            .then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                            .bounceClick { vm.updateSettings { it.copy(accentIndex = index, dynamicColor = false) } },
                                    )
                                }
                            }
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

            SectionTitle("Text")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Font style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ChipFlow {
                        FontOption.values().forEach { f ->
                            ChoiceChip(f.label, s.fontOption == f, { vm.updateSettings { it.copy(fontOption = f) } })
                        }
                    }
                    Text("Heading font", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ChipFlow {
                        DisplayFont.values().forEach { f ->
                            ChoiceChip(f.label, s.displayFont == f, { vm.updateSettings { it.copy(displayFont = f) } })
                        }
                    }
                    Text("Text size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ChipFlow {
                        TextSize.values().forEach { t ->
                            ChoiceChip(t.label, s.textSize == t, { vm.updateSettings { it.copy(textSize = t) } })
                        }
                    }
                    Text(
                        "Small steps every day add up to big changes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle("Widgets")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Widget style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ChipFlow {
                        ChoiceChip("Light", s.widgetStyle == 0, { vm.updateSettings { it.copy(widgetStyle = 0) } })
                        ChoiceChip("Dark", s.widgetStyle == 1, { vm.updateSettings { it.copy(widgetStyle = 1) } })
                        ChoiceChip("Accent colour", s.widgetStyle == 2, { vm.updateSettings { it.copy(widgetStyle = 2) } })
                    }
                    Text(
                        "Applies to the Steps, Streak, Workout and Quote widgets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AiCoachSettings(vm)

            SectionTitle("Feel")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SwitchRow("Vibration", "A light tap on ticks, taps and tab changes", s.haptics) { v ->
                        vm.updateSettings { it.copy(haptics = v) }
                    }
                    SwitchRow("Confetti", "Celebrate when you complete a daily goal", s.celebrate) { v ->
                        vm.updateSettings { it.copy(celebrate = v) }
                    }
                }
            }

            SectionTitle("Daily goals")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepperRow("Water", formatMl(s.waterGoalMl),
                        { vm.updateSettings { it.copy(waterGoalMl = (it.waterGoalMl - 250).coerceAtLeast(500)) } },
                        { vm.updateSettings { it.copy(waterGoalMl = (it.waterGoalMl + 250).coerceAtMost(8000)) } })
                    StepperRow("One glass", "${s.glassMl} ml",
                        { vm.updateSettings { it.copy(glassMl = (it.glassMl - 50).coerceAtLeast(50)) } },
                        { vm.updateSettings { it.copy(glassMl = (it.glassMl + 50).coerceAtMost(1500)) } })
                    if (vm.stepsSupported) {
                        SwitchRow("Live step counting", "Updates steps and the widget within seconds, even with the app closed. Shows one quiet notification.", s.liveSteps) { v ->
                            vm.updateSettings { it.copy(liveSteps = v) }
                        }
                        StepperRow("Steps", formatSteps(s.stepGoal),
                            { vm.updateSettings { it.copy(stepGoal = (it.stepGoal - 500).coerceAtLeast(1000)) } },
                            { vm.updateSettings { it.copy(stepGoal = (it.stepGoal + 500).coerceAtMost(50000)) } })
                    }
                    StepperRow("Sleep", formatSleep(s.sleepGoalMin),
                        { vm.updateSettings { it.copy(sleepGoalMin = (it.sleepGoalMin - 30).coerceAtLeast(240)) } },
                        { vm.updateSettings { it.copy(sleepGoalMin = (it.sleepGoalMin + 30).coerceAtMost(720)) } })
                }
            }

            SectionTitle("Reminders")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SwitchRow("Water reminders", "Between 8 AM and 10 PM, only while you are below your goal", s.waterReminder) { v ->
                        if (v) askNotifications()
                        vm.updateSettings { it.copy(waterReminder = v) }
                    }
                    if (s.waterReminder) {
                        StepperRow("Every", if (s.waterIntervalH == 1) "1 hour" else "${s.waterIntervalH} hours",
                            { vm.updateSettings { it.copy(waterIntervalH = (it.waterIntervalH - 1).coerceAtLeast(1)) } },
                            { vm.updateSettings { it.copy(waterIntervalH = (it.waterIntervalH + 1).coerceAtMost(6)) } })
                    }
                    SwitchRow("Workout reminder", "Daily, only if you have not logged any exercise", s.workoutReminder) { v ->
                        if (v) askNotifications()
                        vm.updateSettings { it.copy(workoutReminder = v) }
                    }
                    if (s.workoutReminder) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatClock(s.workoutReminderMin),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TimeStepper(s.workoutReminderMin) { m -> vm.updateSettings { it.copy(workoutReminderMin = m) } }
                        }
                    }
                    Text(
                        "Habit reminders are set inside each habit. Android can deliver reminders a few minutes late. If they do not show up on Xiaomi, Oppo, Vivo, Realme or Samsung phones, set MyView battery usage to Unrestricted in the phone's app settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle("About")
            MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Image(
                        painterResource(R.drawable.logo_full),
                        contentDescription = "MyView logo",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                    Text("MyView", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Version 1.2.1", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Habits, workouts, water, steps, sleep, weight and mood in one place. Every feature is free. Your data stays on your phone; the app has no internet access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Developer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ShimmerName("ΣTΣЯПITY")
                }
            }
        }
    }
}

/** Developer name with a slowly moving gradient. */
@Composable
private fun ShimmerName(name: String) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        0f, 600f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "shift",
    )
    val brush = Brush.linearGradient(
        colors = listOf(scheme.primary, scheme.tertiary, Color(0xFFFF6FB5), scheme.primary),
        start = Offset(shift, 0f),
        end = Offset(shift + 300f, 0f),
        tileMode = TileMode.Repeated,
    )
    Text(
        name,
        style = TextStyle(brush = brush, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
    )
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
