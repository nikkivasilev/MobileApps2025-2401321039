package com.macrosnap.app.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodHistoryTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_000 * day
    private var nextId = 1L

    private fun entry(name: String, grams: Double, daysAgo: Int, kcal: Double = 64.0) = FoodEntry(
        id = nextId++,
        date = LocalDate.ofEpochDay(1_000L - daysAgo),
        createdAt = now - daysAgo * day,
        mealType = MealType.BREAKFAST,
        item = FoodItem(name, grams, Macros(kcal, 3.3, 4.8, 3.6)),
        source = EntrySource.TEXT,
        photoPath = null,
    )

    @Test
    fun `same food under different case and spacing is merged`() {
        val foods = FoodHistory.build(listOf(entry("Milk 3.5%", 250.0, 3), entry(" milk  3.5% ", 250.0, 1)), now = now)

        assertEquals(1, foods.size)
        assertEquals(2, foods.single().timesLogged)
        assertEquals(" milk  3.5% ", foods.single().item.name)
    }

    @Test
    fun `usual amount is the most common recent one, not a one-off`() {
        val foods = FoodHistory.build(
            listOf(entry("Milk", 250.0, 5), entry("Milk", 250.0, 4), entry("Milk", 500.0, 1)),
            now = now,
        )
        assertEquals(250.0, foods.single().item.grams, 0.0)
    }

    @Test
    fun `nutrition comes from the newest entry so corrections stick`() {
        val foods = FoodHistory.build(
            listOf(entry("Milk", 250.0, 5, kcal = 90.0), entry("Milk", 250.0, 1, kcal = 64.0)),
            now = now,
        )
        assertEquals(64.0, foods.single().item.per100g.kcal, 0.0)
    }

    @Test
    fun `frequent and recent foods rank first`() {
        val entries = List(5) {
            entry("Milk", 250.0, it)
        } + entry("Birthday cake", 120.0, 0) + List(5) { entry("Oats", 50.0, 200 + it) }
        val order = FoodHistory.build(entries, now = now).map { it.key }
        assertEquals(listOf("milk", "birthday cake", "oats"), order)
    }

    @Test
    fun `hidden food stays hidden until logged again`() {
        val entries = listOf(entry("Birthday cake", 120.0, 5))
        assertTrue(FoodHistory.build(entries, hidden = mapOf("birthday cake" to now - 1 * day), now = now).isEmpty())

        val loggedAgain = entries + entry("Birthday cake", 120.0, 0)
        assertEquals(
            1,
            FoodHistory.build(loggedAgain, hidden = mapOf("birthday cake" to now - 1 * day), now = now).size,
        )
    }

    @Test
    fun `matching ignores amounts and prefers names that start with the query`() {
        val foods = FoodHistory.build(
            listOf(entry("Chocolate milk", 250.0, 0), entry("Milk 3.5%", 250.0, 10)),
            now = now,
        )
        assertEquals(listOf("milk 3.5%", "chocolate milk"), foods.matching("250 ml milk").map { it.key })
        assertTrue(foods.matching("200 g").isEmpty())
        assertTrue(foods.matching("bread").isEmpty())
    }

    @Test
    fun `quantities are read in grams and millilitres, Bulgarian units included`() {
        assertEquals(250.0, parseQuantityGrams("250 ml milk")!!, 0.0)
        assertEquals(500.0, parseQuantityGrams("0,5 л мляко")!!, 0.0)
        assertEquals(120.0, parseQuantityGrams("кашкавал 120гр")!!, 0.0)
        assertNull(parseQuantityGrams("2 eggs"))
        assertNull(parseQuantityGrams("glass of milk"))
    }

    @Test
    fun `accepting a suggestion uses the typed amount and keeps earlier foods for the AI`() {
        val milk = FoodHistory.build(listOf(entry("Milk", 250.0, 1)), now = now).single()

        val (item, remaining) = acceptSuggestion("2 eggs, 300 ml milk", milk)
        assertEquals(300.0, item.grams, 0.0)
        assertEquals("2 eggs", remaining)

        val (usual, rest) = acceptSuggestion("mil", milk)
        assertEquals(250.0, usual.grams, 0.0)
        assertEquals("", rest)
    }

    @Test
    fun `suggestions wait for two letters of the current segment`() {
        val foods = FoodHistory.build(listOf(entry("Milk", 250.0, 1)), now = now)
        assertTrue(foods.suggestionsFor("2 eggs, m").isEmpty())
        assertEquals(1, foods.suggestionsFor("2 eggs, mi").size)
    }
}
