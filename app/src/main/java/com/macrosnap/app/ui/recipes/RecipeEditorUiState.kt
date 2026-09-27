package com.macrosnap.app.ui.recipes

import com.macrosnap.app.model.Recipe
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.DraftItem
import com.macrosnap.app.ui.components.EditorTarget
import com.macrosnap.app.ui.toAmountOrNull

data class RecipeEditorUiState(
    val id: Long,
    val name: String = "",
    val servingsInput: String = "1",
    val cookedWeightInput: String = "",
    val ingredients: List<DraftItem> = emptyList(),
    val describeInput: String = "",

    val describeStatus: DescribeStatus = DescribeStatus.Idle,
    val editor: EditorTarget? = null,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
    val isExisting: Boolean get() = id != 0L
    val servings: Int? get() = servingsInput.trim().toIntOrNull()?.takeIf { it in 1..100 }
    val cookedWeight: Double? get() = cookedWeightInput.toAmountOrNull()?.takeIf { it > 0 }
    val cookedWeightValid: Boolean get() = cookedWeightInput.isBlank() || cookedWeight != null

    /** The recipe as currently entered, or null while it can't be saved. */
    val recipe: Recipe?
        get() {
            val servings = servings ?: return null
            if (name.isBlank() || ingredients.isEmpty() || !cookedWeightValid) return null
            return Recipe(id, name.trim(), ingredients.map { it.item }, cookedWeight, servings)
        }

    /** Totals shown while editing, even before the recipe is complete enough to save. */
    val preview: Recipe get() = Recipe(id, name, ingredients.map { it.item }, cookedWeight, servings ?: 1)

    val canSave: Boolean get() = recipe != null && !isSaving && describeStatus !is DescribeStatus.Loading
}

sealed interface RecipeEditorEffect {
    data object Close : RecipeEditorEffect
}
