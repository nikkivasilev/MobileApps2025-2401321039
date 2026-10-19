package com.macrosnap.app.ui.settings

import com.macrosnap.app.data.settings.AppSettings
import com.macrosnap.app.domain.GoalCalculator
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Profile
import com.macrosnap.app.model.Sex
import com.macrosnap.app.model.Supplement
import com.macrosnap.app.ui.UiText
import com.macrosnap.app.ui.components.toMicros
import com.macrosnap.app.ui.toAmountOrNull

data class SettingsUiState(
    val apiKeyInput: String = "",
    val modelsInput: String = "",
    val kcalInput: String = "",
    val proteinInput: String = "",
    val carbsInput: String = "",
    val fatInput: String = "",
    val sex: Sex = Sex.MALE,
    val ageInput: String = "",
    val heightInput: String = "",
    val weightInput: String = "",
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goalType: GoalType = GoalType.MAINTAIN,
    val nutrientInputs: Map<Nutrient, String> = emptyMap(),
    val waterGoalInput: String = "",
    val sleepGoalInput: String = "",
    val restSecondsInput: String = "",

    val saved: AppSettings? = null,
    val supplements: List<Supplement> = emptyList(),

    val keyCheck: KeyCheckState = KeyCheckState.Idle,
) {
    val models: List<String> get() = modelsInput.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    val isConnectionDirty: Boolean
        get() = saved != null && (apiKeyInput.trim() != saved.apiKey || models != saved.models)

    val enteredGoals: Goals?
        get() {
            return Goals(
                kcal = kcalInput.trim().toIntOrNull()?.takeIf { it in 500..10_000 } ?: return null,
                protein = proteinInput.trim().toIntOrNull()?.takeIf { it in 0..1000 } ?: return null,
                carbs = carbsInput.trim().toIntOrNull()?.takeIf { it in 0..2000 } ?: return null,
                fat = fatInput.trim().toIntOrNull()?.takeIf { it in 0..1000 } ?: return null,
            )
        }

    val isGoalsDirty: Boolean get() = enteredGoals != null && enteredGoals != saved?.goals

    val enteredProfile: Profile?
        get() {
            return Profile(
                sex = sex,
                ageYears = ageInput.trim().toIntOrNull()?.takeIf { it in 13..100 } ?: return null,
                heightCm = heightInput.toAmountOrNull()?.takeIf { it in 100.0..250.0 } ?: return null,
                weightKg = weightInput.toAmountOrNull()?.takeIf { it in 30.0..300.0 } ?: return null,
                activity = activity,
                goal = goalType,
            )
        }

    val suggestedGoals: Goals? get() = enteredProfile?.let(GoalCalculator::calculate)

    /** Every field must parse; a blank one would silently drop that nutrient's target. */
    val enteredNutrientTargets: Micros?
        get() = nutrientInputs.toMicros().takeIf { it.size == Nutrient.entries.size }

    val isNutrientTargetsDirty: Boolean
        get() = enteredNutrientTargets != null && enteredNutrientTargets != saved?.nutrientTargets

    val waterGoal: Int? get() = waterGoalInput.trim().toIntOrNull()?.takeIf { it in 500..10_000 }
    val sleepGoal: Double? get() = sleepGoalInput.toAmountOrNull()?.takeIf { it in 4.0..14.0 }
    val restSeconds: Int? get() = restSecondsInput.trim().toIntOrNull()?.takeIf { it in 15..600 }
    val isWellnessDirty: Boolean
        get() = waterGoal != null &&
            sleepGoal != null &&
            restSeconds != null &&
            (waterGoal != saved?.waterGoalMl || sleepGoal != saved?.sleepGoalHours || restSeconds != saved?.restSeconds)
}

sealed interface KeyCheckState {
    data object Idle : KeyCheckState
    data object Checking : KeyCheckState
    data class Valid(val unavailableModels: List<String>) : KeyCheckState
    data class Failed(val error: DataError) : KeyCheckState
}

sealed interface SettingsEffect {
    data class ShowMessage(val message: UiText) : SettingsEffect
}
