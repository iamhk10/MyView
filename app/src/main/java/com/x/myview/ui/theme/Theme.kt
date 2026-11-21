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
import com.x.myview.data.AppLook
import com.x.myview.data.AppSettings
import com.x.myview.data.DisplayFont
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
    AccentOption("Silver", Color(0xFFC9CCD8)),
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

/** The one accent of the Bento look: Electric Indigo, used for the main action and active states only. */
val ElectricIndigo = Color(0xFF6366F1)

val LocalIsDark = staticCompositionLocalOf { true }

/** Which overall look is active (cards, background and titles change with it). */
val LocalLook = staticCompositionLocalOf { AppLook.MIDNIGHT }

/** Brushed-silver edge colour and the deep violet used by the Midnight look. */
val Silver = Color(0xFFDADDE9)
val DeepViolet = Color(0xFF241257)

/** Fonts for a few special places (Android ships them all, nothing is downloaded). */
val ScriptFont: FontFamily = FontFamily(Typeface.create("cursive", Typeface.BOLD))
val QuoteFont: FontFamily = FontFamily(Typeface.create("serif", Typeface.ITALIC))
val NumberFont: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
val BlackFont: FontFamily = FontFamily(Typeface.create("sans-serif-black", Typeface.NORMAL))

/** Big numbers use the condensed font, except in the Bento look which keeps one clean sans-serif. */
@Composable
fun statFont(): FontFamily? = if (LocalLook.current == AppLook.BENTO) null else NumberFont

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

fun displayFamilyFor(option: DisplayFont, text: FontFamily): FontFamily = when (option) {
    DisplayFont.BLACK -> BlackFont
    DisplayFont.CONDENSED -> NumberFont
    DisplayFont.SERIF -> FontFamily(Typeface.create("serif", Typeface.BOLD))
    DisplayFont.SCRIPT -> ScriptFont
    DisplayFont.CASUAL -> FontFamily(Typeface.create("casual", Typeface.NORMAL))
    DisplayFont.SMALL_CAPS -> FontFamily(Typeface.create("sans-serif-smallcaps", Typeface.NORMAL))
    DisplayFont.SAME -> text
}

/**
 * Material typography with the chosen fonts and size scale. Big headings and the larger titles use the
 * display font, everything else the body font.
 */
private fun buildTypography(text: FontFamily, display: FontFamily, scale: Float, tabular: Boolean = false): Typography {
    val base = Typography()
    fun TextStyle.styled(bold: Boolean = false, family: FontFamily = text): TextStyle = copy(
        fontFamily = family,
        fontFeatureSettings = if (tabular) "tnum" else fontFeatureSettings,
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontWeight = if (bold) FontWeight.Bold else fontWeight,
        letterSpacing = if (bold) (-0.4).sp else letterSpacing,
    )
    return Typography(
        displayLarge = base.displayLarge.styled(true, display),
        displayMedium = base.displayMedium.styled(true, display),
        displaySmall = base.displaySmall.styled(true, display),
        headlineLarge = base.headlineLarge.styled(true, display),
        headlineMedium = base.headlineMedium.styled(true, display),
        headlineSmall = base.headlineSmall.styled(true, display),
        titleLarge = base.titleLarge.styled(false, display),
        titleMedium = base.titleMedium.styled(false, display),
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

/** Bento look: charcoal background, slate cards, white text, a single Electric Indigo accent. */
private fun buildBentoScheme(dark: Boolean, amoled: Boolean): ColorScheme {
    val accent = ElectricIndigo
    return if (dark) {
        val bg = if (amoled) Color.Black else Color(0xFF0D0F12)
        val card = Color(0xFF16191F)
        val text = Color(0xFFF8FAFC)
        val muted = Color(0xFF94A3B8)
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = lerp(card, accent, 0.30f),
            onPrimaryContainer = Color(0xFFE0E7FF),
            secondary = Color(0xFFA5B4FC),
            onSecondary = Color(0xFF111827),
            tertiary = Color(0xFFA5B4FC),
            background = bg,
            onBackground = text,
            surface = bg,
            onSurface = text,
            surfaceVariant = Color(0xFF1D2128),
            onSurfaceVariant = muted,
            surfaceTint = accent,
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF262B33),
            surfaceContainerLowest = Color(0xFF0A0C0F),
            surfaceContainerLow = Color(0xFF12151A),
            surfaceContainer = Color(0xFF14171C),
            surfaceContainerHigh = card,
            surfaceContainerHighest = Color(0xFF1D2128),
            error = Color(0xFFFCA5A5),
        )
    } else {
        val bg = Color(0xFFF3F4F8)
        val text = Color(0xFF0F172A)
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E7FF),
            onPrimaryContainer = Color(0xFF312E81),
            secondary = Color(0xFF4F46E5),
            onSecondary = Color.White,
            tertiary = Color(0xFF4F46E5),
            background = bg,
            onBackground = text,
            surface = bg,
            onSurface = text,
            surfaceVariant = Color(0xFFE8EAF0),
            onSurfaceVariant = Color(0xFF64748B),
            surfaceTint = accent,
            outline = Color(0xFF94A3B8),
            outlineVariant = Color(0xFFE2E5EC),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFFAFAFC),
            surfaceContainer = Color(0xFFF8F9FB),
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFEEF0F5),
            error = Color(0xFFB3261E),
        )
    }
}

private fun buildScheme(seed: Color, dark: Boolean, amoled: Boolean, midnight: Boolean): ColorScheme {
    val accent = tuneAccent(seed, dark)
    return if (dark) {
        val bg = if (amoled) Color.Black else if (midnight) lerp(Color(0xFF07060C), accent, 0.045f) else lerp(Color(0xFF0B0B12), accent, 0.07f)
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
    val bento = settings.look == AppLook.BENTO
    val useWallpaper = settings.dynamicColor && !bento && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val target = ACCENTS[settings.accentIndex.coerceIn(0, ACCENTS.lastIndex)].color
    val seed by animateColorAsState(target, tween(400), label = "accent")

    val scheme = if (useWallpaper) {
        val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (amoled) base.toAmoled() else base
    } else {
        remember(seed, dark, amoled, settings.look) {
            if (bento) buildBentoScheme(dark, amoled) else buildScheme(seed, dark, amoled, settings.look == AppLook.MIDNIGHT)
        }
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

    val typography = remember(settings.fontOption, settings.displayFont, settings.textSize, bento) {
        val text = fontFamilyFor(settings.fontOption)
        // Bento keeps one clean sans-serif everywhere, with tabular (equal width) digits.
        val display = if (bento) text else displayFamilyFor(settings.displayFont, text)
        buildTypography(text, display, settings.textSize.scale, tabular = bento)
    }

    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalGlass provides settings.glass,
        LocalHapticsOn provides settings.haptics,
        LocalLook provides settings.look,
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
