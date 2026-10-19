package com.macrosnap.app.ui.today

import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.PastMeal
import com.macrosnap.app.model.PendingPhoto
import com.macrosnap.app.model.Recipe
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.model.SupplementStatus
import com.macrosnap.app.model.WaterDay
import com.macrosnap.app.model.add
import com.macrosnap.app.model.sum
import com.macrosnap.app.model.sumMicros
import com.macrosnap.app.ui.UiText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class TodayUiState(
    val date: LocalDate,
    val goals: Goals = Goals.DEFAULT,
    val nutrientTargets: Micros = emptyMap(),
    val waterGoalMl: Int = 2500,
    val sleepGoalHours: Double = 8.0,
    val entries: List<FoodEntry> = emptyList(),
    val supplements: List<SupplementStatus> = emptyList(),
    val water: WaterDay? = null,
    val sleep: SleepLog? = null,
    val recipes: List<Recipe> = emptyList(),
    val viewing: FoodEntry? = null,
    val editing: FoodEntry? = null,
    val showSleepSheet: Boolean = false,
    val workoutBurnKcal: Int = 0,
    val addWorkoutBurn: Boolean = false,
    val pastFoods: List<PastFood> = emptyList(),
    val pastMeals: List<PastMeal> = emptyList(),
    /** Calories eaten on each day of [weekDays] that has entries. */
    val weekKcal: Map<LocalDate, Double> = emptyMap(),
    /** Photos whose analysis failed while Gemini was unavailable, from any day, oldest first. */
    val pendingPhotos: List<PendingPhoto> = emptyList(),
) {
    /** Monday to Sunday of the week holding [date]. */
    val weekDays: List<LocalDate> get() = weekStart(date).let { start -> (0L..6L).map(start::plusDays) }
    /** The goal the ring and "left" number use; workouts extend it only when the user opted in. */
    val effectiveKcalGoal: Int get() = goals.kcal + if (addWorkoutBurn) workoutBurnKcal else 0
    val totals: Macros get() = entries.map { it.item.total }.sum()
    val nutrientTotals: Micros
        get() = entries.map { it.item.totalMicros }.sumMicros().add(supplements.map { it.micros }.sumMicros())
    val entriesByMeal: Map<MealType, List<FoodEntry>> get() = entries.groupBy { it.mealType }.toSortedMap()
    val isToday: Boolean get() = date == LocalDate.now()
}

fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

sealed interface TodayEffect {
    data class OpenPhotoReview(val epochDay: Long, val photoPath: String) : TodayEffect
    data class ShowMessage(val message: UiText) : TodayEffect
}
