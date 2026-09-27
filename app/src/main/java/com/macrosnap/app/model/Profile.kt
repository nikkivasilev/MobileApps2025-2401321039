package com.macrosnap.app.model

data class Profile(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activity: ActivityLevel,
    val goal: GoalType,
)

enum class Sex(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
}

enum class ActivityLevel(val label: String, val multiplier: Double) {
    SEDENTARY("Sedentary", 1.2),
    LIGHT("Light (1-3 days/wk)", 1.375),
    MODERATE("Moderate (3-5 days/wk)", 1.55),
    ACTIVE("Active (6-7 days/wk)", 1.725),
    VERY_ACTIVE("Very active / physical job", 1.9),
}

enum class GoalType(val label: String, val calorieFactor: Double, val proteinPerKg: Double) {
    LOSE("Lose fat", 0.80, 2.0),
    MAINTAIN("Maintain", 1.00, 1.8),
    GAIN("Build muscle", 1.10, 1.8),
}
