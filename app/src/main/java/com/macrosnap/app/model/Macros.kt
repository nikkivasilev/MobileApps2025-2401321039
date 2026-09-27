package com.macrosnap.app.model

data class Macros(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    operator fun plus(other: Macros) = Macros(
        kcal = kcal + other.kcal,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
    )

    operator fun times(factor: Double) = Macros(
        kcal = kcal * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor,
    )

    companion object {
        val ZERO = Macros(0.0, 0.0, 0.0, 0.0)
    }
}

fun Iterable<Macros>.sum(): Macros = fold(Macros.ZERO, Macros::plus)
