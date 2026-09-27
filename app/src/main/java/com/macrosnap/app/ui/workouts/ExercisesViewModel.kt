package com.macrosnap.app.ui.workouts

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.ExerciseUsage
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.ui.SAVE_FAILED_MESSAGE
import com.macrosnap.app.ui.trySave
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The exercise being created (id 0) or edited, as raw inputs. */
data class ExerciseEditorState(
    val id: Long,
    val isCustom: Boolean,
    val name: String,
    val group: MuscleGroup,
    val type: ExerciseType,
    val setsInput: String,
    val repsInput: String,
    /** Kept for cardio, whose reps field is hidden, so switching type back doesn't lose it. */
    val originalReps: Int = Exercise.DEFAULT_REPS,
) {
    val isNew: Boolean get() = id == 0L

    fun toExercise(): Exercise? {
        val name = name.trim().takeIf { it.isNotEmpty() } ?: return null
        val sets = parseDefaultSets(setsInput) ?: return null
        val reps = if (type == ExerciseType.CARDIO) parseDefaultReps(repsInput) ?: originalReps else parseDefaultReps(repsInput) ?: return null
        return Exercise(id, name, group, type, isCustom, sets, reps)
    }

    companion object {
        fun new(name: String, group: MuscleGroup?) = ExerciseEditorState(
            id = 0,
            isCustom = true,
            name = name,
            group = group ?: MuscleGroup.OTHER,
            type = if (group == MuscleGroup.CARDIO) ExerciseType.CARDIO else ExerciseType.STRENGTH,
            setsInput = defaultSetsFor(if (group == MuscleGroup.CARDIO) ExerciseType.CARDIO else ExerciseType.STRENGTH).toString(),
            repsInput = Exercise.DEFAULT_REPS.toString(),
        )

        fun of(exercise: Exercise) = ExerciseEditorState(
            id = exercise.id,
            isCustom = exercise.isCustom,
            name = exercise.name,
            group = exercise.muscleGroup,
            type = exercise.type,
            setsInput = exercise.defaultSets.toString(),
            repsInput = exercise.defaultReps.toString(),
            originalReps = exercise.defaultReps,
        )
    }
}

data class ExercisesUiState(
    val exercises: List<Exercise> = emptyList(),
    val query: String = "",
    val groupFilter: MuscleGroup? = null,
    val editor: ExerciseEditorState? = null,
    /** Set while asking to delete [ExerciseEditorState.id]; says what else would go with it. */
    val deleteUsage: ExerciseUsage? = null,
    val isSaving: Boolean = false,
) {
    val filtered: List<Exercise>
        get() {
            val trimmed = query.trim()
            return exercises.filter { exercise ->
                (groupFilter == null || exercise.muscleGroup == groupFilter) &&
                    (trimmed.isEmpty() || exercise.name.contains(trimmed, ignoreCase = true))
            }
        }

    /** Another exercise already has the editor's name; two identical entries would be indistinguishable in the picker. */
    val editorNameTaken: Boolean
        get() {
            val editor = editor ?: return false
            val name = editor.name.trim()
            return exercises.any { it.id != editor.id && it.name.equals(name, ignoreCase = true) }
        }

    val canSaveEditor: Boolean get() = editor?.toExercise() != null && !editorNameTaken && !isSaving
}

sealed interface ExercisesEffect {
    data class ShowMessage(val message: String) : ExercisesEffect
}

@Stable
interface ExercisesActions {
    fun onQueryChange(query: String)
    fun onGroupFilterChange(group: MuscleGroup?)
    fun onNewClick()
    fun onExerciseClick(exercise: Exercise)
    fun onEditorChange(editor: ExerciseEditorState)
    fun onEditorSave()
    fun onEditorDismiss()
    fun onDeleteClick()
    fun onDeleteConfirm()
    fun onDeleteDismiss()
}

class ExercisesViewModel(private val workoutRepository: WorkoutRepository) : ViewModel(), ExercisesActions {

    private val _uiState = MutableStateFlow(ExercisesUiState())
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ExercisesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            workoutRepository.observeExercises().collect { list -> _uiState.update { it.copy(exercises = list) } }
        }
    }

    override fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    override fun onGroupFilterChange(group: MuscleGroup?) = _uiState.update { it.copy(groupFilter = group) }

    // A search that found nothing is usually the name of the exercise the user is about to add.
    override fun onNewClick() = _uiState.update {
        it.copy(editor = ExerciseEditorState.new(name = if (it.filtered.isEmpty()) it.query.trim() else "", group = it.groupFilter))
    }

    override fun onExerciseClick(exercise: Exercise) = _uiState.update { it.copy(editor = ExerciseEditorState.of(exercise)) }

    override fun onEditorChange(editor: ExerciseEditorState) = _uiState.update { it.copy(editor = editor) }

    override fun onEditorDismiss() = _uiState.update { it.copy(editor = null, deleteUsage = null) }

    override fun onEditorSave() {
        val state = _uiState.value
        if (!state.canSaveEditor) return
        val exercise = state.editor?.toExercise() ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val saved = trySave { workoutRepository.saveExercise(exercise) }
            _uiState.update { it.copy(isSaving = false, editor = if (saved) null else it.editor) }
            if (!saved) _effects.send(ExercisesEffect.ShowMessage(SAVE_FAILED_MESSAGE))
        }
    }

    override fun onDeleteClick() {
        val editor = _uiState.value.editor?.takeUnless { it.isNew } ?: return
        viewModelScope.launch {
            val usage = workoutRepository.exerciseUsage(editor.id)
            _uiState.update { it.copy(deleteUsage = usage) }
        }
    }

    override fun onDeleteDismiss() = _uiState.update { it.copy(deleteUsage = null) }

    override fun onDeleteConfirm() {
        val editor = _uiState.value.editor?.takeUnless { it.isNew } ?: return
        _uiState.update { it.copy(isSaving = true, deleteUsage = null) }
        viewModelScope.launch {
            val deleted = trySave { workoutRepository.deleteExercise(editor.id) }
            _uiState.update { it.copy(isSaving = false, editor = if (deleted) null else it.editor) }
            if (!deleted) _effects.send(ExercisesEffect.ShowMessage(SAVE_FAILED_MESSAGE))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ExercisesViewModel(appContainer.workoutRepository) }
        }
    }
}
