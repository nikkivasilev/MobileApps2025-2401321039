package com.macrosnap.app.model

/**
 * Nutrition is stored per 100 g so that correcting the portion weight — the part a photo
 * estimate gets wrong most often — rescales every value consistently.
 */
data class FoodItem(
    val name: String,
    val grams: Double,
    val per100g: Macros,
    val micros100g: Micros = emptyMap(),
) {
    val total: Macros get() = per100g * (grams / 100.0)
    val totalMicros: Micros get() = micros100g.scaled(grams / 100.0)
}
