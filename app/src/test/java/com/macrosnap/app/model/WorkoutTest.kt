package com.macrosnap.app.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTest {

    private val bench = Exercise(1, "Bench press", MuscleGroup.CHEST, ExerciseType.STRENGTH, isCustom = false)
    private val squat = Exercise(2, "Squat", MuscleGroup.LEGS, ExerciseType.STRENGTH, isCustom = false)

    private fun set(
        weightKg: Double = 0.0,
        reps: Int = 0,
        isDone: Boolean = true,
        durationSec: Int = 0,
        distanceM: Double = 0.0,
    ) = WorkoutSet(
        id = 0,
        exerciseId = 1,
        exerciseOrder = 0,
        setIndex = 0,
        weightKg = weightKg,
        reps = reps,
        durationSec = durationSec,
        distanceM = distanceM,
        isDone = isDone,
    )

    private fun record(weightKg: Double, reps: Int) = SetRecord(
        date = LocalDate.of(2026, 1, 1),
        workoutId = 1,
        weightKg = weightKg,
        reps = reps,
        durationSec = 0,
        distanceM = 0.0,
    )

    private fun workout(endedAt: Long?, startedAt: Long = 0, exercises: List<WorkoutExercise> = emptyList()) = Workout(
        id = 1,
        name = "Push",
        date = LocalDate.of(2026, 1, 1),
        startedAt = startedAt,
        endedAt = endedAt,
        exercises = exercises,
    )

    @Test
    fun `estimated 1RM follows Epley for multiple reps`() {
        assertEquals(100.0 * (1 + 10 / 30.0), set(100.0, 10).estimated1Rm, 1e-9)
        assertEquals(120.0, set(90.0, 10).estimated1Rm, 1e-9)
    }

    @Test
    fun `a single rep is the weight itself, not Epley's 103 percent`() {
        assertEquals(140.0, set(140.0, 1).estimated1Rm, 0.0)
    }

    @Test
    fun `no reps or no weight gives no 1RM`() {
        assertEquals(0.0, set(100.0, 0).estimated1Rm, 0.0)
        assertEquals(0.0, set(0.0, 12).estimated1Rm, 0.0)
        assertEquals(0.0, set(-20.0, 5).estimated1Rm, 0.0)
        assertEquals(0.0, set(60.0, -3).estimated1Rm, 0.0)
    }

    @Test
    fun `set record estimates 1RM exactly like a workout set`() {
        listOf(100.0 to 10, 140.0 to 1, 0.0 to 5, 80.0 to 0, 62.5 to 7).forEach { (weight, reps) ->
            assertEquals(set(weight, reps).estimated1Rm, record(weight, reps).estimated1Rm, 0.0)
        }
    }

    @Test
    fun `set volume is weight times reps`() {
        assertEquals(800.0, set(80.0, 10).volume, 0.0)
        assertEquals(0.0, set(0.0, 15).volume, 0.0)
    }

    @Test
    fun `a set has work when any of reps, time or distance is logged`() {
        assertFalse(set().hasWork)
        assertFalse(set(weightKg = 100.0).hasWork)
        assertTrue(set(reps = 1).hasWork)
        assertTrue(set(durationSec = 30).hasWork)
        assertTrue(set(distanceM = 0.5).hasWork)
    }

    @Test
    fun `workout without end time is active`() {
        assertTrue(workout(endedAt = null).isActive)
        assertFalse(workout(endedAt = 1_000).isActive)
    }

    @Test
    fun `duration of a finished workout counts whole minutes`() {
        assertEquals(45, workout(startedAt = 1_000, endedAt = 1_000 + 45 * 60_000L).durationMinutes)
        assertEquals(0, workout(startedAt = 0, endedAt = 59_999).durationMinutes)
        assertEquals(1, workout(startedAt = 0, endedAt = 60_000).durationMinutes)
    }

    @Test
    fun `duration of an active workout runs up to now`() {
        val startedAt = System.currentTimeMillis() - 10 * 60_000L
        val minutes = workout(startedAt = startedAt, endedAt = null).durationMinutes
        assertTrue("was $minutes", minutes in 10L..11L)
    }

    @Test
    fun `done sets and volume only count completed sets across exercises`() {
        val workout = workout(
            endedAt = 1,
            exercises = listOf(
                WorkoutExercise(bench, listOf(set(60.0, 10), set(70.0, 8), set(80.0, 6, isDone = false))),
                WorkoutExercise(squat, listOf(set(100.0, 5), set(0.0, 0, isDone = false))),
            ),
        )
        assertEquals(3, workout.doneSets)
        assertEquals(600.0 + 560.0 + 500.0, workout.volumeKg, 1e-9)
    }

    @Test
    fun `empty workout has no sets and no volume`() {
        val workout = workout(endedAt = 1, exercises = listOf(WorkoutExercise(bench, emptyList())))
        assertEquals(0, workout.doneSets)
        assertEquals(0.0, workout.volumeKg, 0.0)
    }

    @Test
    fun `exercise defaults sit inside the allowed ranges`() {
        assertEquals(Exercise.DEFAULT_SETS, bench.defaultSets)
        assertEquals(Exercise.DEFAULT_REPS, bench.defaultReps)
        assertTrue(Exercise.DEFAULT_SETS in Exercise.SETS_RANGE)
        assertTrue(Exercise.DEFAULT_REPS in Exercise.REPS_RANGE)
    }
}
