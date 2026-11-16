package com.x.myview.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon keys stored in the database mapped to real icons. Unknown keys fall back to a check mark. */
private val ICONS: Map<String, ImageVector> = mapOf(
    "check" to Icons.Rounded.Check,
    "fitness" to Icons.Rounded.FitnessCenter,
    "pushup" to Icons.Rounded.FitnessCenter,
    "pullup" to Icons.Rounded.Accessibility,
    "squat" to Icons.Rounded.AccessibilityNew,
    "situp" to Icons.Rounded.SelfImprovement,
    "run" to Icons.Rounded.DirectionsRun,
    "walk" to Icons.Rounded.DirectionsWalk,
    "bike" to Icons.Rounded.DirectionsBike,
    "timer" to Icons.Rounded.Timer,
    "bolt" to Icons.Rounded.Bolt,
    "yoga" to Icons.Rounded.SelfImprovement,
    "book" to Icons.Rounded.MenuBook,
    "sleep" to Icons.Rounded.Bedtime,
    "water" to Icons.Rounded.WaterDrop,
    "fire" to Icons.Rounded.LocalFireDepartment,
    "heart" to Icons.Rounded.Favorite,
    "spa" to Icons.Rounded.Spa,
    "code" to Icons.Rounded.Code,
    "music" to Icons.Rounded.MusicNote,
    "brush" to Icons.Rounded.Brush,
    "star" to Icons.Rounded.Star,
    "food" to Icons.Rounded.Restaurant,
    "pill" to Icons.Rounded.Medication,
    "money" to Icons.Rounded.Savings,
)

fun iconFor(key: String): ImageVector = ICONS[key] ?: Icons.Rounded.Check

/** Icons offered in the pickers, in display order. */
val HABIT_ICON_KEYS = listOf(
    "check", "fitness", "run", "walk", "bike", "yoga", "water", "book", "sleep", "food",
    "pill", "heart", "spa", "code", "music", "brush", "money", "star", "fire", "bolt",
)

val EXERCISE_ICON_KEYS = listOf(
    "pushup", "pullup", "squat", "situp", "run", "walk", "bike", "yoga", "timer", "bolt", "fitness", "fire",
)
