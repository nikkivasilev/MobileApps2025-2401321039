package com.macrosnap.app.ui.review

import com.macrosnap.app.R
import com.macrosnap.app.data.MicrosJson
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.ui.UiText
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
    val recipeServing: RecipeServing? = null,
    val labelWarnings: List<SavedWarning> = emptyList(),
    val microsNote: String? = null,
    val hasAnalyzed: Boolean,
    val savedForLater: Boolean = false,
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

/** A label warning by a stable key: resource ids may change between builds, so they're never stored. */
@Serializable
private data class SavedWarning(
    val key: String,
    val args: List<String>,
)

private val WARNING_KEYS = mapOf(
    R.string.review_warning_serving to "serving",
    R.string.review_warning_serving_kcal to "serving_kcal",
    R.string.review_warning_kj to "kj",
    R.string.review_warning_energy_conflict to "energy_conflict",
    R.string.review_warning_energy_conflict_macros to "energy_conflict_macros",
    R.string.review_warning_impossible_kcal to "impossible_kcal",
    R.string.review_warning_macros_mismatch to "macros_mismatch",
)

private fun UiText.toSavedWarning(): SavedWarning? {
    val res = this as? UiText.Res ?: return null
    return WARNING_KEYS[res.id]?.let { key -> SavedWarning(key, res.args.map { it.toString() }) }
}

private fun SavedWarning.toUiText(): UiText? =
    WARNING_KEYS.entries.firstOrNull { it.value == key }?.let { UiText.Res(it.key, args) }

// A failure's details aren't kept; after a restore it reads as a generic error.
private fun MicrosNote.toKey(): String? = when (this) {
    MicrosNote.Estimating -> null
    MicrosNote.FromLabelAndGemini -> "label_and_gemini"
    MicrosNote.FromGemini -> "gemini"
    MicrosNote.NoProductName -> "no_product_name"
    is MicrosNote.Failed -> if (error == DataError.NoApiKey) "no_api_key" else "failed"
}

private fun microsNoteFromKey(key: String?): MicrosNote? = when (key) {
    "label_and_gemini" -> MicrosNote.FromLabelAndGemini
    "gemini" -> MicrosNote.FromGemini
    "no_product_name" -> MicrosNote.NoProductName
    "no_api_key" -> MicrosNote.Failed(DataError.NoApiKey)
    "failed" -> MicrosNote.Failed(DataError.Unexpected(null))
    else -> null
}

private val json = Json { ignoreUnknownKeys = true }

internal fun ReviewUiState.toSavedJson(): String = json.encodeToString(
    SavedReview(
        hint = hint,
        describeInput = describeInput,
        mealType = mealType.name,
        notes = notes,
        recipeServing = recipeServing,
        labelWarnings = labelWarnings.mapNotNull { it.toSavedWarning() },
        // An estimate still running won't be after a restore, so don't claim it is.
        microsNote = microsNote?.toKey(),
        hasAnalyzed = hasAnalyzed,
        savedForLater = savedForLater,
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
    } catch (ignored: SerializationException) {
        return null
    } catch (ignored: IllegalArgumentException) {
        return null
    }
    return ReviewUiState(
        hint = saved.hint,
        describeInput = saved.describeInput,
        mealType = MealType.entries.firstOrNull { it.name == saved.mealType } ?: MealType.SNACK,
        photoPath = photoPath,
        notes = saved.notes,
        recipeServing = saved.recipeServing,
        labelWarnings = saved.labelWarnings.mapNotNull { it.toUiText() },
        microsNote = microsNoteFromKey(saved.microsNote),
        hasAnalyzed = saved.hasAnalyzed,
        savedForLater = saved.savedForLater,
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
