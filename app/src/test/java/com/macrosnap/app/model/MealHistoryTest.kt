package com.macrosnap.app.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealHistoryTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_000 * day
    private var nextId = 1L

    private fun entry(
        name: String,
        grams: Double,
        daysAgo: Int,
        meal: MealType = MealType.LUNCH,
        kcal: Double = 100.0,
        minute: Int = 0,
    ) = FoodEntry(
        id = nextId++,
        date = LocalDate.ofEpochDay(1_000L - daysAgo),
        createdAt = now - daysAgo * day + minute * 60_000L,
        mealType = meal,
        item = FoodItem(name, grams, Macros(kcal, 10.0, 10.0, 5.0)),
        source = EntrySource.TEXT,
        photoPath = null,
    )

    private fun lunch(daysAgo: Int, vararg foods: Pair<String, Double>) =
        foods.mapIndexed { index, (name, grams) -> entry(name, grams, daysAgo, minute = index) }

    @Test
    fun `the same foods on two days make a meal, in any order and case`() {
        val entries = lunch(3, "Chicken" to 150.0, "Rice" to 200.0) + lunch(1, "rice" to 200.0, " chicken " to 150.0)

        val meal = MealHistory.build(entries, now = now).single()

        assertEquals(2, meal.timesLogged)
        assertEquals(listOf("rice", "chicken"), meal.items.map { it.name.trim().lowercase() })
    }

    @Test
    fun `a meal eaten once or a single food is not listed`() {
        val entries = lunch(3, "Chicken" to 150.0, "Rice" to 200.0) +
            lunch(2, "Soup" to 300.0) + lunch(1, "Soup" to 300.0)

        assertTrue(MealHistory.build(entries, now = now).isEmpty())
    }

    @Test
    fun `an extra food makes a different meal`() {
        val entries = lunch(4, "Chicken" to 150.0, "Rice" to 200.0) +
            lunch(3, "Chicken" to 150.0, "Rice" to 200.0, "Apple" to 120.0)

        assertTrue(MealHistory.build(entries, now = now).isEmpty())
    }

    @Test
    fun `different meals of the same day are separate occasions`() {
        val entries = listOf(
            entry("Oats", 60.0, 2, MealType.BREAKFAST),
            entry("Milk", 200.0, 2, MealType.BREAKFAST),
            entry("Chicken", 150.0, 2),
            entry("Rice", 200.0, 2),
            entry("Oats", 60.0, 1, MealType.BREAKFAST),
            entry("Milk", 200.0, 1, MealType.BREAKFAST),
        )

        val meal = MealHistory.build(entries, now = now).single()

        assertEquals(mealKey(listOf("oats", "milk")), meal.key)
    }

    @Test
    fun `each food gets its usual amount in the meal and its newest nutrition`() {
        val entries = lunch(5, "Chicken" to 150.0, "Rice" to 200.0) +
            lunch(4, "Chicken" to 150.0, "Rice" to 300.0) +
            listOf(entry("Chicken", 250.0, 1, kcal = 120.0), entry("Rice", 200.0, 1, minute = 1))

        val chicken = MealHistory.build(entries, now = now).single().items.first { it.name == "Chicken" }

        assertEquals(150.0, chicken.grams, 0.0)
        assertEquals(120.0, chicken.per100g.kcal, 0.0)
    }

    @Test
    fun `a second helping counts as one larger amount`() {
        val entries = lunch(2, "Chicken" to 150.0, "Rice" to 200.0) +
            lunch(1, "Chicken" to 150.0, "Rice" to 100.0, "Rice" to 100.0)

        val meal = MealHistory.build(entries, now = now).single()

        assertEquals(2, meal.items.size)
        assertEquals(200.0, meal.items.first { it.name == "Rice" }.grams, 0.0)
    }

    @Test
    fun `often and recently eaten meals come first`() {
        val usual = (1..4).flatMap { lunch(it, "Chicken" to 150.0, "Rice" to 200.0) }
        val old = listOf(200, 201).flatMap { daysAgo ->
            listOf(entry("Oats", 60.0, daysAgo, MealType.BREAKFAST), entry("Milk", 200.0, daysAgo, MealType.BREAKFAST))
        }

        val meals = MealHistory.build(old + usual, now = now)

        assertEquals(mealKey(listOf("chicken", "rice")), meals.first().key)
    }

    @Test
    fun `a hidden meal stays hidden until it is eaten again`() {
        val entries = lunch(3, "Chicken" to 150.0, "Rice" to 200.0) + lunch(2, "Chicken" to 150.0, "Rice" to 200.0)
        val key = mealKey(listOf("chicken", "rice"))
        val hidden = mapOf(key to now - day)

        assertTrue(MealHistory.build(entries, hidden, now).isEmpty())

        val eatenAgain = entries + lunch(0, "Chicken" to 150.0, "Rice" to 200.0)
        assertEquals(3, MealHistory.build(eatenAgain, hidden, now + day).single().timesLogged)
    }
}
