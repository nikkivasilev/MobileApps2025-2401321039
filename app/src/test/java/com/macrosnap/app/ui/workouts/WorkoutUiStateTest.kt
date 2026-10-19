package com.macrosnap.app.ui.workouts

import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutExercise
import com.macrosnap.app.model.WorkoutSet
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutUiStateTest {

    private val bench = Exercise(1, "Bench press", MuscleGroup.CHEST, ExerciseType.STRENGTH, isCustom = false)
    private val run = Exercise(2, "Treadmill run", MuscleGroup.CARDIO, ExerciseType.CARDIO, isCustom = false)

    private fun set(index: Int, done: Boolean, reps: Int = 8, durationSec: Int = 0, distanceM: Double = 0.0) =
        WorkoutSet(index.toLong(), 1, 0, index, 60.0, reps, durationSec, distanceM, done)

    private fun workout(sets: List<WorkoutSet>, minutes: Long = 30, ended: Boolean = false) = Workout(
        id = 1,
        name = "Push",
        date = LocalDate.of(2026, 10, 19),
        startedAt = 0,
        endedAt = if (ended) minutes * 60_000 else null,
        exercises = listOf(WorkoutExercise(bench, sets)),
    )

    @Test
    fun `no workout is neither active nor finishable and burns nothing`() {
        val state = WorkoutUiState()

        assertFalse(state.isActive)
        assertFalse(state.canFinish)
        assertEquals(0, state.burnKcal)
    }

    @Test
    fun `an active workout can be finished once a set is done`() {
        assertFalse(WorkoutUiState(workout = workout(listOf(set(0, done = false)))).canFinish)

        val state = WorkoutUiState(workout = workout(listOf(set(0, done = true), set(1, done = false))))
        assertTrue(state.isActive)
        assertTrue(state.canFinish)
        assertFalse(state.copy(isFinishing = true).canFinish)
    }

    @Test
    fun `a finished workout is no longer active`() {
        val state = WorkoutUiState(workout = workout(listOf(set(0, done = true)), ended = true))

        assertFalse(state.isActive)
        assertFalse(state.canFinish)
    }

    @Test
    fun `strength burn is capped at five minutes per completed set`() {
        // 3 sets cap the 30-minute session at 15 minutes: 3.5 MET x 80 kg x 0.25 h = 70 kcal.
        val sets = listOf(set(0, done = true), set(1, done = true), set(2, done = true), set(3, done = false))
        val state = WorkoutUiState(workout = workout(sets, minutes = 30, ended = true), bodyWeightKg = 80.0)

        assertEquals(70, state.burnKcal)
    }

    @Test
    fun `short strength sessions are credited only their real length`() {
        // 10 minutes is under the 3-set cap: 3.5 x 60 kg x 1/6 h = 35 kcal.
        val sets = listOf(set(0, done = true), set(1, done = true), set(2, done = true))
        val state = WorkoutUiState(workout = workout(sets, minutes = 10, ended = true), bodyWeightKg = 60.0)

        assertEquals(35, state.burnKcal)
    }

    @Test
    fun `running burn follows its speed`() {
        // 10 km/h for 30 minutes: MET 10 x 75 kg x 0.5 h = 375 kcal.
        val cardio = WorkoutSet(1, 2, 0, 0, 0.0, 0, durationSec = 1800, distanceM = 5000.0, isDone = true)
        val state = WorkoutUiState(
            workout = workout(emptyList(), ended = true).copy(exercises = listOf(WorkoutExercise(run, listOf(cardio)))),
        )

        assertEquals(375, state.burnKcal)
    }
}
