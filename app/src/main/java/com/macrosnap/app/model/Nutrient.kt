package com.macrosnap.app.model

/**
 * Micronutrients the app tracks. RDA defaults are adult values (NIH/NASEM); [rdaMale]/[rdaFemale]
 * pick by profile sex. [offKey]/[offScale] map Open Food Facts' `<key>_100g` fields, which are
 * stored in grams, onto [unit].
 */
enum class Nutrient(
    val label: String,
    val unit: String,
    val schemaKey: String,
    val offKey: String?,
    val offScale: Double,
    val rdaMale: Double,
    val rdaFemale: Double,
    /** A ceiling rather than a goal: going over is the bad direction. */
    val isLimit: Boolean = false,
) {
    VITAMIN_D("Vitamin D", "µg", "vitamin_d_ug", "vitamin-d", 1e6, 15.0, 15.0),
    VITAMIN_B12("Vitamin B12", "µg", "vitamin_b12_ug", "vitamin-b12", 1e6, 2.4, 2.4),
    VITAMIN_B6("Vitamin B6", "mg", "vitamin_b6_mg", "vitamin-b6", 1e3, 1.3, 1.3),
    VITAMIN_C("Vitamin C", "mg", "vitamin_c_mg", "vitamin-c", 1e3, 90.0, 75.0),
    VITAMIN_A("Vitamin A", "µg", "vitamin_a_ug", "vitamin-a", 1e6, 900.0, 700.0),
    IRON("Iron", "mg", "iron_mg", "iron", 1e3, 8.0, 18.0),
    MAGNESIUM("Magnesium", "mg", "magnesium_mg", "magnesium", 1e3, 420.0, 320.0),
    ZINC("Zinc", "mg", "zinc_mg", "zinc", 1e3, 11.0, 8.0),
    CALCIUM("Calcium", "mg", "calcium_mg", "calcium", 1e3, 1000.0, 1000.0),
    POTASSIUM("Potassium", "mg", "potassium_mg", "potassium", 1e3, 3400.0, 2600.0),
    OMEGA_3("Omega-3", "g", "omega3_g", "omega-3-fat", 1.0, 1.6, 1.1),
    CREATINE("Creatine", "g", "creatine_g", null, 1.0, 5.0, 5.0),
    CAFFEINE("Caffeine", "mg", "caffeine_mg", "caffeine", 1e3, 400.0, 400.0, isLimit = true),
    ;

    fun rda(sex: Sex?): Double = if (sex == Sex.FEMALE) rdaFemale else rdaMale

    companion object {
        fun defaultTargets(sex: Sex?): Micros = entries.associateWith { it.rda(sex) }
    }
}

/** Amounts per nutrient in that nutrient's [Nutrient.unit]. Absent keys mean "unknown", not zero. */
typealias Micros = Map<Nutrient, Double>

fun Micros.scaled(factor: Double): Micros = mapValues { it.value * factor }

fun Micros.add(other: Micros): Micros {
    if (other.isEmpty()) return this
    if (isEmpty()) return other
    val result = toMutableMap()
    other.forEach { (nutrient, amount) -> result[nutrient] = (result[nutrient] ?: 0.0) + amount }
    return result
}

fun Iterable<Micros>.sumMicros(): Micros = fold(emptyMap(), Micros::add)
