package com.macrosnap.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.macrosnap.app.R
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Sex

// Display names for model enums. The model stays free of Android resources; these map each entry
// to a string so it follows the UI language.

val MealType.labelRes: Int
    @StringRes get() = when (this) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACK -> R.string.meal_snack
    }

val Sex.labelRes: Int
    @StringRes get() = when (this) {
        Sex.MALE -> R.string.sex_male
        Sex.FEMALE -> R.string.sex_female
    }

val ActivityLevel.labelRes: Int
    @StringRes get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary
        ActivityLevel.LIGHT -> R.string.activity_light
        ActivityLevel.MODERATE -> R.string.activity_moderate
        ActivityLevel.ACTIVE -> R.string.activity_active
        ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active
    }

val GoalType.labelRes: Int
    @StringRes get() = when (this) {
        GoalType.LOSE -> R.string.goal_lose
        GoalType.MAINTAIN -> R.string.goal_maintain
        GoalType.GAIN -> R.string.goal_gain
    }

val MuscleGroup.labelRes: Int
    @StringRes get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.LEGS -> R.string.muscle_legs
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CORE -> R.string.muscle_core
        MuscleGroup.CARDIO -> R.string.muscle_cardio
        MuscleGroup.OTHER -> R.string.muscle_other
    }

val Nutrient.labelRes: Int
    @StringRes get() = when (this) {
        Nutrient.VITAMIN_D -> R.string.nutrient_vitamin_d
        Nutrient.VITAMIN_B12 -> R.string.nutrient_vitamin_b12
        Nutrient.VITAMIN_B6 -> R.string.nutrient_vitamin_b6
        Nutrient.VITAMIN_B3 -> R.string.nutrient_vitamin_b3
        Nutrient.VITAMIN_B5 -> R.string.nutrient_vitamin_b5
        Nutrient.VITAMIN_C -> R.string.nutrient_vitamin_c
        Nutrient.VITAMIN_A -> R.string.nutrient_vitamin_a
        Nutrient.IRON -> R.string.nutrient_iron
        Nutrient.MAGNESIUM -> R.string.nutrient_magnesium
        Nutrient.ZINC -> R.string.nutrient_zinc
        Nutrient.CALCIUM -> R.string.nutrient_calcium
        Nutrient.POTASSIUM -> R.string.nutrient_potassium
        Nutrient.OMEGA_3 -> R.string.nutrient_omega_3
        Nutrient.CREATINE -> R.string.nutrient_creatine
        Nutrient.CAFFEINE -> R.string.nutrient_caffeine
    }

/** [Nutrient.unit] in the UI language ("mg" / "мг"). */
val Nutrient.unitRes: Int
    @StringRes get() = when (unit) {
        "µg" -> R.string.unit_ug
        "mg" -> R.string.unit_mg
        else -> R.string.unit_g
    }

val MealType.label: String @Composable @ReadOnlyComposable get() = stringResource(labelRes)
val Sex.label: String @Composable @ReadOnlyComposable get() = stringResource(labelRes)
val ActivityLevel.label: String @Composable @ReadOnlyComposable get() = stringResource(labelRes)
val GoalType.label: String @Composable @ReadOnlyComposable get() = stringResource(labelRes)
val MuscleGroup.label: String @Composable @ReadOnlyComposable get() = stringResource(labelRes)
val Nutrient.label: String @Composable @ReadOnlyComposable get() = stringResource(labelRes)
val Nutrient.unitLabel: String @Composable @ReadOnlyComposable get() = stringResource(unitRes)
