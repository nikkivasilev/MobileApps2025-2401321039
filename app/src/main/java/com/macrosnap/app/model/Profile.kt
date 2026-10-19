package com.macrosnap.app.model

data class Profile(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activity: ActivityLevel,
    val goal: GoalType,
)

enum class Sex {
    MALE,
    FEMALE,
}

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
}

enum class GoalType(
    val calorieFactor: Double,
    val proteinPerKg: Double,
) {
    LOSE(0.80, 2.0),
    MAINTAIN(1.00, 1.8),
    GAIN(1.10, 1.8),
}
