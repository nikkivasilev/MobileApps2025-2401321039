package com.macrosnap.app.data

import com.macrosnap.app.data.local.ExerciseEntity
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup

/** Seeded once on first launch; the user adds anything missing as a custom exercise. */
object ExerciseLibrary {

    val DEFAULTS: List<ExerciseEntity> = buildList {
        fun s(name: String, group: MuscleGroup) = add(ExerciseEntity(name = name, muscleGroup = group.name, type = ExerciseType.STRENGTH.name, isCustom = false))
        fun c(name: String) = add(ExerciseEntity(name = name, muscleGroup = MuscleGroup.CARDIO.name, type = ExerciseType.CARDIO.name, isCustom = false))

        s("Bench press", MuscleGroup.CHEST)
        s("Incline bench press", MuscleGroup.CHEST)
        s("Dumbbell bench press", MuscleGroup.CHEST)
        s("Incline dumbbell press", MuscleGroup.CHEST)
        s("Chest fly", MuscleGroup.CHEST)
        s("Cable crossover", MuscleGroup.CHEST)
        s("Push-up", MuscleGroup.CHEST)
        s("Dip", MuscleGroup.CHEST)

        s("Deadlift", MuscleGroup.BACK)
        s("Romanian deadlift", MuscleGroup.LEGS)
        s("Barbell row", MuscleGroup.BACK)
        s("Dumbbell row", MuscleGroup.BACK)
        s("Seated cable row", MuscleGroup.BACK)
        s("Lat pulldown", MuscleGroup.BACK)
        s("Pull-up", MuscleGroup.BACK)
        s("Chin-up", MuscleGroup.BACK)
        s("T-bar row", MuscleGroup.BACK)
        s("Face pull", MuscleGroup.SHOULDERS)

        s("Overhead press", MuscleGroup.SHOULDERS)
        s("Dumbbell shoulder press", MuscleGroup.SHOULDERS)
        s("Lateral raise", MuscleGroup.SHOULDERS)
        s("Front raise", MuscleGroup.SHOULDERS)
        s("Rear delt fly", MuscleGroup.SHOULDERS)
        s("Upright row", MuscleGroup.SHOULDERS)
        s("Shrug", MuscleGroup.SHOULDERS)

        s("Barbell curl", MuscleGroup.BICEPS)
        s("Dumbbell curl", MuscleGroup.BICEPS)
        s("Hammer curl", MuscleGroup.BICEPS)
        s("Preacher curl", MuscleGroup.BICEPS)
        s("Cable curl", MuscleGroup.BICEPS)

        s("Triceps pushdown", MuscleGroup.TRICEPS)
        s("Overhead triceps extension", MuscleGroup.TRICEPS)
        s("Skull crusher", MuscleGroup.TRICEPS)
        s("Close-grip bench press", MuscleGroup.TRICEPS)

        s("Squat", MuscleGroup.LEGS)
        s("Front squat", MuscleGroup.LEGS)
        s("Leg press", MuscleGroup.LEGS)
        s("Hack squat", MuscleGroup.LEGS)
        s("Bulgarian split squat", MuscleGroup.LEGS)
        s("Lunge", MuscleGroup.LEGS)
        s("Leg extension", MuscleGroup.LEGS)
        s("Leg curl", MuscleGroup.LEGS)
        s("Calf raise", MuscleGroup.LEGS)
        s("Hip thrust", MuscleGroup.GLUTES)
        s("Glute bridge", MuscleGroup.GLUTES)
        s("Cable kickback", MuscleGroup.GLUTES)

        s("Plank", MuscleGroup.CORE)
        s("Crunch", MuscleGroup.CORE)
        s("Hanging leg raise", MuscleGroup.CORE)
        s("Cable crunch", MuscleGroup.CORE)
        s("Russian twist", MuscleGroup.CORE)
        s("Ab wheel rollout", MuscleGroup.CORE)

        c("Treadmill run")
        c("Outdoor run")
        c("Walk")
        c("Cycling")
        c("Stationary bike")
        c("Rowing machine")
        c("Elliptical")
        c("Stair climber")
        c("Jump rope")
        c("Swimming")
    }
}
