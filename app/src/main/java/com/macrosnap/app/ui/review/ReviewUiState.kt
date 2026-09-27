package com.macrosnap.app.ui.review

import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
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
    val status: ReviewStatus = ReviewStatus.Ready,
    val describeStatus: DescribeStatus = DescribeStatus.Idle,
    val editor: EditorTarget? = null,
    val isSaving: Boolean = false,
    /** False until the photo has been sent once; the first send waits for the user's hint. */
    val hasAnalyzed: Boolean = false,
) {
    val awaitingFirstAnalysis: Boolean get() = photoPath != null && !hasAnalyzed && status !is ReviewStatus.Loading
    val totals: Macros get() = items.map { it.item.total }.sum()
    val canSave: Boolean get() = items.isNotEmpty() && status !is ReviewStatus.Loading && !isSaving
    val canReanalyze: Boolean get() = photoPath != null && status !is ReviewStatus.Loading
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
