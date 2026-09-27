package com.macrosnap.app.model

/**
 * A dish built from ingredients. [cookedWeightGrams] is what the finished dish weighs: cooking
 * drives off water, so per-100 g values based on raw ingredient weight would be wrong.
 */
data class Recipe(
    val id: Long,
    val name: String,
    val ingredients: List<FoodItem>,
    val cookedWeightGrams: Double?,
    val servings: Int,
) {
    val ingredientWeight: Double get() = ingredients.sumOf { it.grams }
    val totalWeight: Double get() = cookedWeightGrams ?: ingredientWeight
    val total: Macros get() = ingredients.map { it.total }.sum()
    val totalMicros: Micros get() = ingredients.map { it.totalMicros }.sumMicros()
    val per100g: Macros get() = if (totalWeight > 0) total * (100.0 / totalWeight) else Macros.ZERO
    val micros100g: Micros get() = if (totalWeight > 0) totalMicros.scaled(100.0 / totalWeight) else emptyMap()
    val servingGrams: Double get() = if (servings > 0) totalWeight / servings else totalWeight
    val perServing: Macros get() = if (servings > 0) total * (1.0 / servings) else total

    fun portion(grams: Double) = FoodItem(name, grams, per100g, micros100g)
}
