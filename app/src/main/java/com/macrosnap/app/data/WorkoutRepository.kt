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
import com.macrosnap.app.model.WorkoutExercise
import com.macrosnap.app.model.WorkoutSet
import com.macrosnap.app.ui.compact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    suspend fun seedExercisesIfEmpty() {
        if (workoutDao.countExercises() == 0) workoutDao.insertExercises(ExerciseLibrary.DEFAULTS)
    }

    fun observeExercises(): Flow<List<Exercise>> = workoutDao.observeExercises().map { rows -> rows.map { it.toModel() } }

    /** Inserts a new exercise when [Exercise.id] is 0, otherwise updates it. Returns the saved exercise. */
    suspend fun saveExercise(exercise: Exercise): Exercise {
        val entity = ExerciseEntity(
            id = exercise.id,
            name = exercise.name.trim(),
            muscleGroup = exercise.muscleGroup.name,
            type = exercise.type.name,
            isCustom = exercise.isCustom,
            defaultSets = exercise.defaultSets,
            defaultReps = exercise.defaultReps,
        )
        if (exercise.id == 0L) return entity.copy(id = workoutDao.insertExercise(entity)).toModel()
        workoutDao.updateExercise(entity)
        return entity.toModel()
    }

    /** Deleting cascades to the sets and routine entries that use the exercise. */
    suspend fun deleteExercise(id: Long) = workoutDao.deleteExercise(id)

    suspend fun exerciseUsage(id: Long): ExerciseUsage =
        ExerciseUsage(workouts = workoutDao.countWorkoutsUsing(id), routines = workoutDao.countRoutinesUsing(id))

    fun observeRoutines(): Flow<List<Routine>> =
        combine(workoutDao.observeRoutines(), workoutDao.observeExercises()) { routines, exercises ->
            val byId = exercises.associateBy { it.id }
            routines.map { it.toModel(byId) }
        }

    suspend fun getRoutine(id: Long): Routine? {
        val byId = workoutDao.getExercises().associateBy { it.id }
        return workoutDao.getRoutine(id)?.toModel(byId)
    }

    suspend fun saveRoutine(routine: Routine): Long {
        val existing = if (routine.id != 0L) workoutDao.getRoutine(routine.id) else null
        val entity = RoutineEntity(id = routine.id, name = routine.name, createdAt = existing?.routine?.createdAt ?: System.currentTimeMillis())
        val exercises = routine.exercises.mapIndexed { index, it ->
            RoutineExerciseEntity(routineId = routine.id, exerciseId = it.exercise.id, position = index, targetSets = it.targetSets, targetReps = it.targetReps)
        }
        return workoutDao.saveRoutine(entity, exercises)
    }

    suspend fun deleteRoutine(id: Long) = workoutDao.deleteRoutine(id)

    fun observeRecentWorkouts(limit: Int = 30): Flow<List<Workout>> =
        combine(workoutDao.observeRecentWorkouts(limit), workoutDao.observeExercises()) { workouts, exercises ->
            val byId = exercises.associateBy { it.id }
            workouts.map { it.toModel(byId) }
        }

    fun observeActiveWorkout(): Flow<Workout?> =
        combine(workoutDao.observeActiveWorkout(), workoutDao.observeExercises()) { workout, exercises ->
            workout?.toModel(exercises.associateBy { it.id })
        }

    fun observeWorkoutsOn(date: LocalDate): Flow<List<Workout>> =
        combine(workoutDao.observeWorkoutsOn(date.toEpochDay()), workoutDao.observeExercises()) { workouts, exercises ->
            val byId = exercises.associateBy { it.id }
            workouts.map { it.toModel(byId) }
        }

    fun observeWorkoutDays(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>> =
        workoutDao.observeWorkoutsInRange(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { LocalDate.ofEpochDay(it.epochDay) }.toSet() }

    suspend fun getWorkout(id: Long): Workout? {
        val byId = workoutDao.getExercises().associateBy { it.id }
        return workoutDao.getWorkout(id)?.toModel(byId)
    }

    suspend fun startWorkout(name: String, routine: Routine?): Long {
        val now = System.currentTimeMillis()
        val workoutId = workoutDao.upsertWorkout(WorkoutEntity(name = name, epochDay = LocalDate.now().toEpochDay(), startedAt = now, endedAt = null))
        routine?.exercises?.forEachIndexed { order, planned ->
            // Pre-fill each set with what was lifted last time so the user only edits what changed.
            val last = workoutDao.getLastSets(planned.exercise.id, workoutId)
            repeat(planned.targetSets.coerceAtLeast(1)) { index ->
                val previous = last.getOrNull(index) ?: last.lastOrNull()
                workoutDao.insertSet(
                    WorkoutSetEntity(
                        workoutId = workoutId,
                        exerciseId = planned.exercise.id,
                        exerciseOrder = order,
                        setIndex = index,
                        weightKg = previous?.weightKg ?: 0.0,
                        reps = previous?.reps ?: planned.targetReps,
                        durationSec = previous?.durationSec ?: 0,
                        distanceM = previous?.distanceM ?: 0.0,
                        isDone = false,
                    ),
                )
            }
        }
        return workoutId
    }

    suspend fun renameWorkout(workout: Workout, name: String) =
        workoutDao.upsertWorkout(WorkoutEntity(workout.id, name, workout.date.toEpochDay(), workout.startedAt, workout.endedAt))

    suspend fun finishWorkout(workout: Workout): List<PersonalRecord> {
        val records = workout.exercises.mapNotNull { exercise -> personalRecordFor(exercise, workout.id) }
        workoutDao.upsertWorkout(WorkoutEntity(workout.id, workout.name, workout.date.toEpochDay(), workout.startedAt, System.currentTimeMillis()))
        return records
    }

    suspend fun deleteWorkout(id: Long) = workoutDao.deleteWorkout(id)

    suspend fun addSet(workoutId: Long, exercise: Exercise, exerciseOrder: Int, setIndex: Int, template: WorkoutSet?, reps: Int = 0): WorkoutSet {
        val entity = WorkoutSetEntity(
            workoutId = workoutId,
            exerciseId = exercise.id,
            exerciseOrder = exerciseOrder,
            setIndex = setIndex,
            weightKg = template?.weightKg ?: 0.0,
            reps = template?.reps ?: reps,
            durationSec = template?.durationSec ?: 0,
            distanceM = template?.distanceM ?: 0.0,
            isDone = false,
        )
        val id = workoutDao.insertSet(entity)
        return entity.copy(id = id).toModel()
    }

    suspend fun updateSet(workoutId: Long, set: WorkoutSet) = workoutDao.updateSet(
        WorkoutSetEntity(set.id, workoutId, set.exerciseId, set.exerciseOrder, set.setIndex, set.weightKg, set.reps, set.durationSec, set.distanceM, set.isDone),
    )

    suspend fun deleteSet(id: Long) = workoutDao.deleteSet(id)

    suspend fun removeExercise(workoutId: Long, exerciseId: Long) = workoutDao.deleteSetsForExercise(workoutId, exerciseId)

    suspend fun lastSets(exerciseId: Long, excludingWorkoutId: Long): List<WorkoutSet> =
        workoutDao.getLastSets(exerciseId, excludingWorkoutId).map { it.toModel() }

    fun observeRecords(exerciseId: Long): Flow<List<SetRecord>> =
        workoutDao.observeRecords(exerciseId).map { rows -> rows.map { it.toModel() } }

    fun observeExerciseIdsWithHistory(): Flow<List<Long>> = workoutDao.observeExerciseIdsWithHistory()

    private suspend fun personalRecordFor(exercise: WorkoutExercise, workoutId: Long): PersonalRecord? {
        val done = exercise.sets.filter { it.isDone && it.hasWork }
        if (done.isEmpty()) return null
        val history = workoutDao.getRecords(exercise.exercise.id, workoutId).map { it.toModel() }
        return when (exercise.exercise.type) {
            ExerciseType.STRENGTH -> {
                val best = done.maxByOrNull { it.estimated1Rm } ?: return null
                val previousBest = history.maxOfOrNull { it.estimated1Rm } ?: 0.0
                val heaviest = done.maxOf { it.weightKg }
                val previousHeaviest = history.maxOfOrNull { it.weightKg } ?: 0.0
                when {
                    best.estimated1Rm > previousBest && previousBest > 0 ->
                        PersonalRecord(exercise.exercise, "${best.weightKg.compact()} kg × ${best.reps} (est. 1RM ${best.estimated1Rm.compact()} kg, was ${previousBest.compact()})")
                    heaviest > previousHeaviest && previousHeaviest > 0 ->
                        PersonalRecord(exercise.exercise, "heaviest set ${heaviest.compact()} kg (was ${previousHeaviest.compact()})")
                    else -> null
                }
            }
            ExerciseType.CARDIO -> {
                val longest = done.maxOf { it.distanceM }
                val previousLongest = history.maxOfOrNull { it.distanceM } ?: 0.0
                if (longest > previousLongest && previousLongest > 0) {
                    PersonalRecord(exercise.exercise, "longest distance ${(longest / 1000).compact()} km (was ${(previousLongest / 1000).compact()})")
                } else {
                    null
                }
            }
        }
    }
}

