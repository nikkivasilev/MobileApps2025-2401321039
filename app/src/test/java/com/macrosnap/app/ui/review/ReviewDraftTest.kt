package com.macrosnap.app.ui.review

import com.macrosnap.app.R
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.ui.UiText
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.DraftItem
import com.macrosnap.app.ui.components.EditorTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewDraftTest {

    private val chicken = DraftItem(
        id = 7,
        item = FoodItem(
            name = "Grilled chicken breast",
            grams = 180.0,
            per100g = Macros(kcal = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6),
            micros100g = mapOf(Nutrient.VITAMIN_B6 to 0.6, Nutrient.CREATINE to 0.4),
        ),
        confidence = Confidence.HIGH,
        fromPhoto = true,
    )

    private val lasagna = DraftItem(
        id = 8,
        item = FoodItem(name = "Лазаня", grams = 350.0, per100g = Macros(135.0, 7.5, 12.0, 6.2)),
        servingGrams = 350.0,
    )

    private val fullState = ReviewUiState(
        hint = "half the sauce",
        describeInput = "2 eggs",
        mealType = MealType.DINNER,
        items = listOf(chicken, lasagna),
        notes = "Portion of rice is a guess.",
        recipeServing = RecipeServing(name = "Lasagna", servingGrams = 350.0, totalGrams = 2100.0, servings = 6),
        labelWarnings = listOf(
            UiText.Res(R.string.review_warning_kj, listOf("192", "45.9")),
            UiText.Res(R.string.review_warning_macros_mismatch),
        ),
        microsNote = MicrosNote.FromLabelAndGemini,
        hasAnalyzed = true,
        savedForLater = true,
    )

    private fun ReviewUiState.roundTrip(photoPath: String? = null): ReviewUiState? =
        restoreReview(toSavedJson(), photoPath)

    @Test
    fun `saved review comes back as it was`() {
        val restored = fullState.roundTrip(photoPath = "/photos/lunch.jpg")

        assertEquals(fullState.copy(photoPath = "/photos/lunch.jpg"), restored)
    }

    @Test
    fun `photo path comes from the caller, not the saved text`() {
        val saved = fullState.copy(photoPath = "/photos/old.jpg").toSavedJson()

        assertNull(restoreReview(saved, photoPath = null)?.photoPath)
        assertEquals("/photos/new.jpg", restoreReview(saved, photoPath = "/photos/new.jpg")?.photoPath)
    }

    @Test
    fun `screen state that only matters while open is not restored`() {
        val busy = fullState.copy(
            status = ReviewStatus.Loading(UiText.Raw("Analyzing")),
            describeStatus = DescribeStatus.Loading,
            editor = EditorTarget.Existing(chicken),
            isSaving = true,
            isEstimatingMicros = true,
        )

        val restored = busy.roundTrip()!!

        assertEquals(ReviewStatus.Ready, restored.status)
        assertEquals(DescribeStatus.Idle, restored.describeStatus)
        assertNull(restored.editor)
        assertEquals(false, restored.isSaving)
        assertEquals(false, restored.isEstimatingMicros)
        assertEquals(fullState.items, restored.items)
    }

    @Test
    fun `minimal review without items or extras round trips`() {
        val empty = ReviewUiState(mealType = MealType.BREAKFAST)

        assertEquals(empty, empty.roundTrip())
    }

    @Test
    fun `items without micros or confidence stay unknown`() {
        val plain = DraftItem(id = 1, item = FoodItem("Water", 250.0, Macros(0.0, 0.0, 0.0, 0.0)))

        val restored = ReviewUiState(mealType = MealType.SNACK, items = listOf(plain)).roundTrip()!!.items.single()

        assertEquals(plain, restored)
        assertTrue(restored.item.micros100g.isEmpty())
        assertNull(restored.confidence)
    }

    @Test
    fun `micros notes are restored except an estimate still running`() {
        fun MicrosNote?.restored() = fullState.copy(microsNote = this).roundTrip()!!.microsNote

        assertEquals(MicrosNote.FromGemini, MicrosNote.FromGemini.restored())
        assertEquals(MicrosNote.FromLabelAndGemini, MicrosNote.FromLabelAndGemini.restored())
        assertEquals(MicrosNote.NoProductName, MicrosNote.NoProductName.restored())
        assertEquals(MicrosNote.Failed(DataError.NoApiKey), MicrosNote.Failed(DataError.NoApiKey).restored())
        assertNull(MicrosNote.Estimating.restored())
        assertNull(null.restored())
    }

    @Test
    fun `a failed estimate keeps only that it failed`() {
        val restored = fullState.copy(microsNote = MicrosNote.Failed(DataError.RateLimited(daily = true))).roundTrip()

        assertEquals(MicrosNote.Failed(DataError.Unexpected(null)), restored?.microsNote)
    }

    @Test
    fun `label warnings keep their arguments as text and drop anything not a known warning`() {
        val state = fullState.copy(
            labelWarnings = listOf(
                UiText.Res(R.string.review_warning_serving, listOf(1000)),
                UiText.Raw("free text"),
                UiText.Res(R.string.review_warning_kj, listOf(192, 45.9)),
                UiText.Plural(R.string.review_warning_impossible_kcal, count = 2),
                UiText.Res(id = 0),
                UiText.Res(R.string.review_warning_energy_conflict_macros, listOf("330", "4")),
            ),
        )

        assertEquals(
            listOf(
                UiText.Res(R.string.review_warning_serving, listOf("1000")),
                UiText.Res(R.string.review_warning_kj, listOf("192", "45.9")),
                UiText.Res(R.string.review_warning_energy_conflict_macros, listOf("330", "4")),
            ),
            state.roundTrip()?.labelWarnings,
        )
    }

    @Test
    fun `every known label warning survives a restore`() {
        val warnings = listOf(
            R.string.review_warning_serving,
            R.string.review_warning_serving_kcal,
            R.string.review_warning_kj,
            R.string.review_warning_energy_conflict,
            R.string.review_warning_energy_conflict_macros,
            R.string.review_warning_impossible_kcal,
            R.string.review_warning_macros_mismatch,
        ).map { UiText.Res(it, listOf("1")) }

        assertEquals(warnings, fullState.copy(labelWarnings = warnings).roundTrip()?.labelWarnings)
    }

    @Test
    fun `nothing saved or unreadable text starts fresh`() {
        assertNull(restoreReview(null, photoPath = "/photos/a.jpg"))
        assertNull(restoreReview("", photoPath = null))
        assertNull(restoreReview("not json at all", photoPath = null))
        assertNull(restoreReview("[1, 2, 3]", photoPath = null))
        // Valid JSON from something else: the required fields are missing.
        assertNull(restoreReview("""{"hint": "x"}""", photoPath = null))
        // Right shape, wrong type.
        assertNull(
            restoreReview(
                """{"hint": "", "describeInput": "", "mealType": "LUNCH", "notes": null, "hasAnalyzed": "maybe",
                   "items": []}""",
                photoPath = null,
            ),
        )
    }

    @Test
    fun `unknown names fall back and unknown fields are ignored`() {
        val text = """
            {"hint": "", "describeInput": "", "mealType": "BRUNCH", "notes": null, "hasAnalyzed": false,
             "addedInVersion9": true, "microsNote": "something_new",
             "labelWarnings": [{"key": "future_warning", "args": []}],
             "items": [{"id": 3, "name": "Toast", "grams": 30, "kcal": 265, "protein": 9, "carbs": 49, "fat": 3.2,
                        "micros": "{\"IRON_MG\": 1, \"ZINC\": 0.8}", "confidence": "CERTAIN", "servingGrams": null,
                        "fromPhoto": false}]}
        """.trimIndent()

        val restored = restoreReview(text, photoPath = null)!!

        assertEquals(MealType.SNACK, restored.mealType)
        assertNull(restored.microsNote)
        assertTrue(restored.labelWarnings.isEmpty())
        assertEquals(false, restored.savedForLater)
        assertNull(restored.recipeServing)
        val toast = restored.items.single()
        assertNull(toast.confidence)
        assertEquals(mapOf(Nutrient.ZINC to 0.8), toast.item.micros100g)
        assertEquals(Macros(265.0, 9.0, 49.0, 3.2), toast.item.per100g)
    }
}
