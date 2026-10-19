package com.macrosnap.app.ui.review

import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.ui.UiText
import com.macrosnap.app.ui.components.DraftItem
import com.macrosnap.app.ui.userText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewUiStateTest {

    private val loading = ReviewStatus.Loading(UiText.Raw("Analyzing"))
    private val rice = DraftItem(1, FoodItem("Rice", 200.0, Macros(130.0, 2.7, 28.0, 0.3)))
    private val chicken = DraftItem(2, FoodItem("Chicken", 150.0, Macros(165.0, 31.0, 0.0, 3.6)))

    private fun past(name: String, timesLogged: Int = 1) =
        PastFood(name.lowercase(), FoodItem(name, 100.0, Macros(50.0, 1.0, 1.0, 1.0)), timesLogged, 0)

    private fun state(
        items: List<DraftItem> = listOf(rice),
        photoPath: String? = null,
        status: ReviewStatus = ReviewStatus.Ready,
        hasAnalyzed: Boolean = false,
        isSaving: Boolean = false,
    ) = ReviewUiState(
        mealType = MealType.LUNCH,
        items = items,
        photoPath = photoPath,
        status = status,
        hasAnalyzed = hasAnalyzed,
        isSaving = isSaving,
    )

    @Test
    fun `totals scale each item's per-100 g values by its grams`() {
        val totals = state(items = listOf(rice, chicken)).totals

        assertEquals(260.0 + 247.5, totals.kcal, 1e-9)
        assertEquals(5.4 + 46.5, totals.protein, 1e-9)
        assertEquals(56.0, totals.carbs, 1e-9)
        assertEquals(0.6 + 5.4, totals.fat, 1e-9)
        assertEquals(0.0, state(items = emptyList()).totals.kcal, 0.0)
    }

    @Test
    fun `a photo waits for its first analysis until it is sent`() {
        assertTrue(state(photoPath = "p.jpg").awaitingFirstAnalysis)
        assertFalse(state(photoPath = "p.jpg", status = loading).awaitingFirstAnalysis)
        assertFalse(state(photoPath = "p.jpg", hasAnalyzed = true).awaitingFirstAnalysis)
        assertFalse(state(photoPath = null).awaitingFirstAnalysis)
    }

    @Test
    fun `saving needs items and nothing in progress`() {
        assertTrue(state().canSave)
        assertTrue(state(status = ReviewStatus.Failed(DataError.Network)).canSave)
        assertFalse(state(items = emptyList()).canSave)
        assertFalse(state(status = loading).canSave)
        assertFalse(state(isSaving = true).canSave)
    }

    @Test
    fun `only a photo that is not loading can be re-analysed`() {
        assertTrue(state(photoPath = "p.jpg", hasAnalyzed = true).canReanalyze)
        assertFalse(state(photoPath = "p.jpg", status = loading).canReanalyze)
        assertFalse(state(photoPath = null).canReanalyze)
    }

    @Test
    fun `describe suggestions match the segment being typed`() {
        val foods = listOf(past("Oat milk"), past("Milk"), past("Bread"))
        val base = state().copy(pastFoods = foods)

        assertEquals(
            listOf("Milk", "Oat milk"),
            base.copy(describeInput = "2 eggs, 250 ml mil").describeSuggestions.map { it.item.name },
        )
        assertEquals(emptyList<PastFood>(), base.copy(describeInput = "bread, m").describeSuggestions)
        assertEquals(emptyList<PastFood>(), base.copy(describeInput = "").describeSuggestions)
    }

    @Test
    fun `failed status uses the error's own text unless the screen gives one`() {
        assertEquals(DataError.Network.userText(), ReviewStatus.Failed(DataError.Network).message)
        val custom = UiText.Raw("No food found")
        assertEquals(custom, ReviewStatus.Failed(DataError.Network, custom).message)
    }
}
