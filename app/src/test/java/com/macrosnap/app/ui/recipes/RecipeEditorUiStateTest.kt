package com.macrosnap.app.ui.recipes

import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.Recipe
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.DraftItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEditorUiStateTest {

    private val pasta = FoodItem("Pasta", 400.0, Macros(350.0, 12.0, 70.0, 1.5))
    private val sauce = FoodItem("Tomato sauce", 300.0, Macros(30.0, 1.5, 5.0, 0.2))

    private val filled = RecipeEditorUiState(
        id = 0,
        name = " Pasta al pomodoro ",
        servingsInput = " 4 ",
        cookedWeightInput = "",
        ingredients = listOf(DraftItem(1, pasta), DraftItem(2, sauce)),
    )

    @Test
    fun `an id of zero means a new recipe`() {
        assertFalse(filled.isExisting)
        assertTrue(filled.copy(id = 9).isExisting)
    }

    @Test
    fun `servings must be a whole number from 1 to 100`() {
        assertEquals(4, filled.servings)
        assertEquals(100, filled.copy(servingsInput = "100").servings)
        assertNull(filled.copy(servingsInput = "0").servings)
        assertNull(filled.copy(servingsInput = "101").servings)
        assertNull(filled.copy(servingsInput = "").servings)
    }

    @Test
    fun `cooked weight is optional but must be positive when given`() {
        assertTrue(filled.cookedWeightValid)
        assertNull(filled.cookedWeight)

        val cooked = filled.copy(cookedWeightInput = "950,5")
        assertEquals(950.5, cooked.cookedWeight!!, 0.0)
        assertTrue(cooked.cookedWeightValid)

        assertFalse(filled.copy(cookedWeightInput = "0").cookedWeightValid)
        assertFalse(filled.copy(cookedWeightInput = "heavy").cookedWeightValid)
    }

    @Test
    fun `recipe is built from the trimmed name and the ingredients`() {
        assertEquals(Recipe(0, "Pasta al pomodoro", listOf(pasta, sauce), null, 4), filled.recipe)
        assertEquals(800.0, filled.copy(cookedWeightInput = "800").recipe!!.cookedWeightGrams!!, 0.0)
    }

    @Test
    fun `recipe is null until name, ingredients, servings and weight are all valid`() {
        assertNull(filled.copy(name = " ").recipe)
        assertNull(filled.copy(ingredients = emptyList()).recipe)
        assertNull(filled.copy(servingsInput = "0").recipe)
        assertNull(filled.copy(cookedWeightInput = "-5").recipe)
        assertFalse(filled.copy(name = "").canSave)
    }

    @Test
    fun `preview shows totals even while the recipe is incomplete`() {
        val draft = filled.copy(name = "", servingsInput = "", cookedWeightInput = "700")

        val preview = draft.preview
        assertNull(draft.recipe)
        assertEquals(1, preview.servings)
        assertEquals(1400.0 + 90.0, preview.total.kcal, 1e-9)
        assertEquals(1490.0 / 7, preview.per100g.kcal, 1e-9)
    }

    @Test
    fun `cannot save while saving or while a description is being analysed`() {
        assertTrue(filled.canSave)
        assertFalse(filled.copy(isSaving = true).canSave)
        assertFalse(filled.copy(describeStatus = DescribeStatus.Loading).canSave)
    }

    @Test
    fun `describe suggestions come from past foods matching the last segment`() {
        val basil = PastFood("basil", FoodItem("Basil", 5.0, Macros(23.0, 3.0, 3.0, 0.6)), 3, 0)
        val state = filled.copy(pastFoods = listOf(basil))

        assertEquals(listOf(basil), state.copy(describeInput = "salt, 10 g bas").describeSuggestions)
        assertTrue(state.copy(describeInput = "basil, salt").describeSuggestions.isEmpty())
    }
}
