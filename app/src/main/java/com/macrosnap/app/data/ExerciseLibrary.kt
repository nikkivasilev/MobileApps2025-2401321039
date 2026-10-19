package com.macrosnap.app.data

import androidx.annotation.StringRes
import com.macrosnap.app.R
import com.macrosnap.app.data.local.ExerciseEntity
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup

/** Seeded once on first launch; the user adds anything missing from the Exercises screen. */
object ExerciseLibrary {

    /**
     * A default exercise. [name] is what gets stored (always English, so existing installs match),
     * [nameRes] is how it's shown in the UI language.
     */
    data class Seed(
        val name: String,
        @StringRes val nameRes: Int,
        val group: MuscleGroup,
        val type: ExerciseType,
    )

    val SEEDS: List<Seed> = buildList {
        fun s(name: String, nameRes: Int, group: MuscleGroup) = add(Seed(name, nameRes, group, ExerciseType.STRENGTH))
        fun c(name: String, nameRes: Int) = add(Seed(name, nameRes, MuscleGroup.CARDIO, ExerciseType.CARDIO))

        s("Bench press", R.string.exercise_bench_press, MuscleGroup.CHEST)
        s("Incline bench press", R.string.exercise_incline_bench_press, MuscleGroup.CHEST)
        s("Dumbbell bench press", R.string.exercise_dumbbell_bench_press, MuscleGroup.CHEST)
        s("Incline dumbbell press", R.string.exercise_incline_dumbbell_press, MuscleGroup.CHEST)
        s("Chest fly", R.string.exercise_chest_fly, MuscleGroup.CHEST)
        s("Cable crossover", R.string.exercise_cable_crossover, MuscleGroup.CHEST)
        s("Push-up", R.string.exercise_push_up, MuscleGroup.CHEST)
        s("Dip", R.string.exercise_dip, MuscleGroup.CHEST)

        s("Deadlift", R.string.exercise_deadlift, MuscleGroup.BACK)
        s("Romanian deadlift", R.string.exercise_romanian_deadlift, MuscleGroup.LEGS)
        s("Barbell row", R.string.exercise_barbell_row, MuscleGroup.BACK)
        s("Dumbbell row", R.string.exercise_dumbbell_row, MuscleGroup.BACK)
        s("Seated cable row", R.string.exercise_seated_cable_row, MuscleGroup.BACK)
        s("Lat pulldown", R.string.exercise_lat_pulldown, MuscleGroup.BACK)
        s("Pull-up", R.string.exercise_pull_up, MuscleGroup.BACK)
        s("Chin-up", R.string.exercise_chin_up, MuscleGroup.BACK)
        s("T-bar row", R.string.exercise_t_bar_row, MuscleGroup.BACK)
        s("Face pull", R.string.exercise_face_pull, MuscleGroup.SHOULDERS)

        s("Overhead press", R.string.exercise_overhead_press, MuscleGroup.SHOULDERS)
        s("Dumbbell shoulder press", R.string.exercise_dumbbell_shoulder_press, MuscleGroup.SHOULDERS)
        s("Lateral raise", R.string.exercise_lateral_raise, MuscleGroup.SHOULDERS)
        s("Front raise", R.string.exercise_front_raise, MuscleGroup.SHOULDERS)
        s("Rear delt fly", R.string.exercise_rear_delt_fly, MuscleGroup.SHOULDERS)
        s("Upright row", R.string.exercise_upright_row, MuscleGroup.SHOULDERS)
        s("Shrug", R.string.exercise_shrug, MuscleGroup.SHOULDERS)

        s("Barbell curl", R.string.exercise_barbell_curl, MuscleGroup.BICEPS)
        s("Dumbbell curl", R.string.exercise_dumbbell_curl, MuscleGroup.BICEPS)
        s("Hammer curl", R.string.exercise_hammer_curl, MuscleGroup.BICEPS)
        s("Preacher curl", R.string.exercise_preacher_curl, MuscleGroup.BICEPS)
        s("Cable curl", R.string.exercise_cable_curl, MuscleGroup.BICEPS)

        s("Triceps pushdown", R.string.exercise_triceps_pushdown, MuscleGroup.TRICEPS)
        s("Overhead triceps extension", R.string.exercise_overhead_triceps_extension, MuscleGroup.TRICEPS)
        s("Skull crusher", R.string.exercise_skull_crusher, MuscleGroup.TRICEPS)
        s("Close-grip bench press", R.string.exercise_close_grip_bench_press, MuscleGroup.TRICEPS)

        s("Squat", R.string.exercise_squat, MuscleGroup.LEGS)
        s("Front squat", R.string.exercise_front_squat, MuscleGroup.LEGS)
        s("Leg press", R.string.exercise_leg_press, MuscleGroup.LEGS)
        s("Hack squat", R.string.exercise_hack_squat, MuscleGroup.LEGS)
        s("Bulgarian split squat", R.string.exercise_bulgarian_split_squat, MuscleGroup.LEGS)
        s("Lunge", R.string.exercise_lunge, MuscleGroup.LEGS)
        s("Leg extension", R.string.exercise_leg_extension, MuscleGroup.LEGS)
        s("Leg curl", R.string.exercise_leg_curl, MuscleGroup.LEGS)
        s("Calf raise", R.string.exercise_calf_raise, MuscleGroup.LEGS)
        s("Hip thrust", R.string.exercise_hip_thrust, MuscleGroup.GLUTES)
        s("Glute bridge", R.string.exercise_glute_bridge, MuscleGroup.GLUTES)
        s("Cable kickback", R.string.exercise_cable_kickback, MuscleGroup.GLUTES)

        s("Plank", R.string.exercise_plank, MuscleGroup.CORE)
        s("Crunch", R.string.exercise_crunch, MuscleGroup.CORE)
        s("Hanging leg raise", R.string.exercise_hanging_leg_raise, MuscleGroup.CORE)
        s("Cable crunch", R.string.exercise_cable_crunch, MuscleGroup.CORE)
        s("Russian twist", R.string.exercise_russian_twist, MuscleGroup.CORE)
        s("Ab wheel rollout", R.string.exercise_ab_wheel_rollout, MuscleGroup.CORE)

        c("Treadmill run", R.string.exercise_treadmill_run)
        c("Outdoor run", R.string.exercise_outdoor_run)
        c("Walk", R.string.exercise_walk)
        c("Cycling", R.string.exercise_cycling)
        c("Stationary bike", R.string.exercise_stationary_bike)
        c("Rowing machine", R.string.exercise_rowing_machine)
        c("Elliptical", R.string.exercise_elliptical)
        c("Stair climber", R.string.exercise_stair_climber)
        c("Jump rope", R.string.exercise_jump_rope)
        c("Swimming", R.string.exercise_swimming)
    }

    val DEFAULTS: List<ExerciseEntity> = SEEDS.map { seed ->
        ExerciseEntity(
            name = seed.name,
            muscleGroup = seed.group.name,
            type = seed.type.name,
            isCustom = false,
            defaultSets = if (seed.type == ExerciseType.CARDIO) 1 else Exercise.DEFAULT_SETS,
        )
    }
}
