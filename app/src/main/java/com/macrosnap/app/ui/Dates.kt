package com.macrosnap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** Date patterns shared across screens; month and day names come from the UI language. */
object DatePatterns {
    /** "Sat, 17 Oct" / "сб, 17 окт". */
    const val SHORT = "EEE, d MMM"

    /** "Saturday, 17 October" / "Събота, 17 октомври". */
    const val FULL = "EEEE, d MMMM"
    const val TIME = "HH:mm"
}

/** The locale the UI is showing right now, including an in-app language choice. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Formats in the UI language and capitalizes the first letter, which Bulgarian day names lack. */
@Composable
@ReadOnlyComposable
fun TemporalAccessor.formatted(pattern: String): String {
    val locale = currentLocale()
    return DateTimeFormatter.ofPattern(pattern, locale).format(this).replaceFirstChar { it.titlecase(locale) }
}
