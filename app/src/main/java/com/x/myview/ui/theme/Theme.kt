package com.x.myview.ui.theme

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.x.myview.data.AppSettings
import com.x.myview.data.FontOption
import com.x.myview.data.ThemeMode

class AccentOption(val name: String, val color: Color)

val ACCENTS = listOf(
    AccentOption("Violet", Color(0xFFB69CFF)),
    AccentOption("Pink", Color(0xFFFF6FB5)),
    AccentOption("Blue", Color(0xFF6EA8FF)),
    AccentOption("Teal", Color(0xFF3DD6C6)),
    AccentOption("Green", Color(0xFF7BDC8A)),
    AccentOption("Orange", Color(0xFFFFA14A)),
    AccentOption("Red", Color(0xFFFF6B6B)),
    AccentOption("Gold", Color(0xFFFFC24A)),
    AccentOption("Indigo", Color(0xFF8C9EFF)),
)

/** Colours a habit or exercise can use. Stored as an index. */
val ITEM_COLORS = listOf(
    Color(0xFFB69CFF), Color(0xFFFF6FB5), Color(0xFF6EA8FF), Color(0xFF3DD6C6),
    Color(0xFF7BDC8A), Color(0xFFFFC24A), Color(0xFFFF8A4A), Color(0xFFFF6B6B),
)

fun itemColor(index: Int): Color = ITEM_COLORS[index.coerceIn(0, ITEM_COLORS.lastIndex)]

val WaterBlue = Color(0xFF4DB8FF)
val FireOrange = Color(0xFFFF9A3D)
val StepsTeal = Color(0xFF3DD6C6)

val LocalIsDark = staticCompositionLocalOf { true }

/** Whether taps give a small vibration (a setting). */
val LocalHapticsOn = staticCompositionLocalOf { true }

/** Font families that every Android phone already has. */
fun fontFamilyFor(option: FontOption): FontFamily = when (option) {
    FontOption.DEFAULT -> FontFamily.Default
    FontOption.MEDIUM -> FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
    FontOption.CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
    FontOption.LIGHT -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
    FontOption.SERIF -> FontFamily.Serif
    FontOption.MONO -> FontFamily.Monospace
}

/** Material typography with the chosen font and size scale; big headings are bold and slightly tighter. */
private fun buildTypography(family: FontFamily, scale: Float): Typography {
    val base = Typography()
    fun TextStyle.styled(bold: Boolean = false): TextStyle = copy(
        fontFamily = family,
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontWeight = if (bold) FontWeight.Bold else fontWeight,
        letterSpacing = if (bold) (-0.4).sp else letterSpacing,
    )
    return Typography(
        displayLarge = base.displayLarge.styled(true),
        displayMedium = base.displayMedium.styled(true),
        displaySmall = base.displaySmall.styled(true),
        headlineLarge = base.headlineLarge.styled(true),
        headlineMedium = base.headlineMedium.styled(true),
        headlineSmall = base.headlineSmall.styled(true),
        titleLarge = base.titleLarge.styled(),
        titleMedium = base.titleMedium.styled(),
        titleSmall = base.titleSmall.styled(),
        bodyLarge = base.bodyLarge.styled(),
        bodyMedium = base.bodyMedium.styled(),
        bodySmall = base.bodySmall.styled(),
        labelLarge = base.labelLarge.styled(),
        labelMedium = base.labelMedium.styled(),
        labelSmall = base.labelSmall.styled(),
    )
}
val LocalGlass = staticCompositionLocalOf { false }

/** Height of the floating nav bar plus margins, so scrolling lists can leave room under them. */
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

fun onColorFor(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF1B1530) else Color.White

private fun tuneAccent(seed: Color, dark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    if (hsl[1] > 0.12f) hsl[1] = hsl[1].coerceIn(0.35f, 0.95f)
    hsl[2] = if (dark) hsl[2].coerceIn(0.62f, 0.78f) else hsl[2].coerceIn(0.32f, 0.48f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun buildScheme(seed: Color, dark: Boolean, amoled: Boolean): ColorScheme {
    val accent = tuneAccent(seed, dark)
    return if (dark) {
        val bg = if (amoled) Color.Black else lerp(Color(0xFF0B0B12), accent, 0.07f)
        val onSurface = Color(0xFFEDEAF3)
        darkColorScheme(
            primary = accent,
            onPrimary = onColorFor(accent),
            primaryContainer = lerp(bg, accent, 0.38f),
            onPrimaryContainer = lerp(accent, Color.White, 0.65f),
            secondary = lerp(accent, Color.White, 0.35f),
            onSecondary = onColorFor(lerp(accent, Color.White, 0.35f)),
            tertiary = lerp(accent, Color.White, 0.2f),
            background = bg,
            onBackground = onSurface,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = lerp(bg, Color.White, 0.09f),
            onSurfaceVariant = Color(0xFFB9B5C6),
            surfaceTint = accent,
            outline = Color(0xFF7F7A8E),
            outlineVariant = lerp(bg, Color.White, 0.16f),
            surfaceContainerLowest = lerp(bg, Color.Black, 0.3f),
            surfaceContainerLow = lerp(bg, Color.White, 0.03f),
            surfaceContainer = lerp(bg, Color.White, 0.06f),
            surfaceContainerHigh = lerp(bg, Color.White, 0.09f),
            surfaceContainerHighest = lerp(bg, Color.White, 0.12f),
            error = Color(0xFFFFB4AB),
        )
    } else {
        val bg = lerp(Color(0xFFF8F6FC), accent, 0.06f)
        val onSurface = Color(0xFF1A1823)
        lightColorScheme(
            primary = accent,
            onPrimary = onColorFor(accent),
            primaryContainer = lerp(bg, accent, 0.28f),
            onPrimaryContainer = lerp(accent, Color.Black, 0.6f),
            secondary = lerp(accent, Color.Black, 0.15f),
            onSecondary = onColorFor(lerp(accent, Color.Black, 0.15f)),
            tertiary = lerp(accent, Color.Black, 0.25f),
            background = bg,
            onBackground = onSurface,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = lerp(bg, Color.Black, 0.06f),
            onSurfaceVariant = Color(0xFF5B5768),
            surfaceTint = accent,
            outline = Color(0xFF8A8598),
            outlineVariant = lerp(bg, Color.Black, 0.14f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(bg, Color.White, 0.5f),
            surfaceContainer = lerp(bg, Color.Black, 0.03f),
            surfaceContainerHigh = lerp(bg, Color.Black, 0.06f),
            surfaceContainerHighest = lerp(bg, Color.Black, 0.09f),
            error = Color(0xFFB3261E),
        )
    }
}

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
)

@Composable
fun MyViewTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = settings.themeMode == ThemeMode.AMOLED
    val useWallpaper = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val target = ACCENTS[settings.accentIndex.coerceIn(0, ACCENTS.lastIndex)].color
    val seed by animateColorAsState(target, tween(400), label = "accent")

    val scheme = if (useWallpaper) {
        val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (amoled) base.toAmoled() else base
    } else {
        remember(seed, dark, amoled) { buildScheme(seed, dark, amoled) }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(dark) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            onDispose { }
        }
    }

    val typography = remember(settings.fontOption, settings.textSize) {
        buildTypography(fontFamilyFor(settings.fontOption), settings.textSize.scale)
    }

    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalGlass provides settings.glass,
        LocalHapticsOn provides settings.haptics,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            // Text and icons outside a Surface default to black. Providing the content colour here
            // keeps every screen readable in light, dark, AMOLED and wallpaper-colour modes.
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}

@Composable
fun bottomInset(): Dp = LocalBottomInset.current
