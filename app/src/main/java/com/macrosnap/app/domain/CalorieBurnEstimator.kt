package com.macrosnap.app.domain

import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.METERS_PER_KM
import com.macrosnap.app.model.MILLIS_PER_SECOND
import com.macrosnap.app.model.SECONDS_PER_HOUR
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutSet
import kotlin.math.roundToInt

/**
 * MET-based estimate: kcal = MET × body weight (kg) × hours. Resistance training is scored on
 * the whole session at 3.5 MET (the Compendium's "weight training, general", which already
 * accounts for rest between sets); cardio sets use their own MET and logged time. The session
 * time credited to strength is capped per completed set, so a workout left open for hours
 * doesn't turn into thousands of kcal.
 */
object CalorieBurnEstimator {

    const val DEFAULT_BODY_WEIGHT_KG = 75.0
    private const val STRENGTH_MET = 3.5

    /** A set plus the rest after it rarely runs past this; more session time than that was idle. */
    private const val MAX_SECONDS_PER_STRENGTH_SET = 5 * 60

    fun estimate(workout: Workout, bodyWeightKg: Double): Int {
        val weight = bodyWeightKg.takeIf { it > 0 } ?: DEFAULT_BODY_WEIGHT_KG
        var cardioSeconds = 0
        var kcal = 0.0
        var strengthSets = 0

        workout.exercises.forEach { entry ->
            entry.sets.filter { it.isDone && it.hasWork }.forEach { set ->
                when (entry.exercise.type) {
                    ExerciseType.STRENGTH -> strengthSets++
                    ExerciseType.CARDIO -> {
                        val seconds = set.durationSec.takeIf { it > 0 } ?: return@forEach
                        cardioSeconds += seconds
                        kcal += met(entry.exercise, set) * weight * seconds / SECONDS_PER_HOUR
                    }
                }
            }
        }
        if (strengthSets > 0) {
            val totalSeconds = ((workout.endedAt ?: System.currentTimeMillis()) - workout.startedAt) / MILLIS_PER_SECOND
            val strengthSeconds = (totalSeconds - cardioSeconds)
                .coerceIn(0.0, strengthSets * MAX_SECONDS_PER_STRENGTH_SET.toDouble())
            kcal += STRENGTH_MET * weight * strengthSeconds / SECONDS_PER_HOUR
        }
        return kcal.roundToInt()
    }

    // MET values from the Compendium of Physical Activities.
    @Suppress("MagicNumber")
    private fun met(exercise: Exercise, set: WorkoutSet): Double {
        val name = exercise.name.lowercase()
        val kmh = if (set.distanceM > 0 && set.durationSec > 0) {
            set.distanceM / METERS_PER_KM / (set.durationSec / SECONDS_PER_HOUR)
        } else {
            null
        }
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