data class ExerciseUsage(val workouts: Int, val routines: Int) {
    val isUsed: Boolean get() = workouts > 0 || routines > 0
}

private fun ExerciseEntity.toModel() = Exercise(
    id = id,
    name = name,
    muscleGroup = MuscleGroup.entries.firstOrNull { it.name == muscleGroup } ?: MuscleGroup.OTHER,
    type = ExerciseType.entries.firstOrNull { it.name == type } ?: ExerciseType.STRENGTH,
    isCustom = isCustom,
    defaultSets = defaultSets,
    defaultReps = defaultReps,
)

private fun WorkoutSetEntity.toModel() = WorkoutSet(id, exerciseId, exerciseOrder, setIndex, weightKg, reps, durationSec, distanceM, isDone)

private fun SetRecordRow.toModel() = SetRecord(LocalDate.ofEpochDay(epochDay), workoutId, weightKg, reps, durationSec, distanceM)

private fun RoutineWithExercises.toModel(exercisesById: Map<Long, ExerciseEntity>) = Routine(
    id = routine.id,
    name = routine.name,
    exercises = exercises.sortedBy { it.position }.mapNotNull { row ->
        exercisesById[row.exerciseId]?.let { RoutineExercise(it.toModel(), row.targetSets, row.targetReps) }
    },
)

private fun WorkoutWithSets.toModel(exercisesById: Map<Long, ExerciseEntity>) = Workout(
    id = workout.id,
    name = workout.name,
    date = LocalDate.ofEpochDay(workout.epochDay),
    startedAt = workout.startedAt,
    endedAt = workout.endedAt,
    exercises = sets
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, rows) ->
            val exercise = exercisesById[exerciseId] ?: return@mapNotNull null
            WorkoutExercise(exercise.toModel(), rows.sortedBy { it.setIndex }.map { it.toModel() })
        }
        .sortedBy { it.sets.first().exerciseOrder },
)
