package com.macrosnap.app.ui

import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object WorkoutsRoute

@Serializable
data object RecipesRoute

@Serializable
data object ProgressRoute

@Serializable
data object SettingsRoute

/** recipeId 0 creates a new recipe. */
@Serializable
data class RecipeEditorRoute(val recipeId: Long = 0)

/** supplementId 0 creates a new supplement. */
@Serializable
data class SupplementEditorRoute(val supplementId: Long = 0)

@Serializable
data class WorkoutRoute(val workoutId: Long)

@Serializable
data object ExercisesRoute

/** routineId 0 creates a new routine. */
@Serializable
data class RoutineEditorRoute(val routineId: Long = 0)

/**
 * With none of photo / barcode / recipe / past food set, the review screen opens as a manual entry:
 * a blank item editor, or just the describe-in-words field when [describe] is true, pre-filled
 * with [describeText] if given.
 */
@Serializable
data class ReviewRoute(
    val epochDay: Long,
    val photoPath: String? = null,
    val barcode: String? = null,
    val recipeId: Long? = null,
    val describe: Boolean = false,
    /** A food from "Your foods", by [com.macrosnap.app.model.foodKey]. */
    val pastFoodKey: String? = null,
    val describeText: String? = null,
    /** [photoPath] was kept after a failed analysis; send it again straight away with its saved hint. */
    val resumePending: Boolean = false,
)
