package com.macrosnap.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.macrosnap.app.R

/**
 * Inter (SIL Open Font License, see third_party/inter/OFL.txt), bundled as static weights from
 * Google Fonts. Every style below derives from it; a weight without its own file is synthesized.
 */
private val AppFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Tabular figures, so totals don't shift sideways as their digits change. */
private const val TABULAR = "tnum"

private val base = Typography()

private fun TextStyle.styled(weight: FontWeight, tracking: Double = 0.0, tabular: Boolean = false) = copy(
    fontFamily = AppFont,
    fontWeight = weight,
    letterSpacing = tracking.em,
    fontFeatureSettings = if (tabular) TABULAR else null,
)

val AppTypography = Typography(
    displayLarge = base.displayLarge.styled(FontWeight.Bold, -0.03, tabular = true),
    displayMedium = base.displayMedium.styled(FontWeight.Bold, -0.03, tabular = true),
    displaySmall = base.displaySmall.styled(FontWeight.Bold, -0.025, tabular = true),
    headlineLarge = base.headlineLarge.styled(FontWeight.Bold, -0.02, tabular = true),
    headlineMedium = base.headlineMedium.styled(FontWeight.Bold, -0.02, tabular = true),
    headlineSmall = base.headlineSmall.styled(FontWeight.SemiBold, -0.015, tabular = true),
    titleLarge = base.titleLarge.styled(FontWeight.Bold, -0.01),
    titleMedium = base.titleMedium.styled(FontWeight.SemiBold, -0.005),
    titleSmall = base.titleSmall.styled(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.styled(FontWeight.Normal, 0.0),
    bodyMedium = base.bodyMedium.styled(FontWeight.Normal, 0.0),
    bodySmall = base.bodySmall.styled(FontWeight.Normal, 0.005),
    labelLarge = base.labelLarge.styled(FontWeight.SemiBold, 0.0),
    labelMedium = base.labelMedium.styled(FontWeight.Medium, 0.01),
    labelSmall = base.labelSmall.styled(FontWeight.Medium, 0.02),
)

/** The hero "kcal left" figure: bigger than any Material style. */
val HeroNumberStyle = TextStyle(
    fontFamily = AppFont,
    fontWeight = FontWeight.Bold,
    fontSize = 52.sp,
    lineHeight = 56.sp,
    letterSpacing = (-0.035).em,
    fontFeatureSettings = TABULAR,
)

/** Section titles above groups of cards ("Meals", "Habits"). */
val SectionTitleStyle = TextStyle(
    fontFamily = AppFont,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.01).em,
)
