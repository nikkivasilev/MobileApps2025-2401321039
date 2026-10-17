package com.macrosnap.app.data.settings

import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Profile

data class AppSettings(
    val apiKey: String,
    val models: List<String>,
    val goals: Goals,
    val profile: Profile?,
    /** Only the targets the user changed; everything else falls back to the RDA for their sex. */
    val nutrientOverrides: Micros,
    val waterGoalMl: Int,
    val sleepGoalHours: Double,
    val restSeconds: Int,
    /** When on, Today's calorie goal grows by the estimated burn of that day's workouts. */
    val addWorkoutBurn: Boolean,
    /** Food keys hidden from "Your foods", with when; logging one again after that brings it back. */
    val hiddenFoods: Map<String, Long> = emptyMap(),
    /** Meal keys hidden from "Your meals", with when; eating one again after that brings it back. */
    val hiddenMeals: Map<String, Long> = emptyMap(),
) {
    val nutrientTargets: Micros get() = Nutrient.defaultTargets(profile?.sex) + nutrientOverrides

    companion object {
        const val DEFAULT_WATER_GOAL_ML = 2500
        const val DEFAULT_SLEEP_GOAL_HOURS = 8.0
        const val DEFAULT_REST_SECONDS = 90

        /**
         * Tried in order. The free tier gives the full Flash model only a small daily allowance,
         * so the Flash-Lite models (hundreds of requests a day) back it up when it runs out.
         */
        val DEFAULT_MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite")
    }
}
