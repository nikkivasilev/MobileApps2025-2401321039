package com.macrosnap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * "Ink and data": a monochrome UI where colour is reserved for what's being tracked. Primary is
 * ink (near-black, near-white in dark mode) so buttons and selection never compete with the
 * macro, water and sleep hues. The palette is fixed rather than wallpaper-derived (dynamic
 * colour) because arbitrary wallpaper hues clash with the fixed data colours.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF111114),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9E9EE),
    onPrimaryContainer = Color(0xFF111114),
    secondary = Color(0xFF55555F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E9EE),
    onSecondaryContainer = Color(0xFF111114),
    tertiary = Color(0xFF1B9A63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDF5E9),
    onTertiaryContainer = Color(0xFF0B3D27),
    error = Color(0xFFD93A40),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF6B1216),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFF5F5F7),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = Color(0xFF6B6B76),
    surfaceTint = Color(0xFF111114),
    inverseSurface = Color(0xFF232328),
    inverseOnSurface = Color(0xFFF3F3F6),
    inversePrimary = Color(0xFFF3F3F6),
    outline = Color(0xFFB9B9C2),
    outlineVariant = Color(0xFFE2E2E7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE4E4E9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F0F3),
    surfaceContainerHigh = Color(0xFFEDEDF0),
    surfaceContainerHighest = Color(0xFFE6E6EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF3F3F6),
    onPrimary = Color(0xFF0E0E10),
    primaryContainer = Color(0xFF2A2A30),
    onPrimaryContainer = Color(0xFFF3F3F6),
    secondary = Color(0xFFB4B4BF),
    onSecondary = Color(0xFF0E0E10),
    secondaryContainer = Color(0xFF2A2A30),
    onSecondaryContainer = Color(0xFFF3F3F6),
    tertiary = Color(0xFF3DD68C),
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF123D2A),
    onTertiaryContainer = Color(0xFFB5F2D2),
    error = Color(0xFFFF6B6F),
    onError = Color(0xFF3B0709),
    errorContainer = Color(0xFF4A1417),
    onErrorContainer = Color(0xFFFFDAD9),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF3F3F6),
    surface = Color(0xFF0E0E10),
    onSurface = Color(0xFFF3F3F6),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFF9C9CA8),
    surfaceTint = Color(0xFFF3F3F6),
    inverseSurface = Color(0xFFF3F3F6),
    inverseOnSurface = Color(0xFF17171A),
    inversePrimary = Color(0xFF111114),
    outline = Color(0xFF55555E),
    outlineVariant = Color(0xFF2C2C32),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2E2E34),
    surfaceDim = Color(0xFF0E0E10),
    surfaceContainerLowest = Color(0xFF0A0A0C),
    surfaceContainerLow = Color(0xFF1A1A1E),
    surfaceContainer = Color(0xFF1C1C21),
    surfaceContainerHigh = Color(0xFF242429),
    surfaceContainerHighest = Color(0xFF2E2E34),
)

/** Colours outside Material's roles: card surfaces and the fixed hue of each tracked quantity. */
@Immutable
data class AppColors(
    /** Flat card on the grey page: white in light mode, a lifted grey in dark mode. */
    val card: Color,
    /** Tinted tiles inside a card (macro tiles, add-method tiles). */
    val tile: Color,
    /** Unfilled part of rings and bars. */
    val track: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val water: Color,
    val sleep: Color,
    val micros: Color,
    val calories: Color,
    /** Stars, streaks and other small highlights. */
    val highlight: Color,
    /** Text and icons on a filled data colour (the water button, a ticked check); dark in both themes for contrast. */
    val onData: Color,
)

private val LightAppColors = AppColors(
    card = Color(0xFFFFFFFF),
    tile = Color(0xFFF5F5F7),
    track = Color(0xFFEAEAEF),
    protein = Color(0xFFEF5A6F),
    carbs = Color(0xFFF09A2B),
    fat = Color(0xFF4C8DF6),
    water = Color(0xFF1FAFC7),
    sleep = Color(0xFF7B61FF),
    micros = Color(0xFF1B9A63),
    calories = Color(0xFF111114),
    highlight = Color(0xFFF5B400),
    onData = Color(0xFF111114),
)

private val DarkAppColors = AppColors(
    card = Color(0xFF1A1A1E),
    tile = Color(0xFF242429),
    track = Color(0xFF2E2E34),
    protein = Color(0xFFFF7385),
    carbs = Color(0xFFFFB84D),
    fat = Color(0xFF6BA4FF),
    water = Color(0xFF3CCBE0),
    sleep = Color(0xFF9A86FF),
    micros = Color(0xFF3DD68C),
    calories = Color(0xFFF3F3F6),
    highlight = Color(0xFFFFC933),
    onData = Color(0xFF0E0E10),
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}

/** Fixed hues so protein/carbs/fat read the same everywhere; slightly lighter in dark mode. */
object MacroColors {
    val Protein: Color
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current.protein
    val Carbs: Color
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current.carbs
    val Fat: Color
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current.fat
}

private val AppShapes = Shapes(
    // Text fields and menus.
    extraSmall = RoundedCornerShape(12.dp),
    // Chips and small tiles.
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    // Cards and FABs.
    large = RoundedCornerShape(24.dp),
    // Sheets and dialogs.
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MacroSnapTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
