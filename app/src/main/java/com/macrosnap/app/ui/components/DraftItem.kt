package com.macrosnap.app.ui.components

import com.macrosnap.app.model.Confidence
import com.macrosnap.app.model.FoodItem

/** An item in an editable list (meal review, recipe ingredients) that isn't persisted yet. */
data class DraftItem(
    val id: Long,
    val item: FoodItem,
    val confidence: Confidence? = null,
    /** Set for items that came from a recipe, so the card can step in servings as well as grams. */
    val servingGrams: Double? = null,
    /** Set for items the photo analysis produced, so a re-analysis replaces only those. */
    val fromPhoto: Boolean = false,
) {
    val servings: Double? get() = servingGrams?.takeIf { it > 0 }?.let { item.grams / it }
}

sealed interface EditorTarget {
    data object NewItem : EditorTarget
    data class Existing(val draft: DraftItem) : EditorTarget
}
