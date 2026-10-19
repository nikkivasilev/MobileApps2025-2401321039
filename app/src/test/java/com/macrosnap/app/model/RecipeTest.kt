package com.macrosnap.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTest {

    private val rice = FoodItem(
        "Rice",
        200.0,
        Macros(kcal = 360.0, protein = 7.0, carbs = 80.0, fat = 1.0),
        mapOf(Nutrient.MAGNESIUM to 25.0),
    )
    private val chicken = FoodItem(
        "Chicken",
        300.0,
        Macros(kcal = 120.0, protein = 23.0, carbs = 0.0, fat = 2.0),
        mapOf(Nutrient.MAGNESIUM to 30.0, Nutrient.VITAMIN_B6 to 0.6),
    )

    private fun recipe(
        ingredients: List<FoodItem> = listOf(rice, chicken),
        cookedWeightGrams: Double? = null,
        servings: Int = 1,
    ) = Recipe(1, "Chicken rice", ingredients, cookedWeightGrams, servings)

    @Test
    fun `total adds up all ingredients`() {
        val total = recipe().total
        assertEquals(720.0 + 360.0, total.kcal, 1e-9)
        assertEquals(14.0 + 69.0, total.protein, 1e-9)
        assertEquals(160.0, total.carbs, 1e-9)
        assertEquals(2.0 + 6.0, total.fat, 1e-9)
    }

    @Test
    fun `without cooked weight the raw ingredient weight is used`() {
        val recipe = recipe()
        assertEquals(500.0, recipe.ingredientWeight, 0.0)
        assertEquals(500.0, recipe.totalWeight, 0.0)
        assertEquals(1080.0 / 5, recipe.per100g.kcal, 1e-9)
    }

    @Test
    fun `cooked weight sets the per 100 g values so water loss concentrates them`() {
        val recipe = recipe(cookedWeightGrams = 400.0)
        assertEquals(400.0, recipe.totalWeight, 0.0)
        assertEquals(1080.0 / 4, recipe.per100g.kcal, 1e-9)
        assertEquals(83.0 / 4, recipe.per100g.protein, 1e-9)
    }

    @Test
    fun `micros are summed and scaled to 100 g of the finished dish`() {
        val recipe = recipe(cookedWeightGrams = 400.0)
        assertEquals(50.0 + 90.0, recipe.totalMicros.getValue(Nutrient.MAGNESIUM), 1e-9)
        assertEquals(1.8, recipe.totalMicros.getValue(Nutrient.VITAMIN_B6), 1e-9)
        assertEquals(35.0, recipe.micros100g.getValue(Nutrient.MAGNESIUM), 1e-9)
        assertEquals(0.45, recipe.micros100g.getValue(Nutrient.VITAMIN_B6), 1e-9)
    }

    @Test
    fun `servings split weight and macros evenly`() {
        val recipe = recipe(cookedWeightGrams = 900.0, servings = 3)
        assertEquals(300.0, recipe.servingGrams, 1e-9)
        assertEquals(360.0, recipe.perServing.kcal, 1e-9)
    }

    @Test
    fun `zero servings is treated as one`() {
        val recipe = recipe(servings = 0)
        assertEquals(500.0, recipe.servingGrams, 0.0)
        assertEquals(recipe.total, recipe.perServing)
    }

    @Test
    fun `an empty recipe has zero values instead of dividing by zero`() {
        val recipe = recipe(ingredients = emptyList())
        assertEquals(0.0, recipe.totalWeight, 0.0)
        assertEquals(Macros.ZERO, recipe.per100g)
        assertTrue(recipe.micros100g.isEmpty())
    }

    @Test
    fun `a portion eats the dish at its own per 100 g values`() {
        val recipe = recipe(cookedWeightGrams = 400.0)
        val portion = recipe.portion(200.0)
        assertEquals("Chicken rice", portion.name)
        assertEquals(200.0, portion.grams, 0.0)
        assertEquals(1080.0 / 2, portion.total.kcal, 1e-9)
        assertEquals(70.0, portion.totalMicros.getValue(Nutrient.MAGNESIUM), 1e-9)
    }

    @Test
    fun `eating the whole cooked dish gives the recipe total`() {
        val recipe = recipe(cookedWeightGrams = 430.0)
        val whole = recipe.portion(430.0).total
        assertEquals(recipe.total.kcal, whole.kcal, 1e-9)
        assertEquals(recipe.total.protein, whole.protein, 1e-9)
    }
}
