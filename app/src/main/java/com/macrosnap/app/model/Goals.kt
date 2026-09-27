package com.macrosnap.app.model

data class Goals(
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
) {
    companion object {
        val DEFAULT = Goals(kcal = 2200, protein = 140, carbs = 250, fat = 70)
    }
}
