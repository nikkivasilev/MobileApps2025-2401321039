package com.macrosnap.app.domain

import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutSet
import kotlin.math.roundToInt

/**
 * MET-based estimate: kcal = MET × body weight (kg) × hours. Resistance training is scored on
 * the whole session at 3.5 MET (the Compendium's "weight training, general", which already
 * accounts for rest between sets); cardio sets use their own MET and logged time.
 */
object CalorieBurnEstimator {

    const val DEFAULT_BODY_WEIGHT_KG = 75.0
    private const val STRENGTH_MET = 3.5

    fun estimate(workout: Workout, bodyWeightKg: Double): Int {
        val weight = bodyWeightKg.takeIf { it > 0 } ?: DEFAULT_BODY_WEIGHT_KG
        var cardioSeconds = 0
        var kcal = 0.0
        var hasStrength = false

        workout.exercises.forEach { entry ->
            entry.sets.filter { it.isDone && it.hasWork }.forEach { set ->
                when (entry.exercise.type) {
                    ExerciseType.STRENGTH -> hasStrength = true
                    ExerciseType.CARDIO -> {
                        val seconds = set.durationSec.takeIf { it > 0 } ?: return@forEach
                        cardioSeconds += seconds
                        kcal += met(entry.exercise, set) * weight * seconds / 3600.0
                    }
                }
            }
        }
        if (hasStrength) {
            val totalSeconds = ((workout.endedAt ?: System.currentTimeMillis()) - workout.startedAt) / 1000.0
            val strengthSeconds = (totalSeconds - cardioSeconds).coerceAtLeast(0.0)
            kcal += STRENGTH_MET * weight * strengthSeconds / 3600.0
        }
        return kcal.roundToInt()
    }

    private fun met(exercise: Exercise, set: WorkoutSet): Double {
        val name = exercise.name.lowercase()
        val kmh = if (set.distanceM > 0 && set.durationSec > 0) set.distanceM / 1000.0 / (set.durationSec / 3600.0) else null
        return when {
            "run" in name || "jog" in name || "treadmill" in name ->
                // Running MET tracks speed closely (8 km/h ≈ 8.3, 12 km/h ≈ 11.5).
                kmh?.coerceIn(5.0, 18.0) ?: 8.5
            "walk" in name -> if (kmh != null && kmh >= 5.5) 4.3 else 3.5
            "cycl" in name || "bike" in name -> if (kmh != null && kmh >= 22) 8.0 else 6.0
            "row" in name -> 7.0
            "elliptical" in name -> 5.0
            "stair" in name -> 9.0
            "jump rope" in name || "skipping" in name -> 11.0
            "swim" in name -> 7.0
            else -> 6.0
        }
    }
}
