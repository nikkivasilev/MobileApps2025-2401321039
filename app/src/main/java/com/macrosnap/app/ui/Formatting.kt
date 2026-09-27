package com.macrosnap.app.ui

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

fun Macros.summary(): String = "P ${protein.whole()} · C ${carbs.whole()} · F ${fat.whole()} g"

fun DataError.userMessage(): String = when (this) {
    DataError.NoApiKey -> "Add your free Gemini API key in Settings to analyze photos."
    DataError.InvalidApiKey -> "The Gemini API key was rejected. Check it in Settings."
    DataError.Network -> "No connection. Check your internet and try again."
    DataError.RateLimited -> "Today's free quota is used up on every configured model. Try again later or log this one manually."
    DataError.ModelUnavailable -> "None of the configured Gemini models are available. Check the model list in Settings."
    DataError.Server -> "Gemini is having trouble right now. Try again in a moment."
    DataError.BadImage -> "That photo couldn't be read. Try taking it again."
    DataError.NotFood -> "No food was recognized. Try a different photo or wording, or add the items manually."
    DataError.ProductNotFound -> "That barcode isn't in Open Food Facts. Add the item manually instead."
    is DataError.Unexpected -> detail?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."
}
