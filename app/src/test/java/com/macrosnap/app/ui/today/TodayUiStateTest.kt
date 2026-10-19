package com.macrosnap.app.ui.today

import com.macrosnap.app.model.EntrySource
import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Supplement
import com.macrosnap.app.model.SupplementStatus
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUiStateTest {

    private val wednesday = LocalDate.of(2026, 10, 14)
    private var nextId = 1L

    private fun entry(meal: MealType, item: FoodItem) = FoodEntry(
        id = nextId++,
        date = wednesday,
        createdAt = 0,
        mealType = meal,
        item = item,
        source = EntrySource.MANUAL,
        photoPath = null,
    )

    private val oats = FoodItem(
        "Oats",
        50.0,
        Macros(380.0, 13.0, 60.0, 7.0),
        micros100g = mapOf(Nutrient.IRON to 4.0, Nutrient.MAGNESIUM to 130.0),
    )
    private val salmon = FoodItem(
        "Salmon",
        200.0,
        Macros(200.0, 20.0, 0.0, 13.0),
        micros100g = mapOf(Nutrient.VITAMIN_D to 10.0),
    )

    private fun supplement(micros: Map<Nutrient, Double>, taken: Int) = SupplementStatus(
        Supplement(1, "D3", "1 capsule", 1, micros, isActive = true),
        takenCount = taken,
        micros = if (taken > 0) micros else emptyMap(),
    )

    @Test
    fun `week runs Monday to Sunday around the day shown`() {
        val days = TodayUiState(date = wednesday).weekDays

        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 10, 12), days.first())
        assertEquals(LocalDate.of(2026, 10, 18), days.last())
        assertEquals(DayOfWeek.entries.toList(), days.map { it.dayOfWeek })
    }

    @Test
    fun `a Sunday belongs to the week that started six days earlier`() {
        val sunday = LocalDate.of(2026, 10, 18)

        assertEquals(LocalDate.of(2026, 10, 12), weekStart(sunday))
        assertEquals(sunday, weekStart(sunday.plusDays(1)).minusDays(1))
        assertEquals(LocalDate.of(2026, 10, 12), weekStart(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun `workout burn extends the calorie goal only when opted in`() {
        val state = TodayUiState(date = wednesday, goals = Goals(2000, 150, 200, 60), workoutBurnKcal = 350)

        assertEquals(2000, state.effectiveKcalGoal)
        assertEquals(2350, state.copy(addWorkoutBurn = true).effectiveKcalGoal)
    }

    @Test
    fun `totals add up every entry at its portion size`() {
        val state = TodayUiState(
            date = wednesday,
            entries = listOf(entry(MealType.BREAKFAST, oats), entry(MealType.DINNER, salmon)),
        )

        assertEquals(190.0 + 400.0, state.totals.kcal, 1e-9)
        assertEquals(6.5 + 40.0, state.totals.protein, 1e-9)
        assertEquals(30.0, state.totals.carbs, 1e-9)
        assertEquals(3.5 + 26.0, state.totals.fat, 1e-9)
    }

    @Test
    fun `nutrient totals combine food and supplements taken, leaving unknowns out`() {
        val state = TodayUiState(
            date = wednesday,
            entries = listOf(entry(MealType.BREAKFAST, oats), entry(MealType.DINNER, salmon)),
            supplements = listOf(supplement(mapOf(Nutrient.VITAMIN_D to 25.0), taken = 1)),
        )

        val totals = state.nutrientTotals
        assertEquals(2.0, totals.getValue(Nutrient.IRON), 1e-9)
        assertEquals(65.0, totals.getValue(Nutrient.MAGNESIUM), 1e-9)
        assertEquals(20.0 + 25.0, totals.getValue(Nutrient.VITAMIN_D), 1e-9)
        assertNull(totals[Nutrient.ZINC])
    }

    @Test
    fun `a supplement not taken adds nothing`() {
        val state = TodayUiState(
            date = wednesday,
            supplements = listOf(supplement(mapOf(Nutrient.ZINC to 15.0), taken = 0)),
        )

        assertTrue(state.nutrientTotals.isEmpty())
    }

    @Test
    fun `entries are grouped by meal in the order of the day`() {
        val state = TodayUiState(
            date = wednesday,
            entries = listOf(
                entry(MealType.SNACK, oats),
                entry(MealType.DINNER, salmon),
                entry(MealType.BREAKFAST, oats),
                entry(MealType.DINNER, oats),
            ),
        )

        val byMeal = state.entriesByMeal
        assertEquals(listOf(MealType.BREAKFAST, MealType.DINNER, MealType.SNACK), byMeal.keys.toList())
        assertEquals(listOf("Salmon", "Oats"), byMeal.getValue(MealType.DINNER).map { it.item.name })
    }

    @Test
    fun `is today follows the clock`() {
        assertTrue(TodayUiState(date = LocalDate.now()).isToday)
        assertFalse(TodayUiState(date = LocalDate.now().minusDays(1)).isToday)
    }
}
