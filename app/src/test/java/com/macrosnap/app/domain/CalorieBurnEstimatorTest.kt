package com.macrosnap.app.domain

import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutExercise
import com.macrosnap.app.model.WorkoutSet
import java.time.LocalDate
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieBurnEstimatorTest {

    private val minute = 60_000L

    private fun strength(name: String = "Bench press") =
        Exercise(1, name, MuscleGroup.CHEST, ExerciseType.STRENGTH, isCustom = false)

    private fun cardio(name: String) = Exercise(2, name, MuscleGroup.CARDIO, ExerciseType.CARDIO, isCustom = false)

    private fun set(
        reps: Int = 0,
        weightKg: Double = 0.0,
        durationSec: Int = 0,
        distanceM: Double = 0.0,
        isDone: Boolean = true,
    ) = WorkoutSet(
        id = 0,
        exerciseId = 0,
        exerciseOrder = 0,
        setIndex = 0,
        weightKg = weightKg,
        reps = reps,
        durationSec = durationSec,
        distanceM = distanceM,
        isDone = isDone,
    )

    private fun liftingSets(count: Int) = List(count) { set(reps = 8, weightKg = 60.0) }

    private fun workout(minutes: Long, vararg exercises: WorkoutExercise) = Workout(
        id = 1,
        name = "Session",
        date = LocalDate.of(2026, 1, 1),
        startedAt = 0,
        endedAt = minutes * minute,
        exercises = exercises.toList(),
    )

    private fun kcal(met: Double, weightKg: Double, seconds: Int) = (met * weightKg * seconds / 3600.0).roundToInt()

    private fun cardioKcal(name: String, durationSec: Int, distanceM: Double = 0.0, weightKg: Double = 80.0) =
        CalorieBurnEstimator.estimate(
            workout(0, WorkoutExercise(cardio(name), listOf(set(durationSec = durationSec, distanceM = distanceM)))),
            weightKg,
        )

    @Test
    fun `an empty workout burns nothing`() {
        assertEquals(0, CalorieBurnEstimator.estimate(workout(60), 80.0))
    }

    @Test
    fun `strength is scored on session time at the weight-training MET`() {
        // 12 sets allow up to 60 min; the session took 45.
        val workout = workout(45, WorkoutExercise(strength(), liftingSets(12)))
        assertEquals(kcal(3.5, 80.0, 45 * 60), CalorieBurnEstimator.estimate(workout, 80.0))
    }

    @Test
    fun `a workout left open is capped at five minutes per completed set`() {
        val workout = workout(240, WorkoutExercise(strength(), liftingSets(3)))
        assertEquals(kcal(3.5, 80.0, 15 * 60), CalorieBurnEstimator.estimate(workout, 80.0))
    }

    @Test
    fun `unfinished or empty sets earn nothing`() {
        val sets = listOf(set(reps = 8, weightKg = 60.0, isDone = false), set(weightKg = 60.0))
        assertEquals(0, CalorieBurnEstimator.estimate(workout(60, WorkoutExercise(strength(), sets)), 80.0))
    }

    @Test
    fun `a timed hold like a plank counts as a strength set`() {
        val workout = workout(30, WorkoutExercise(strength("Plank"), listOf(set(durationSec = 60))))
        assertEquals(kcal(3.5, 80.0, 5 * 60), CalorieBurnEstimator.estimate(workout, 80.0))
    }

    @Test
    fun `missing or non-positive body weight falls back to 75 kg`() {
        val workout = workout(30, WorkoutExercise(strength(), liftingSets(10)))
        val expected = kcal(3.5, CalorieBurnEstimator.DEFAULT_BODY_WEIGHT_KG, 30 * 60)
        assertEquals(expected, CalorieBurnEstimator.estimate(workout, 0.0))
        assertEquals(expected, CalorieBurnEstimator.estimate(workout, -5.0))
    }

    @Test
    fun `burn scales with body weight`() {
        val workout = workout(30, WorkoutExercise(strength(), liftingSets(10)))
        assertEquals(
            2 * CalorieBurnEstimator.estimate(workout, 60.0),
            CalorieBurnEstimator.estimate(workout, 120.0),
        )
    }

    @Test
    fun `cardio time is taken out of the strength time`() {
        // 60 min session, 20 min run without distance (8.5 MET), 10 lifting sets get the remaining 40 min.
        val workout = workout(
            60,
            WorkoutExercise(strength(), liftingSets(10)),
            WorkoutExercise(cardio("Treadmill run"), listOf(set(durationSec = 20 * 60))),
        )
        val expected = (8.5 * 80 * 20 / 60.0 + 3.5 * 80 * 40 / 60.0).roundToInt()
        assertEquals(expected, CalorieBurnEstimator.estimate(workout, 80.0))
    }

    @Test
    fun `cardio logged longer than the session leaves no strength time`() {
        val workout = workout(
            10,
            WorkoutExercise(strength(), liftingSets(4)),
            WorkoutExercise(cardio("Swimming"), listOf(set(durationSec = 30 * 60))),
        )
        assertEquals(kcal(7.0, 80.0, 30 * 60), CalorieBurnEstimator.estimate(workout, 80.0))
    }

    @Test
    fun `cardio without a duration is skipped`() {
        assertEquals(0, cardioKcal("Outdoor run", durationSec = 0, distanceM = 5_000.0))
    }

    @Test
    fun `running MET follows speed`() {
        // 10 km in 1 h = 10 km/h -> MET 10.
        assertEquals(kcal(10.0, 80.0, 3600), cardioKcal("Outdoor run", 3600, 10_000.0))
        assertEquals(kcal(8.5, 80.0, 1800), cardioKcal("Jogging", 1800))
    }

    @Test
    fun `running MET is clamped between 5 and 18`() {
        // 2 km/h and 30 km/h.
        assertEquals(kcal(5.0, 80.0, 3600), cardioKcal("Treadmill run", 3600, 2_000.0))
        assertEquals(kcal(18.0, 80.0, 3600), cardioKcal("Treadmill run", 3600, 30_000.0))
    }

    @Test
    fun `brisk walking scores higher than a stroll`() {
        assertEquals(kcal(3.5, 80.0, 3600), cardioKcal("Walk", 3600))
        assertEquals(kcal(3.5, 80.0, 3600), cardioKcal("Walk", 3600, 5_000.0))
        assertEquals(kcal(4.3, 80.0, 3600), cardioKcal("Walk", 3600, 5_500.0))
    }

    @Test
    fun `fast cycling scores higher than easy cycling`() {
        assertEquals(kcal(6.0, 80.0, 3600), cardioKcal("Cycling", 3600, 20_000.0))
        assertEquals(kcal(8.0, 80.0, 3600), cardioKcal("Cycling", 3600, 22_000.0))
        assertEquals(kcal(6.0, 80.0, 3600), cardioKcal("Stationary bike", 3600))
    }

    @Test
    fun `other cardio uses its fixed MET by name`() {
        val expected = mapOf(
            "Rowing machine" to 7.0,
            "Elliptical" to 5.0,
            "Stair climber" to 9.0,
            "Jump rope" to 11.0,
            "Skipping" to 11.0,
            "Swimming" to 7.0,
            "Zumba" to 6.0,
        )
        expected.forEach { (name, met) ->
            assertEquals(name, kcal(met, 80.0, 1800), cardioKcal(name, 1800))
        }
    }

    @Test
    fun `cardio sets add up`() {
        val sets = listOf(set(durationSec = 600), set(durationSec = 900), set(durationSec = 300, isDone = false))
        val workout = workout(0, WorkoutExercise(cardio("Elliptical"), sets))
        assertEquals(kcal(5.0, 80.0, 1500), CalorieBurnEstimator.estimate(workout, 80.0))
    }
}
