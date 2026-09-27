package com.macrosnap.app.ui.supplements

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.FoodAnalyzer
import com.macrosnap.app.data.SupplementRepository
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.ui.SAVE_FAILED_MESSAGE
import com.macrosnap.app.ui.SupplementEditorRoute
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.toInputs
import com.macrosnap.app.ui.trySave
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
interface SupplementEditorActions {
    fun onNameChange(value: String)
    fun onDoseLabelChange(value: String)
    fun onTimesPerDayChange(value: String)
    fun onNutrientChange(nutrient: Nutrient, value: String)
    fun onActiveChange(active: Boolean)
    fun onDescribeChange(text: String)
    fun onDescribeSubmit()
    fun onSave()
    fun onDeleteClick()
    fun onDeleteConfirm()
    fun onDeleteDismiss()
}

class SupplementEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val supplementRepository: SupplementRepository,
    private val foodAnalyzer: FoodAnalyzer,
) : ViewModel(), SupplementEditorActions {

    private val route = savedStateHandle.toRoute<SupplementEditorRoute>()

    private val _uiState = MutableStateFlow(SupplementEditorUiState(id = route.supplementId, isLoaded = route.supplementId == 0L))
    val uiState: StateFlow<SupplementEditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SupplementEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        if (route.supplementId != 0L) {
            viewModelScope.launch {
                val supplement = supplementRepository.get(route.supplementId)
                if (supplement == null) {
                    _effects.send(SupplementEditorEffect.Close)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        name = supplement.name,
                        doseLabel = supplement.doseLabel,
                        timesPerDayInput = supplement.timesPerDay.toString(),
                        nutrientInputs = supplement.micros.toInputs(),
                        isActive = supplement.isActive,
                        isLoaded = true,
                    )
                }
            }
        }
    }

    override fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    override fun onDoseLabelChange(value: String) = _uiState.update { it.copy(doseLabel = value) }

    override fun onTimesPerDayChange(value: String) = _uiState.update { it.copy(timesPerDayInput = value.filter(Char::isDigit).take(2)) }

    override fun onNutrientChange(nutrient: Nutrient, value: String) = _uiState.update {
        it.copy(nutrientInputs = it.nutrientInputs + (nutrient to value))
    }

    override fun onActiveChange(active: Boolean) = _uiState.update { it.copy(isActive = active) }

    override fun onDescribeChange(text: String) = _uiState.update {
        it.copy(describeInput = text, describeStatus = if (it.describeStatus is DescribeStatus.Failed) DescribeStatus.Idle else it.describeStatus)
    }

    override fun onDescribeSubmit() {
        val description = _uiState.value.describeInput.trim()
        if (description.isEmpty() || _uiState.value.describeStatus is DescribeStatus.Loading) return
        _uiState.update { it.copy(describeStatus = DescribeStatus.Loading) }
        viewModelScope.launch {
            when (val outcome = foodAnalyzer.analyzeSupplement(description)) {
                is Outcome.Success -> _uiState.update { state ->
                    val analysis = outcome.value
                    state.copy(
                        describeInput = "",
                        describeStatus = DescribeStatus.Idle,
                        name = state.name.ifBlank { analysis.name },
                        doseLabel = analysis.doseLabel.ifBlank { state.doseLabel },
                        nutrientInputs = analysis.micros.toInputs(),
                        notes = analysis.notes,
                    )
                }
                is Outcome.Failure -> _uiState.update { it.copy(describeStatus = DescribeStatus.Failed(outcome.error)) }
            }
        }
    }

    override fun onSave() {
        val supplement = _uiState.value.takeIf { it.canSave }?.supplement ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (trySave { supplementRepository.save(supplement) }) {
                _effects.send(SupplementEditorEffect.Close)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(SupplementEditorEffect.ShowMessage(SAVE_FAILED_MESSAGE))
            }
        }
    }

    override fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirm = true) }

    override fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    override fun onDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, isSaving = true) }
        viewModelScope.launch {
            if (trySave { supplementRepository.delete(route.supplementId) }) {
                _effects.send(SupplementEditorEffect.Close)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(SupplementEditorEffect.ShowMessage(SAVE_FAILED_MESSAGE))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SupplementEditorViewModel(createSavedStateHandle(), appContainer.supplementRepository, appContainer.foodAnalyzer)
            }
        }
    }
}
