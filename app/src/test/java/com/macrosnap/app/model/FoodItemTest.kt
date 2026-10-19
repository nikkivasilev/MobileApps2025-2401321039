package com.macrosnap.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodItemTest {

    private val oats = Macros(kcal = 380.0, protein = 13.0, carbs = 60.0, fat = 7.0)

    @Test
    fun `totals scale per 100 g values by grams eaten`() {
        val total = FoodItem("Oats", 50.0, oats).total
        assertEquals(190.0, total.kcal, 1e-9)
        assertEquals(6.5, total.protein, 1e-9)
        assertEquals(30.0, total.carbs, 1e-9)
        assertEquals(3.5, total.fat, 1e-9)
    }

    @Test
    fun `exactly 100 g gives the per 100 g values`() {
        assertEquals(oats, FoodItem("Oats", 100.0, oats).total)
    }

    @Test
    fun `zero grams gives zero totals`() {
        assertEquals(Macros.ZERO, FoodItem("Oats", 0.0, oats).total)
    }

    @Test
    fun `micros scale with grams and unknown nutrients stay unknown`() {
        val item = FoodItem("Oats", 250.0, oats, mapOf(Nutrient.IRON to 4.0, Nutrient.MAGNESIUM to 130.0))
        val micros = item.totalMicros
        assertEquals(setOf(Nutrient.IRON, Nutrient.MAGNESIUM), micros.keys)
        assertEquals(10.0, micros.getValue(Nutrient.IRON), 1e-9)
        assertEquals(325.0, micros.getValue(Nutrient.MAGNESIUM), 1e-9)
    }

    @Test
    fun `no micros by default`() {
        assertTrue(FoodItem("Oats", 50.0, oats).totalMicros.isEmpty())
    }
}
