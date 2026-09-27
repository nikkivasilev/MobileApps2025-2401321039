package com.macrosnap.app.ui.supplements

import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Supplement
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.toMicros

data class SupplementEditorUiState(
    val id: Long,
    val name: String = "",
    val doseLabel: String = "",
    val timesPerDayInput: String = "1",
    val nutrientInputs: Map<Nutrient, String> = emptyMap(),
    val isActive: Boolean = true,
    val describeInput: String = "",

    val describeStatus: DescribeStatus = DescribeStatus.Idle,
    val notes: String? = null,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
    val isExisting: Boolean get() = id != 0L
    val timesPerDay: Int? get() = timesPerDayInput.trim().toIntOrNull()?.takeIf { it in 1..10 }

    val supplement: Supplement?
        get() {
            val timesPerDay = timesPerDay ?: return null
            if (name.isBlank()) return null
            return Supplement(
                id = id,
                name = name.trim(),
                doseLabel = doseLabel.trim().ifEmpty { "1 dose" },
                timesPerDay = timesPerDay,
                micros = nutrientInputs.toMicros().filterValues { it > 0 },
                isActive = isActive,
            )
        }

    val canSave: Boolean get() = supplement != null && !isSaving && describeStatus !is DescribeStatus.Loading
}

sealed interface SupplementEditorEffect {
    data object Close : SupplementEditorEffect
}
