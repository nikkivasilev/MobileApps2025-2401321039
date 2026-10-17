package com.macrosnap.app.model

/**
 * Micronutrients the app tracks (display names live in `ui/Labels.kt`). RDA defaults are adult
 * values (NIH/NASEM); [rdaMale]/[rdaFemale] pick by profile sex. [offKey]/[offScale] map Open
 * Food Facts' `<key>_100g` fields, which are stored in grams, onto [unit].
 */
enum class Nutrient(
    val unit: String,
    val schemaKey: String,
    val offKey: String?,
    val offScale: Double,
    val rdaMale: Double,
    val rdaFemale: Double,
    /** A ceiling rather than a goal: going over is the bad direction. */
    val isLimit: Boolean = false,
) {
    VITAMIN_D("µg", "vitamin_d_ug", "vitamin-d", 1e6, 15.0, 15.0),
    VITAMIN_B12("µg", "vitamin_b12_ug", "vitamin-b12", 1e6, 2.4, 2.4),
    VITAMIN_B6("mg", "vitamin_b6_mg", "vitamin-b6", 1e3, 1.3, 1.3),
    // Open Food Facts files niacin under its old name, vitamin PP.
    VITAMIN_B3("mg", "vitamin_b3_mg", "vitamin-pp", 1e3, 16.0, 14.0),
    VITAMIN_B5("mg", "vitamin_b5_mg", "pantothenic-acid", 1e3, 5.0, 5.0),
    VITAMIN_C("mg", "vitamin_c_mg", "vitamin-c", 1e3, 90.0, 75.0),
    VITAMIN_A("µg", "vitamin_a_ug", "vitamin-a", 1e6, 900.0, 700.0),
    IRON("mg", "iron_mg", "iron", 1e3, 8.0, 18.0),
    MAGNESIUM("mg", "magnesium_mg", "magnesium", 1e3, 420.0, 320.0),
    ZINC("mg", "zinc_mg", "zinc", 1e3, 11.0, 8.0),
    CALCIUM("mg", "calcium_mg", "calcium", 1e3, 1000.0, 1000.0),
    POTASSIUM("mg", "potassium_mg", "potassium", 1e3, 3400.0, 2600.0),
    OMEGA_3("g", "omega3_g", "omega-3-fat", 1.0, 1.6, 1.1),
    CREATINE("g", "creatine_g", null, 1.0, 5.0, 5.0),
    CAFFEINE("mg", "caffeine_mg", "caffeine", 1e3, 400.0, 400.0, isLimit = true),
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
