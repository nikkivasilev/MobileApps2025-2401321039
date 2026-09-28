package com.macrosnap.app.ui.review

import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.suggestionsFor
import com.macrosnap.app.model.sum
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.DraftItem
import com.macrosnap.app.ui.components.EditorTarget

data class ReviewUiState(
    val hint: String = "",
    val describeInput: String = "",
    val mealType: MealType,
    val items: List<DraftItem> = emptyList(),
    val photoPath: String? = null,
    val notes: String? = null,
    /** Problems spotted in a barcode's database listing, shown so the user checks the label. */
    val labelWarnings: List<String> = emptyList(),
    /** Where a scanned product's vitamins and minerals came from, or why they're missing. */
    val microsNote: String? = null,
    val isEstimatingMicros: Boolean = false,
    val pastFoods: List<PastFood> = emptyList(),
    val status: ReviewStatus = ReviewStatus.Ready,
    val describeStatus: DescribeStatus = DescribeStatus.Idle,
    val editor: EditorTarget? = null,
    val isSaving: Boolean = false,
    /** False until the photo has been sent once; the first send waits for the user's hint. */
    val hasAnalyzed: Boolean = false,
    /** The photo is kept on Today's waiting list, so leaving without logging doesn't delete it. */
    val savedForLater: Boolean = false,
) {
    val awaitingFirstAnalysis: Boolean get() = photoPath != null && !hasAnalyzed && status !is ReviewStatus.Loading
    val totals: Macros get() = items.map { it.item.total }.sum()
    val canSave: Boolean get() = items.isNotEmpty() && status !is ReviewStatus.Loading && !isSaving
    val canReanalyze: Boolean get() = photoPath != null && status !is ReviewStatus.Loading

    /** Foods logged before that match what's being typed in the describe box: added without the AI. */
    val describeSuggestions: List<PastFood> get() = pastFoods.suggestionsFor(describeInput)
}

sealed interface ReviewStatus {
    data class Loading(val label: String) : ReviewStatus
    data object Ready : ReviewStatus
    data class Failed(val error: DataError) : ReviewStatus
}

sealed interface ReviewEffect {
    data class ShowMessage(val message: String) : ReviewEffect
    data object Saved : ReviewEffect
}
