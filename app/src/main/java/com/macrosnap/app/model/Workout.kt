package com.macrosnap.app.model

import java.time.LocalDate

enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    LEGS("Legs"),
    GLUTES("Glutes"),
    CORE("Core"),
    CARDIO("Cardio"),
    OTHER("Other"),
}

enum class ExerciseType { STRENGTH, CARDIO }

data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: MuscleGroup,
    val type: ExerciseType,
    val isCustom: Boolean,
)

data class WorkoutSet(
    val id: Long,
    val exerciseId: Long,
    val exerciseOrder: Int,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val durationSec: Int,
    val distanceM: Double,
    val isDone: Boolean,
) {
    /** Epley estimate; a single rep is just the weight itself. */
    val estimated1Rm: Double
        get() = when {
            reps <= 0 || weightKg <= 0 -> 0.0
            reps == 1 -> weightKg
            else -> weightKg * (1 + reps / 30.0)
        }
    val volume: Double get() = weightKg * reps
    val hasWork: Boolean get() = reps > 0 || durationSec > 0 || distanceM > 0
}

data class WorkoutExercise(
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
)

data class Workout(
    val id: Long,
    val name: String,
    val date: LocalDate,
    val startedAt: Long,
    val endedAt: Long?,
    val exercises: List<WorkoutExercise>,
) {
    val isActive: Boolean get() = endedAt == null
    val durationMinutes: Long get() = ((endedAt ?: System.currentTimeMillis()) - startedAt) / 60_000
    val doneSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.isDone } }
    val volumeKg: Double get() = exercises.sumOf { ex -> ex.sets.filter { it.isDone }.sumOf { it.volume } }
}

data class RoutineExercise(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: Int,
)

data class Routine(
    val id: Long,
    val name: String,
    val exercises: List<RoutineExercise>,
)

/** One completed set with the day it happened, for progress charts and PR checks. */
data class SetRecord(
    val date: LocalDate,
    val workoutId: Long,
    val weightKg: Double,
    val reps: Int,
    val durationSec: Int,
    val distanceM: Double,
) {
    val estimated1Rm: Double
        get() = when {
            reps <= 0 || weightKg <= 0 -> 0.0
            reps == 1 -> weightKg
            else -> weightKg * (1 + reps / 30.0)
        }
}

data class PersonalRecord(
    val exercise: Exercise,
    val description: String,
)
