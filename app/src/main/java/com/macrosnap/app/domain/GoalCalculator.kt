package com.macrosnap.app.domain

import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Profile
import com.macrosnap.app.model.Sex
import kotlin.math.roundToInt

object GoalCalculator {

    private const val MIN_KCAL = 1200.0
    private const val FAT_SHARE_OF_KCAL = 0.25

    /** Mifflin-St Jeor BMR x activity multiplier, then adjusted for the chosen goal. */
    fun calculate(profile: Profile): Goals {
        val sexOffset = if (profile.sex == Sex.MALE) 5.0 else -161.0
        val bmr = 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * profile.ageYears + sexOffset
        val maintenance = bmr * profile.activity.multiplier
        val kcal = (maintenance * profile.goal.calorieFactor).coerceAtLeast(MIN_KCAL)

        val protein = profile.weightKg * profile.goal.proteinPerKg
        val fat = kcal * FAT_SHARE_OF_KCAL / 9.0
        val carbs = ((kcal - protein * 4.0 - fat * 9.0) / 4.0).coerceAtLeast(0.0)

        return Goals(
            kcal = (kcal / 10.0).roundToInt() * 10,
            protein = protein.roundToInt(),
            carbs = carbs.roundToInt(),
            fat = fat.roundToInt(),
        )
    }
}
