package com.macrosnap.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B4F),
    secondary = Color(0xFF4C6358),
    tertiary = Color(0xFF3D6473),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD6B4),
    secondary = Color(0xFFB3CCBE),
    tertiary = Color(0xFFA5CDDE),
)

/** Fixed hues so protein/carbs/fat read the same everywhere, regardless of the dynamic theme. */
object MacroColors {
    val Protein = Color(0xFF4F9DF7)
    val Carbs = Color(0xFFF2A93B)
    val Fat = Color(0xFFE5677D)
}

@Composable
fun MacroSnapTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
