package com.macrosnap.app.ui.workouts

import com.macrosnap.app.domain.CalorieBurnEstimator
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.PersonalRecord
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutSet
import com.macrosnap.app.ui.UiText

/** What the user has typed for one set; parsed on every change and written through to the DB. */
data class SetInputs(
    val weight: String = "",
    val reps: String = "",
    val minutes: String = "",
    val km: String = "",
)

data class WorkoutUiState(
    val workout: Workout? = null,
    val inputs: Map<Long, SetInputs> = emptyMap(),
    val lastSets: Map<Long, List<WorkoutSet>> = emptyMap(),
    val exercises: List<Exercise> = emptyList(),
    val restSeconds: Int = 90,
    val bodyWeightKg: Double = 75.0,

    val restEndsAt: Long? = null,
    val showPicker: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val summary: WorkoutSummary? = null,
    val isFinishing: Boolean = false,
) {
    val isActive: Boolean get() = workout?.isActive == true
    val canFinish: Boolean get() = isActive && (workout?.doneSets ?: 0) > 0 && !isFinishing
    val burnKcal: Int get() = workout?.let { CalorieBurnEstimator.estimate(it, bodyWeightKg) } ?: 0
}

data class WorkoutSummary(
    val durationMinutes: Long,
    val sets: Int,
    val volumeKg: Double,
    val burnKcal: Int,
    val records: List<PersonalRecord>,
)

sealed interface WorkoutEffect {
    data object Close : WorkoutEffect
    data class ShowMessage(val message: UiText) : WorkoutEffect
}
