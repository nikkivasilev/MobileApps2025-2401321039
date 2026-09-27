package com.macrosnap.app.ui.workouts

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.model.Routine
import com.macrosnap.app.model.RoutineExercise
import com.macrosnap.app.ui.RoutineEditorRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineExerciseDraft(
    val exercise: Exercise,
    val setsInput: String = "3",
    val repsInput: String = "8",
) {
    val sets: Int? get() = setsInput.trim().toIntOrNull()?.takeIf { it in 1..20 }
    val reps: Int? get() = repsInput.trim().toIntOrNull()?.takeIf { it in 1..200 }
}

data class RoutineEditorUiState(
    val id: Long,
    val name: String = "",
    val drafts: List<RoutineExerciseDraft> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val showPicker: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isExisting: Boolean get() = id != 0L
    val routine: Routine?
        get() {
            if (name.isBlank() || drafts.isEmpty()) return null
            val exercises = drafts.map { RoutineExercise(it.exercise, it.sets ?: return null, it.reps ?: return null) }
            return Routine(id, name.trim(), exercises)
        }
    val canSave: Boolean get() = routine != null && !isSaving
}

sealed interface RoutineEditorEffect {
    data object Close : RoutineEditorEffect
}

@Stable
interface RoutineEditorActions {
    fun onNameChange(name: String)
    fun onAddExerciseClick()
    fun onPickerDismiss()
    fun onExerciseSelected(exercise: Exercise)
    fun onExerciseCreated(name: String, group: MuscleGroup, type: ExerciseType)
    fun onDraftChange(index: Int, draft: RoutineExerciseDraft)
    fun onDraftRemove(index: Int)
    fun onSave()
    fun onDeleteClick()
    fun onDeleteConfirm()
    fun onDeleteDismiss()
}

class RoutineEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
) : ViewModel(), RoutineEditorActions {

    private val route = savedStateHandle.toRoute<RoutineEditorRoute>()

    private val _uiState = MutableStateFlow(RoutineEditorUiState(id = route.routineId))
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RoutineEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        if (route.routineId != 0L) {
            viewModelScope.launch {
                val routine = workoutRepository.getRoutine(route.routineId)
                if (routine == null) {
                    _effects.send(RoutineEditorEffect.Close)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        name = routine.name,
                        drafts = routine.exercises.map { ex -> RoutineExerciseDraft(ex.exercise, ex.targetSets.toString(), ex.targetReps.toString()) },
                    )
                }
            }
        }
        viewModelScope.launch {
            workoutRepository.observeExercises().collect { list -> _uiState.update { it.copy(exercises = list) } }
        }
    }

    override fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    override fun onAddExerciseClick() = _uiState.update { it.copy(showPicker = true) }

    override fun onPickerDismiss() = _uiState.update { it.copy(showPicker = false) }

    override fun onExerciseSelected(exercise: Exercise) = _uiState.update {
        it.copy(showPicker = false, drafts = it.drafts + RoutineExerciseDraft(exercise))
    }

    override fun onExerciseCreated(name: String, group: MuscleGroup, type: ExerciseType) {
        viewModelScope.launch { onExerciseSelected(workoutRepository.addCustomExercise(name, group, type)) }
    }

    override fun onDraftChange(index: Int, draft: RoutineExerciseDraft) = _uiState.update { state ->
        state.copy(drafts = state.drafts.mapIndexed { i, d -> if (i == index) draft else d })
    }

    override fun onDraftRemove(index: Int) = _uiState.update { state ->
        state.copy(drafts = state.drafts.filterIndexed { i, _ -> i != index })
    }

    override fun onSave() {
        val routine = _uiState.value.takeIf { it.canSave }?.routine ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            workoutRepository.saveRoutine(routine)
            _effects.send(RoutineEditorEffect.Close)
        }
    }

    override fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirm = true) }

    override fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    override fun onDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, isSaving = true) }
        viewModelScope.launch {
            workoutRepository.deleteRoutine(route.routineId)
            _effects.send(RoutineEditorEffect.Close)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RoutineEditorViewModel(createSavedStateHandle(), appContainer.workoutRepository) }
        }
    }
}
