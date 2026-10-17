package com.macrosnap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.macrosnap.app.R
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.Macros
import java.util.Locale
import kotlin.math.roundToInt

fun Double.whole(): String = roundToInt().toString()

/** One decimal, dropped when it's zero: 12.0 -> "12", 12.34 -> "12.3". */
fun Double.compact(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded)
}

/** Accepts both "12.5" and "12,5" — many locales' numeric keyboards only offer a comma. */
fun String.toAmountOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

@Composable
fun Macros.summary(): String = stringResource(R.string.macros_summary, protein.whole(), carbs.whole(), fat.whole())

fun DataError.userText(): UiText = when (this) {
    DataError.NoApiKey -> uiText(R.string.error_no_api_key)
    DataError.InvalidApiKey -> uiText(R.string.error_invalid_api_key)
    DataError.Network -> uiText(R.string.error_network)
    is DataError.RateLimited -> uiText(if (daily) R.string.error_rate_limited_daily else R.string.error_rate_limited)
    DataError.ModelUnavailable -> uiText(R.string.error_model_unavailable)
    DataError.Server -> uiText(R.string.error_server)
    DataError.BadImage -> uiText(R.string.error_bad_image)
    DataError.NotFood -> uiText(R.string.error_not_food)
    DataError.ProductNotFound -> uiText(R.string.error_product_not_found)
    is DataError.Unexpected -> detail?.takeIf { it.isNotBlank() }?.let(UiText::Raw) ?: uiText(R.string.error_unexpected)
}

@Composable
fun DataError.userMessage(): String = userText().asString()
