package com.macrosnap.app.data

import com.macrosnap.app.data.local.ExerciseEntity
import com.macrosnap.app.data.local.RoutineEntity
import com.macrosnap.app.data.local.RoutineExerciseEntity
import com.macrosnap.app.data.local.RoutineWithExercises
import com.macrosnap.app.data.local.SetRecordRow
import com.macrosnap.app.data.local.WorkoutDao
import com.macrosnap.app.data.local.WorkoutEntity
import com.macrosnap.app.data.local.WorkoutSetEntity
import com.macrosnap.app.data.local.WorkoutWithSets
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.model.PersonalRecord
import com.macrosnap.app.model.Routine
import com.macrosnap.app.model.RoutineExercise
import com.macrosnap.app.model.SetRecord
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutSet
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutRepositoryTest {

    /** In-memory tables answering the queries the way the SQL in [WorkoutDao] does. */
    private class FakeWorkoutDao : WorkoutDao {
        val exercises = mutableListOf<ExerciseEntity>()
        val routines = mutableListOf<RoutineEntity>()
        val routineExercises = mutableListOf<RoutineExerciseEntity>()
        val workouts = mutableListOf<WorkoutEntity>()
        val sets = mutableListOf<WorkoutSetEntity>()
        var exerciseUpdates = 0
        private var nextId = 1L

        private fun <T> query(block: () -> T): Flow<T> = flow { emit(block()) }

        private fun withSets(workout: WorkoutEntity) =
            WorkoutWithSets(workout, sets.filter { it.workoutId == workout.id })

        private fun withExercises(routine: RoutineEntity) =
            RoutineWithExercises(routine, routineExercises.filter { it.routineId == routine.id })

        private fun finishedWorkoutsWith(exerciseId: Long, excludingWorkoutId: Long) = workouts
            .filter { it.endedAt != null && it.id != excludingWorkoutId }
            .filter { w -> sets.any { it.workoutId == w.id && it.exerciseId == exerciseId && it.isDone } }
            .sortedBy { it.startedAt }

        override fun observeExercises() = query { exercises.sortedBy { it.name.lowercase() } }
        override suspend fun getExercises() = exercises.toList()
        override suspend fun countExercises() = exercises.size
        override suspend fun insertExercises(exercises: List<ExerciseEntity>) {
            exercises.forEach { insertExercise(it) }
        }
        override suspend fun insertExercise(exercise: ExerciseEntity): Long {
            val id = nextId++
            exercises += exercise.copy(id = id)
            return id
        }
        override suspend fun updateExercise(exercise: ExerciseEntity) {
            exerciseUpdates++
            exercises.replaceAll { if (it.id == exercise.id) exercise else it }
        }
        override suspend fun deleteExercise(id: Long) {
            exercises.removeAll { it.id == id }
        }
        override suspend fun countWorkoutsUsing(exerciseId: Long) =
            sets.filter { it.exerciseId == exerciseId }.map { it.workoutId }.distinct().size
        override suspend fun countRoutinesUsing(exerciseId: Long) =
            routineExercises.filter { it.exerciseId == exerciseId }.map { it.routineId }.distinct().size

        override fun observeRoutines() = query { routines.sortedBy { it.name.lowercase() }.map(::withExercises) }
        override suspend fun getRoutine(id: Long) = routines.firstOrNull { it.id == id }?.let(::withExercises)
        override suspend fun upsertRoutine(routine: RoutineEntity): Long {
            if (routines.any { it.id == routine.id }) {
                routines.replaceAll { if (it.id == routine.id) routine else it }
                return -1 // What Room's @Upsert returns for an update.
            }
            val id = nextId++
            routines += routine.copy(id = id)
            return id
        }
        override suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>) {
            exercises.forEach { routineExercises += it.copy(id = nextId++) }
        }
        override suspend fun deleteRoutineExercises(routineId: Long) {
            routineExercises.removeAll { it.routineId == routineId }
        }
        override suspend fun deleteRoutine(id: Long) {
            routines.removeAll { it.id == id }
        }

        override fun observeRecentWorkouts(limit: Int) =
            query { workouts.sortedByDescending { it.startedAt }.take(limit).map(::withSets) }
        override fun observeActiveWorkout() =
            query { workouts.filter { it.endedAt == null }.maxByOrNull { it.startedAt }?.let(::withSets) }
        override suspend fun getWorkout(id: Long) = workouts.firstOrNull { it.id == id }?.let(::withSets)
        override fun observeWorkoutsOn(epochDay: Long) =
            query { workouts.filter { it.epochDay == epochDay }.sortedBy { it.startedAt }.map(::withSets) }
        override fun observeWorkoutsInRange(from: Long, to: Long) =
            query { workouts.filter { it.epochDay in from..to && it.endedAt != null } }
        override suspend fun upsertWorkout(workout: WorkoutEntity): Long {
            if (workout.id != 0L && workouts.any { it.id == workout.id }) {
                workouts.replaceAll { if (it.id == workout.id) workout else it }
                return -1
            }
            val id = nextId++
            workouts += workout.copy(id = id)
            return id
        }
        override suspend fun deleteWorkout(id: Long) {
            workouts.removeAll { it.id == id }
            sets.removeAll { it.workoutId == id }
        }
        override suspend fun insertSet(set: WorkoutSetEntity): Long {
            val id = nextId++
            sets += set.copy(id = id)
            return id
        }
        override suspend fun updateSet(set: WorkoutSetEntity) {
            sets.replaceAll { if (it.id == set.id) set else it }
        }
        override suspend fun deleteSet(id: Long) {
            sets.removeAll { it.id == id }
        }
        override suspend fun deleteSetsForExercise(workoutId: Long, exerciseId: Long) {
            sets.removeAll { it.workoutId == workoutId && it.exerciseId == exerciseId }
        }

        override fun observeRecords(exerciseId: Long) = query { records(exerciseId, excludingWorkoutId = -1) }
        override suspend fun getRecords(exerciseId: Long, excludingWorkoutId: Long) =
            records(exerciseId, excludingWorkoutId)

        private fun records(exerciseId: Long, excludingWorkoutId: Long) =
            finishedWorkoutsWith(exerciseId, excludingWorkoutId).flatMap { w ->
                sets.filter { it.workoutId == w.id && it.exerciseId == exerciseId && it.isDone }
                    .sortedBy { it.setIndex }
                    .map { SetRecordRow(w.epochDay, w.id, it.weightKg, it.reps, it.durationSec, it.distanceM) }
            }

        override suspend fun getLastSets(exerciseId: Long, excludingWorkoutId: Long): List<WorkoutSetEntity> {
            val last = finishedWorkoutsWith(exerciseId, excludingWorkoutId).lastOrNull() ?: return emptyList()
            return sets.filter { it.workoutId == last.id && it.exerciseId == exerciseId && it.isDone }
                .sortedBy { it.setIndex }
        }

        override fun observeExerciseIdsWithHistory(): Flow<List<Long>> = error("unused")
    }

    private val dao = FakeWorkoutDao()
    private val repository = WorkoutRepository(dao)
    private val today = LocalDate.of(2026, 10, 19)

    private suspend fun exercise(
        name: String,
        type: ExerciseType = ExerciseType.STRENGTH,
        group: MuscleGroup = MuscleGroup.CHEST,
    ): Exercise = repository.saveExercise(Exercise(0, name, group, type, isCustom = true))

    private data class Lift(
        val weightKg: Double = 0.0,
        val reps: Int = 0,
        val distanceM: Double = 0.0,
        val durationSec: Int = 0,
        val isDone: Boolean = true,
    )

    /** A workout already in the log, finished unless [finished] is false. */
    private suspend fun pastWorkout(
        exercise: Exercise,
        vararg lifts: Lift,
        startedAt: Long,
        finished: Boolean = true,
    ): Long {
        val id = dao.upsertWorkout(
            WorkoutEntity(
                name = "Past",
                epochDay = today.toEpochDay(),
                startedAt = startedAt,
                endedAt = if (finished) startedAt + 3_600_000 else null,
            ),
        )
        lifts.forEachIndexed { index, lift -> dao.insertSet(setEntity(id, exercise.id, index, lift)) }
        return id
    }

    private fun setEntity(workoutId: Long, exerciseId: Long, index: Int, lift: Lift, order: Int = 0) = WorkoutSetEntity(
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseOrder = order,
        setIndex = index,
        weightKg = lift.weightKg,
        reps = lift.reps,
        durationSec = lift.durationSec,
        distanceM = lift.distanceM,
        isDone = lift.isDone,
    )

    /** Starts today's workout with [lifts] logged for [exercise] and finishes it. */
    private suspend fun finishWith(exercise: Exercise, vararg lifts: Lift): List<PersonalRecord> {
        val id = pastWorkout(exercise, *lifts, startedAt = 10_000_000_000, finished = false)
        return repository.finishWorkout(repository.getWorkout(id)!!)
    }

    @Test
    fun `defaults are seeded only into an empty library`() = runTest {
        repository.seedExercisesIfEmpty()
        val seeded = dao.exercises.size
        assertEquals(ExerciseLibrary.DEFAULTS.size, seeded)

        repository.seedExercisesIfEmpty()
        assertEquals(seeded, dao.exercises.size)
    }

    @Test
    fun `new exercise is inserted with a trimmed name and gets its id`() = runTest {
        val saved = repository.saveExercise(
            Exercise(0, "  Cable fly ", MuscleGroup.CHEST, ExerciseType.STRENGTH, isCustom = true, 4, 12),
        )

        assertTrue(saved.id > 0)
        assertEquals("Cable fly", saved.name)
        assertEquals(listOf(saved), repository.observeExercises().first())
    }

    @Test
    fun `existing exercise is updated in place`() = runTest {
        val bench = exercise("Bench")

        val renamed = repository.saveExercise(bench.copy(name = "Bench press", defaultReps = 5))

        assertEquals(1, dao.exerciseUpdates)
        assertEquals(bench.id, renamed.id)
        assertEquals(listOf(renamed), repository.observeExercises().first())
    }

    @Test
    fun `unknown muscle group and type stored in the database fall back to defaults`() = runTest {
        dao.exercises += ExerciseEntity(id = 7, name = "Odd", muscleGroup = "NECK", type = "YOGA", isCustom = true)

        val odd = repository.observeExercises().first().single()

        assertEquals(MuscleGroup.OTHER, odd.muscleGroup)
        assertEquals(ExerciseType.STRENGTH, odd.type)
    }

    @Test
    fun `exercise usage counts distinct workouts and routines`() = runTest {
        val bench = exercise("Bench")
        assertFalse(repository.exerciseUsage(bench.id).isUsed)

        pastWorkout(bench, Lift(60.0, 8), Lift(60.0, 8), startedAt = 1_000)
        pastWorkout(bench, Lift(60.0, 8), startedAt = 2_000)
        repository.saveRoutine(Routine(0, "Push", listOf(RoutineExercise(bench, 3, 8))))

        val usage = repository.exerciseUsage(bench.id)
        assertEquals(ExerciseUsage(workouts = 2, routines = 1), usage)
        assertTrue(usage.isUsed)
        assertTrue(ExerciseUsage(workouts = 0, routines = 1).isUsed)
    }

    @Test
    fun `saved routine keeps exercise order and drops entries whose exercise is gone`() = runTest {
        val squat = exercise("Squat", group = MuscleGroup.LEGS)
        val bench = exercise("Bench")
        val row = exercise("Row", group = MuscleGroup.BACK)
        val id = repository.saveRoutine(
            Routine(
                0,
                "Full body",
                listOf(RoutineExercise(squat, 5, 5), RoutineExercise(bench, 3, 8), RoutineExercise(row, 3, 10)),
            ),
        )
        dao.exercises.removeAll { it.id == bench.id }

        val routine = repository.getRoutine(id)!!

        assertEquals("Full body", routine.name)
        assertEquals(listOf(squat, row), routine.exercises.map { it.exercise })
        assertEquals(listOf(5 to 5, 3 to 10), routine.exercises.map { it.targetSets to it.targetReps })
        assertEquals(listOf(routine), repository.observeRoutines().first())
    }

    @Test
    fun `editing a routine keeps its creation time and replaces its exercises`() = runTest {
        val squat = exercise("Squat")
        val bench = exercise("Bench")
        val id = repository.saveRoutine(Routine(0, "A", listOf(RoutineExercise(squat, 5, 5))))
        val createdAt = dao.routines.single().createdAt

        val sameId = repository.saveRoutine(
            Routine(id, "A2", listOf(RoutineExercise(bench, 3, 8), RoutineExercise(squat, 2, 3))),
        )

        assertEquals(id, sameId)
        assertEquals(createdAt, dao.routines.single().createdAt)
        assertEquals(listOf(bench, squat), repository.getRoutine(id)!!.exercises.map { it.exercise })
        assertEquals(listOf(0, 1), dao.routineExercises.sortedBy { it.position }.map { it.position })
    }

    @Test
    fun `missing routine and workout are null`() = runTest {
        assertNull(repository.getRoutine(42))
        assertNull(repository.getWorkout(42))
    }

    @Test
    fun `free workout starts today with no sets`() = runTest {
        val id = repository.startWorkout("Evening", routine = null)

        val workout = repository.observeActiveWorkout().first()!!
        assertEquals(id, workout.id)
        assertEquals("Evening", workout.name)
        assertEquals(LocalDate.now(), workout.date)
        assertTrue(workout.isActive)
        assertTrue(workout.exercises.isEmpty())
    }

    @Test
    fun `routine pre-fills sets from the last finished session`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(50.0, 10), startedAt = 1_000)
        // The newest finished session wins; its undone set is not a template.
        pastWorkout(bench, Lift(60.0, 8), Lift(62.5, 6), Lift(70.0, 1, isDone = false), startedAt = 2_000)
        // An unfinished workout is not "last time".
        pastWorkout(bench, Lift(99.0, 1), startedAt = 3_000, finished = false)

        val id = repository.startWorkout("Push", Routine(1, "Push", listOf(RoutineExercise(bench, 4, 12))))

        val sets = repository.getWorkout(id)!!.exercises.single().sets
        assertEquals(listOf(60.0, 62.5, 62.5, 62.5), sets.map { it.weightKg })
        assertEquals(listOf(8, 6, 6, 6), sets.map { it.reps })
        assertEquals(listOf(0, 1, 2, 3), sets.map { it.setIndex })
        assertTrue(sets.none { it.isDone })
    }

    @Test
    fun `routine exercise never done before starts at its target reps`() = runTest {
        val bench = exercise("Bench")
        val run = exercise("Run", type = ExerciseType.CARDIO, group = MuscleGroup.CARDIO)
        pastWorkout(run, Lift(distanceM = 5_000.0, durationSec = 1_500), startedAt = 1_000)

        val id = repository.startWorkout(
            "Mixed",
            Routine(1, "Mixed", listOf(RoutineExercise(bench, 2, 10), RoutineExercise(run, 0, 0))),
        )

        val workout = repository.getWorkout(id)!!
        assertEquals(listOf(bench, run), workout.exercises.map { it.exercise })
        val benchSets = workout.exercises[0].sets
        assertEquals(listOf(10, 10), benchSets.map { it.reps })
        assertTrue(benchSets.all { it.weightKg == 0.0 })
        // A routine entry with no sets still gets one, copied from last time.
        val runSet = workout.exercises[1].sets.single()
        assertEquals(5_000.0, runSet.distanceM, 0.0)
        assertEquals(1_500, runSet.durationSec)
        assertEquals(1, runSet.exerciseOrder)
    }

    @Test
    fun `first ever session sets no record`() = runTest {
        val bench = exercise("Bench")
        val run = exercise("Run", type = ExerciseType.CARDIO)

        assertTrue(finishWith(bench, Lift(100.0, 5)).isEmpty())
        assertTrue(finishWith(run, Lift(distanceM = 10_000.0, durationSec = 3_000)).isEmpty())
    }

    @Test
    fun `better estimated one-rep max is reported against the previous best`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(80.0, 5), Lift(90.0, 3), startedAt = 1_000)

        val records = finishWith(bench, Lift(85.0, 8), Lift(80.0, 8))

        val record = records.single() as PersonalRecord.Estimated1Rm
        assertEquals(bench, record.exercise)
        assertEquals(85.0, record.set.weightKg, 0.0)
        assertEquals(8, record.set.reps)
        assertEquals(99.0, record.previous, 1e-9) // 90 x (1 + 3/30)
    }

    @Test
    fun `heavier set without a better estimate is a heaviest-set record`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(100.0, 5), startedAt = 1_000)

        val record = finishWith(bench, Lift(105.0, 1)).single() as PersonalRecord.HeaviestSet

        assertEquals(105.0, record.weightKg, 0.0)
        assertEquals(100.0, record.previous, 0.0)
    }

    @Test
    fun `matching the previous best is not a record`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(100.0, 5), startedAt = 1_000)

        assertTrue(finishWith(bench, Lift(100.0, 5), Lift(90.0, 5)).isEmpty())
    }

    @Test
    fun `sets that are not done or have no work are ignored for records`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(100.0, 5), startedAt = 1_000)

        val records = finishWith(bench, Lift(150.0, 5, isDone = false), Lift(200.0, 0), Lift(90.0, 5))

        assertTrue(records.isEmpty())
    }

    @Test
    fun `an exercise with nothing done yields no record`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(100.0, 5), startedAt = 1_000)

        assertTrue(finishWith(bench, Lift(150.0, 5, isDone = false)).isEmpty())
    }

    @Test
    fun `unfinished past workouts do not count as history`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(200.0, 5), startedAt = 1_000, finished = false)
        pastWorkout(bench, Lift(80.0, 5), startedAt = 2_000)

        val record = finishWith(bench, Lift(90.0, 5)).single() as PersonalRecord.Estimated1Rm

        assertEquals(80.0 * (1 + 5 / 30.0), record.previous, 1e-9)
    }

    @Test
    fun `longer cardio distance is reported in kilometres`() = runTest {
        val run = exercise("Run", type = ExerciseType.CARDIO, group = MuscleGroup.CARDIO)
        pastWorkout(run, Lift(distanceM = 5_000.0, durationSec = 1_500), startedAt = 1_000)

        val record = finishWith(run, Lift(distanceM = 7_500.0, durationSec = 2_400)).single()

        record as PersonalRecord.LongestDistance
        assertEquals(7.5, record.distanceKm, 1e-9)
        assertEquals(5.0, record.previousKm, 1e-9)
        assertTrue(finishWith(run, Lift(distanceM = 6_000.0, durationSec = 1_800)).isEmpty())
    }

    @Test
    fun `records are checked per exercise`() = runTest {
        val bench = exercise("Bench")
        val squat = exercise("Squat", group = MuscleGroup.LEGS)
        pastWorkout(bench, Lift(100.0, 5), startedAt = 1_000)
        pastWorkout(squat, Lift(120.0, 5), startedAt = 1_500)
        val id = dao.upsertWorkout(WorkoutEntity(name = "Today", epochDay = 1, startedAt = 5_000, endedAt = null))
        dao.insertSet(setEntity(id, bench.id, 0, Lift(110.0, 5), order = 0))
        dao.insertSet(setEntity(id, squat.id, 0, Lift(110.0, 5), order = 1))

        val records = repository.finishWorkout(repository.getWorkout(id)!!)

        assertEquals(listOf(bench), records.map { it.exercise })
    }

    @Test
    fun `finishing stamps the end time and keeps the rest`() = runTest {
        val bench = exercise("Bench")
        val id = pastWorkout(bench, Lift(60.0, 8), startedAt = 1_000, finished = false)
        val before = System.currentTimeMillis()

        repository.finishWorkout(repository.getWorkout(id)!!)

        val stored = dao.workouts.single { it.id == id }
        assertNotNull(stored.endedAt)
        assertTrue(stored.endedAt!! >= before)
        assertEquals(1_000L, stored.startedAt)
        assertEquals("Past", stored.name)
        assertNull(repository.observeActiveWorkout().first())
    }

    @Test
    fun `rename keeps the workout's times and day`() = runTest {
        val bench = exercise("Bench")
        val id = pastWorkout(bench, Lift(60.0, 8), startedAt = 1_000)
        val workout = repository.getWorkout(id)!!

        repository.renameWorkout(workout, "Chest day")

        assertEquals(workout.copy(name = "Chest day"), repository.getWorkout(id))
    }

    @Test
    fun `workout groups sets by exercise in their order and drops deleted exercises`() = runTest {
        val bench = exercise("Bench")
        val squat = exercise("Squat")
        val gone = exercise("Gone")
        val id = dao.upsertWorkout(
            WorkoutEntity(name = "W", epochDay = today.toEpochDay(), startedAt = 0, endedAt = 60_000),
        )
        dao.insertSet(setEntity(id, squat.id, 1, Lift(100.0, 5), order = 1))
        dao.insertSet(setEntity(id, bench.id, 0, Lift(60.0, 8), order = 0))
        dao.insertSet(setEntity(id, squat.id, 0, Lift(90.0, 5), order = 1))
        dao.insertSet(setEntity(id, gone.id, 0, Lift(10.0, 5), order = 2))
        dao.exercises.removeAll { it.id == gone.id }

        val workout = repository.getWorkout(id)!!

        assertEquals(today, workout.date)
        assertEquals(listOf(bench, squat), workout.exercises.map { it.exercise })
        assertEquals(listOf(90.0, 100.0), workout.exercises[1].sets.map { it.weightKg })
        assertEquals(listOf(workout), repository.observeWorkoutsOn(today).first())
        assertEquals(listOf(workout), repository.observeRecentWorkouts().first())
    }

    @Test
    fun `workout days include only finished workouts in range`() = runTest {
        val bench = exercise("Bench")
        pastWorkout(bench, Lift(60.0, 8), startedAt = 1_000)
        pastWorkout(bench, Lift(60.0, 8), startedAt = 2_000)
        dao.upsertWorkout(WorkoutEntity(0, "Old", today.minusDays(30).toEpochDay(), startedAt = 0, endedAt = 1))
        dao.upsertWorkout(WorkoutEntity(0, "Open", today.minusDays(1).toEpochDay(), startedAt = 0, endedAt = null))

        val days = repository.observeWorkoutDays(today.minusDays(6), today).first()

        assertEquals(setOf(today), days)
    }

    @Test
    fun `added set copies its template or starts from the given reps`() = runTest {
        val bench = exercise("Bench")
        val id = repository.startWorkout("W", routine = null)
        val template = WorkoutSet(99, bench.id, 0, 0, 70.0, 6, 0, 0.0, isDone = true)

        val copied = repository.addSet(id, bench, exerciseOrder = 0, setIndex = 1, template = template)
        val blank = repository.addSet(id, bench, exerciseOrder = 0, setIndex = 2, template = null, reps = 12)

        assertEquals(WorkoutSet(copied.id, bench.id, 0, 1, 70.0, 6, 0, 0.0, isDone = false), copied)
        assertEquals(WorkoutSet(blank.id, bench.id, 0, 2, 0.0, 12, 0, 0.0, isDone = false), blank)
        assertEquals(listOf(copied, blank), repository.getWorkout(id)!!.exercises.single().sets)
    }

    @Test
    fun `updated set is written through and deleted sets disappear`() = runTest {
        val bench = exercise("Bench")
        val squat = exercise("Squat")
        val id = repository.startWorkout("W", routine = null)
        val first = repository.addSet(id, bench, 0, 0, template = null, reps = 8)
        val second = repository.addSet(id, bench, 0, 1, template = null, reps = 8)
        repository.addSet(id, squat, 1, 0, template = null, reps = 5)

        repository.updateSet(id, first.copy(weightKg = 80.0, isDone = true))
        repository.deleteSet(second.id)
        repository.removeExercise(id, squat.id)

        val workout = repository.getWorkout(id)!!
        assertEquals(listOf(first.copy(weightKg = 80.0, isDone = true)), workout.exercises.single().sets)
        assertEquals(1, workout.doneSets)
    }

    @Test
    fun `last sets and records come only from finished sessions`() = runTest {
        val bench = exercise("Bench")
        val earlier = pastWorkout(bench, Lift(60.0, 8), startedAt = 1_000)
        val later = pastWorkout(bench, Lift(65.0, 8), Lift(67.5, 1, isDone = false), startedAt = 2_000)
        val current = pastWorkout(bench, Lift(70.0, 8), startedAt = 3_000, finished = false)

        assertEquals(listOf(65.0), repository.lastSets(bench.id, current).map { it.weightKg })
        assertEquals(listOf(60.0), repository.lastSets(bench.id, later).map { it.weightKg })
        assertEquals(
            listOf(
                SetRecord(today, earlier, 60.0, 8, 0, 0.0),
                SetRecord(today, later, 65.0, 8, 0, 0.0),
            ),
            repository.observeRecords(bench.id).first(),
        )
    }

    @Test
    fun `deleting removes the workout, routine and exercise`() = runTest {
        val bench = exercise("Bench")
        val workoutId = pastWorkout(bench, Lift(60.0, 8), startedAt = 1_000)
        val routineId = repository.saveRoutine(Routine(0, "R", listOf(RoutineExercise(bench, 3, 8))))

        repository.deleteWorkout(workoutId)
        repository.deleteRoutine(routineId)
        repository.deleteExercise(bench.id)

        assertNull(repository.getWorkout(workoutId))
        assertNull(repository.getRoutine(routineId))
        assertTrue(repository.observeExercises().first().isEmpty())
    }

    @Test
    fun `active workout reflects what the dao holds`() = runTest {
        assertNull(repository.observeActiveWorkout().first())
        val id = repository.startWorkout("Now", routine = null)
        val active: Workout? = repository.observeActiveWorkout().first()
        assertEquals(id, active?.id)
    }
}
