package com.macrosnap.app.ui.review

import com.macrosnap.app.data.MicrosJson
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.ui.components.DraftItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The part of a review worth surviving process death: Android may kill the app while the user
 * checks another app mid-review, and re-running a photo analysis costs free-tier quota.
 */
@Serializable
private data class SavedReview(
    val hint: String,
    val describeInput: String,
    val mealType: String,
    val notes: String?,
    val hasAnalyzed: Boolean,
    val items: List<SavedDraftItem>,
)

@Serializable
private data class SavedDraftItem(
    val id: Long,
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val micros: String?,
    val confidence: String?,
    val servingGrams: Double?,
    val fromPhoto: Boolean,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun ReviewUiState.toSavedJson(): String = json.encodeToString(
    SavedReview(
        hint = hint,
        describeInput = describeInput,
        mealType = mealType.name,
        notes = notes,
        hasAnalyzed = hasAnalyzed,
        items = items.map { draft ->
            SavedDraftItem(
                id = draft.id,
                name = draft.item.name,
                grams = draft.item.grams,
                kcal = draft.item.per100g.kcal,
                protein = draft.item.per100g.protein,
                carbs = draft.item.per100g.carbs,
                fat = draft.item.per100g.fat,
                micros = MicrosJson.encode(draft.item.micros100g),
                confidence = draft.confidence?.name,
                servingGrams = draft.servingGrams,
                fromPhoto = draft.fromPhoto,
            )
        },
    ),
)

/** Returns null when there's nothing usable saved, so the screen starts fresh. */
internal fun restoreReview(text: String?, photoPath: String?): ReviewUiState? {
    if (text == null) return null
    val saved = try {
        json.decodeFromString<SavedReview>(text)
    } catch (e: SerializationException) {
        return null
    } catch (e: IllegalArgumentException) {
        return null
    }
    return ReviewUiState(
        hint = saved.hint,
        describeInput = saved.describeInput,
        mealType = MealType.entries.firstOrNull { it.name == saved.mealType } ?: MealType.SNACK,
        photoPath = photoPath,
        notes = saved.notes,
        hasAnalyzed = saved.hasAnalyzed,
        items = saved.items.map { item ->
            DraftItem(
                id = item.id,
                item = FoodItem(
                    name = item.name,
                    grams = item.grams,
                    per100g = Macros(item.kcal, item.protein, item.carbs, item.fat),
                    micros100g = MicrosJson.decode(item.micros),
                ),
                confidence = Confidence.entries.firstOrNull { it.name == item.confidence },
                servingGrams = item.servingGrams,
                fromPhoto = item.fromPhoto,
            )
        },
    )
}
