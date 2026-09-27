package com.macrosnap.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodEntryEntity::class,
        WeightEntryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        SupplementEntity::class,
        SupplementLogEntity::class,
        WaterLogEntity::class,
        SleepLogEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun weightDao(): WeightDao
    abstract fun recipeDao(): RecipeDao
    abstract fun supplementDao(): SupplementDao
    abstract fun wellnessDao(): WellnessDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `recipes` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                            "`cookedWeightGrams` REAL, `servings` INTEGER NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `recipe_ingredients` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, " +
                            "`position` INTEGER NOT NULL, `name` TEXT NOT NULL, `grams` REAL NOT NULL, " +
                            "`kcalPer100` REAL NOT NULL, `proteinPer100` REAL NOT NULL, " +
                            "`carbsPer100` REAL NOT NULL, `fatPer100` REAL NOT NULL, " +
                            "FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)")
                }
            },
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `food_entries` ADD COLUMN `micros` TEXT")
                    db.execSQL("ALTER TABLE `recipe_ingredients` ADD COLUMN `micros` TEXT")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `supplements` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                            "`doseLabel` TEXT NOT NULL, `timesPerDay` INTEGER NOT NULL, `micros` TEXT, " +
                            "`isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `supplement_logs` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `supplementId` INTEGER NOT NULL, " +
                            "`epochDay` INTEGER NOT NULL, `takenAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`supplementId`) REFERENCES `supplements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplement_logs_supplementId` ON `supplement_logs` (`supplementId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplement_logs_epochDay` ON `supplement_logs` (`epochDay`)")
                }
            },
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `water_logs` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                            "`ml` INTEGER NOT NULL, `loggedAt` INTEGER NOT NULL)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_logs_epochDay` ON `water_logs` (`epochDay`)")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `sleep_logs` (" +
                            "`epochDay` INTEGER NOT NULL, `bedtime` INTEGER NOT NULL, `wakeTime` INTEGER NOT NULL, " +
                            "`quality` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `exercises` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                            "`muscleGroup` TEXT NOT NULL, `type` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `routines` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `routine_exercises` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routineId` INTEGER NOT NULL, " +
                            "`exerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                            "`targetSets` INTEGER NOT NULL, `targetReps` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercises_routineId` ON `routine_exercises` (`routineId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercises_exerciseId` ON `routine_exercises` (`exerciseId`)")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `workouts` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                            "`epochDay` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workouts_epochDay` ON `workouts` (`epochDay`)")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `workout_sets` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutId` INTEGER NOT NULL, " +
                            "`exerciseId` INTEGER NOT NULL, `exerciseOrder` INTEGER NOT NULL, `setIndex` INTEGER NOT NULL, " +
                            "`weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `durationSec` INTEGER NOT NULL, " +
                            "`distanceM` REAL NOT NULL, `isDone` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_workoutId` ON `workout_sets` (`workoutId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON `workout_sets` (`exerciseId`)")
                }
            },
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `supplement_logs` ADD COLUMN `micros` TEXT")
                    // Existing doses get the supplement's current content: the best record there is.
                    db.execSQL(
                        "UPDATE `supplement_logs` SET `micros` = " +
                            "(SELECT `micros` FROM `supplements` WHERE `supplements`.`id` = `supplement_logs`.`supplementId`)",
                    )
                }
            },
        )
    }
}
