package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Exercises

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun getExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    // Routines

    @Transaction
    @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE")
    fun observeRoutines(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: Long): RoutineWithExercises?

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity): Long

    @Insert
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Transaction
    suspend fun saveRoutine(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>): Long {
        val id = upsertRoutine(routine).takeIf { it > 0 } ?: routine.id
        deleteRoutineExercises(id)
        insertRoutineExercises(exercises.map { it.copy(routineId = id) })
        return id
    }

    // Workouts

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentWorkouts(limit: Int): Flow<List<WorkoutWithSets>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(): Flow<WorkoutWithSets?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: Long): WorkoutWithSets?

    @Transaction
    @Query("SELECT * FROM workouts WHERE epochDay = :epochDay ORDER BY startedAt")
    fun observeWorkoutsOn(epochDay: Long): Flow<List<WorkoutWithSets>>

    @Query("SELECT * FROM workouts WHERE epochDay BETWEEN :from AND :to AND endedAt IS NOT NULL")
    fun observeWorkoutsInRange(from: Long, to: Long): Flow<List<WorkoutEntity>>

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteSetsForExercise(workoutId: Long, exerciseId: Long)

    // History and progress

    @Query(
        "SELECT w.epochDay AS epochDay, s.workoutId AS workoutId, s.weightKg AS weightKg, s.reps AS reps, " +
            "s.durationSec AS durationSec, s.distanceM AS distanceM " +
            "FROM workout_sets s JOIN workouts w ON w.id = s.workoutId " +
            "WHERE s.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL " +
            "ORDER BY w.startedAt, s.setIndex",
    )
    fun observeRecords(exerciseId: Long): Flow<List<SetRecordRow>>

    @Query(
        "SELECT w.epochDay AS epochDay, s.workoutId AS workoutId, s.weightKg AS weightKg, s.reps AS reps, " +
            "s.durationSec AS durationSec, s.distanceM AS distanceM " +
            "FROM workout_sets s JOIN workouts w ON w.id = s.workoutId " +
            "WHERE s.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL AND w.id != :excludingWorkoutId " +
            "ORDER BY w.startedAt, s.setIndex",
    )
    suspend fun getRecords(exerciseId: Long, excludingWorkoutId: Long): List<SetRecordRow>

    /** The sets from the most recent finished workout that included this exercise. */
    @Query(
        "SELECT * FROM workout_sets WHERE exerciseId = :exerciseId AND isDone = 1 AND workoutId = (" +
            "SELECT w.id FROM workouts w JOIN workout_sets s ON s.workoutId = w.id " +
            "WHERE s.exerciseId = :exerciseId AND s.isDone = 1 AND w.endedAt IS NOT NULL AND w.id != :excludingWorkoutId " +
            "ORDER BY w.startedAt DESC LIMIT 1) ORDER BY setIndex",
    )
    suspend fun getLastSets(exerciseId: Long, excludingWorkoutId: Long): List<WorkoutSetEntity>

    @Query(
        "SELECT DISTINCT s.exerciseId FROM workout_sets s JOIN workouts w ON w.id = s.workoutId " +
            "WHERE s.isDone = 1 AND w.endedAt IS NOT NULL",
    )
    fun observeExerciseIdsWithHistory(): Flow<List<Long>>
}
