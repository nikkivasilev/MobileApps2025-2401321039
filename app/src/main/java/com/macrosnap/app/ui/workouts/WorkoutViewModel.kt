package com.macrosnap.app.ui.workouts

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.macrosnap.app.R
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.WeightRepository
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.domain.CalorieBurnEstimator
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.METERS_PER_KM
import com.macrosnap.app.model.SECONDS_PER_MINUTE
import com.macrosnap.app.model.Workout
import com.macrosnap.app.model.WorkoutExercise
import com.macrosnap.app.model.WorkoutSet
import com.macrosnap.app.ui.WorkoutRoute
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.toAmountOrNull
import com.macrosnap.app.ui.uiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
interface WorkoutActions {
    fun onNameChange(name: String)
    fun onAddExerciseClick()
    fun onPickerDismiss()
    fun onExerciseSelected(exercise: Exercise)
    fun onExerciseCreated(exercise: Exercise)
    fun onRemoveExercise(exerciseId: Long)
    fun onAddSet(exerciseId: Long)
    fun onRemoveLastSet(exerciseId: Long)
    fun onSetInputChange(setId: Long, inputs: SetInputs)
    fun onSetDone(setId: Long, done: Boolean)
    fun onRestSkip()
    fun onRestExtend()
    fun onRestElapsed()
    fun onFinishClick()
    fun onSummaryDismiss()
    fun onDeleteClick()
    fun onDeleteConfirm()
    fun onDeleteDismiss()
}

class WorkoutViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val settingsStore: SettingsStore,
    weightRepository: WeightRepository,
) : ViewModel(),
    WorkoutActions {

    private val route = savedStateHandle.toRoute<WorkoutRoute>()

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _effects = Channel<WorkoutEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkout(route.workoutId)
            if (workout == null) {
                _effects.send(WorkoutEffect.Close)
                return@launch
            }
            val restSeconds = settingsStore.settings.first().restSeconds
            _uiState.update {
                it.copy(
                    workout = workout,
                    inputs = workout.allSets().associate { s ->
                        s.id to s.toInputs()
                    },
                    restSeconds = restSeconds,
                )
            }
            workout.exercises.forEach { loadLastSets(it.exercise.id) }
        }
        viewModelScope.launch {
            workoutRepository.observeExercises().collect { list -> _uiState.update { it.copy(exercises = list) } }
        }
        viewModelScope.launch {
            combine(weightRepository.observeLatestKg(), settingsStore.settings) { latest, settings ->
                latest ?: settings.profile?.weightKg ?: CalorieBurnEstimator.DEFAULT_BODY_WEIGHT_KG
            }.collect { kg -> _uiState.update { it.copy(bodyWeightKg = kg) } }
        }
    }

    private fun loadLastSets(exerciseId: Long) {
        viewModelScope.launch {
            val last = workoutRepository.lastSets(exerciseId, route.workoutId)
            _uiState.update { it.copy(lastSets = it.lastSets + (exerciseId to last)) }
        }
    }

    private inline fun updateWorkout(transform: (Workout) -> Workout) = _uiState.update { state ->
        state.workout?.let { state.copy(workout = transform(it)) } ?: state
    }

    override fun onNameChange(name: String) {
        updateWorkout { it.copy(name = name) }
        val workout = _uiState.value.workout ?: return
        viewModelScope.launch { workoutRepository.renameWorkout(workout, name) }
    }

    override fun onAddExerciseClick() = _uiState.update { it.copy(showPicker = true) }

    override fun onPickerDismiss() = _uiState.update { it.copy(showPicker = false) }

    override fun onExerciseSelected(exercise: Exercise) {
        _uiState.update { it.copy(showPicker = false) }
        val workout = _uiState.value.workout ?: return
        if (workout.exercises.any { it.exercise.id == exercise.id }) {
            onAddSet(exercise.id)
            return
        }
        viewModelScope.launch {
            // Repeat last session's sets; an exercise never logged before starts from its defaults.
            val last = workoutRepository.lastSets(exercise.id, workout.id)
            val order = workout.exercises.size
            val sets = if (last.isNotEmpty()) {
                last.mapIndexed { index, previous ->
                    workoutRepository.addSet(workout.id, exercise, order, index, previous)
                }
            } else {
                val reps = if (exercise.type == ExerciseType.STRENGTH) exercise.defaultReps else 0
                (0 until exercise.defaultSets.coerceAtLeast(1)).map { index ->
                    workoutRepository.addSet(workout.id, exercise, order, index, template = null, reps = reps)
                }
            }
            updateWorkout { it.copy(exercises = it.exercises + WorkoutExercise(exercise, sets)) }
            _uiState.update { state ->
                state.copy(
                    inputs = state.inputs + sets.associate {
                        it.id to it.toInputs()
                    },
                    lastSets = state.lastSets + (exercise.id to last),
                )
            }
        }
    }

    override fun onExerciseCreated(exercise: Exercise) {
        viewModelScope.launch { onExerciseSelected(workoutRepository.saveExercise(exercise)) }
    }

    override fun onRemoveExercise(exerciseId: Long) {
        val workout = _uiState.value.workout ?: return
        val removedIds = workout.exercises.firstOrNull { it.exercise.id == exerciseId }?.sets?.map { it.id }.orEmpty()
        updateWorkout { it.copy(exercises = it.exercises.filterNot { ex -> ex.exercise.id == exerciseId }) }
        _uiState.update { it.copy(inputs = it.inputs - removedIds.toSet()) }
        viewModelScope.launch { workoutRepository.removeExercise(workout.id, exerciseId) }
    }

    override fun onAddSet(exerciseId: Long) {
        val workout = _uiState.value.workout ?: return
        val entry = workout.exercises.firstOrNull { it.exercise.id == exerciseId } ?: return
        val template = entry.sets.lastOrNull()
        viewModelScope.launch {
            val set = workoutRepository.addSet(
                workout.id,
                entry.exercise,
                template?.exerciseOrder ?: workout.exercises.size,
                entry.sets.size,
                template,
            )
            updateWorkout { w ->
                w.copy(
                    exercises = w.exercises.map {
                        if (it.exercise.id == exerciseId) it.copy(sets = it.sets + set) else it
                    },
                )
            }
            _uiState.update { it.copy(inputs = it.inputs + (set.id to set.toInputs())) }
        }
    }

    override fun onRemoveLastSet(exerciseId: Long) {
        val workout = _uiState.value.workout ?: return
        val entry = workout.exercises.firstOrNull { it.exercise.id == exerciseId } ?: return
        val last = entry.sets.lastOrNull() ?: return
        if (entry.sets.size == 1) {
            onRemoveExercise(exerciseId)
            return
        }
        updateWorkout { w ->
            w.copy(
                exercises = w.exercises.map {
                    if (it.exercise.id == exerciseId) it.copy(sets = it.sets.dropLast(1)) else it
                },
            )
        }
        _uiState.update { it.copy(inputs = it.inputs - last.id) }
        viewModelScope.launch { workoutRepository.deleteSet(last.id) }
    }

    override fun onSetInputChange(setId: Long, inputs: SetInputs) {
        _uiState.update { it.copy(inputs = it.inputs + (setId to inputs)) }
        changeSet(setId) { set ->
            set.copy(
                weightKg = inputs.weight.toAmountOrNull() ?: 0.0,
                reps = inputs.reps.trim().toIntOrNull() ?: 0,
                durationSec = ((inputs.minutes.toAmountOrNull() ?: 0.0) * 60).toInt(),
                distanceM = (inputs.km.toAmountOrNull() ?: 0.0) * 1000,
            )
        }
    }

    override fun onSetDone(setId: Long, done: Boolean) {
        changeSet(setId) { it.copy(isDone = done) }
        if (done && _uiState.value.isActive) {
            _uiState.update { it.copy(restEndsAt = System.currentTimeMillis() + it.restSeconds * 1000L) }
        }
    }

    private fun changeSet(setId: Long, transform: (WorkoutSet) -> WorkoutSet) {
        var changed: WorkoutSet? = null
        updateWorkout { w ->
            w.copy(
                exercises = w.exercises.map { ex ->
                    ex.copy(
                        sets = ex.sets.map { set ->
                            if (set.id == setId) transform(set).also { changed = it } else set
                        },
                    )
                },
            )
        }
        val workout = _uiState.value.workout ?: return
        val set = changed ?: return
        viewModelScope.launch { workoutRepository.updateSet(workout.id, set) }
    }

    override fun onRestSkip() = _uiState.update { it.copy(restEndsAt = null) }

    override fun onRestExtend() = _uiState.update { state ->
        state.copy(restEndsAt = (state.restEndsAt ?: System.currentTimeMillis()) + 30_000L)
    }

    override fun onRestElapsed() = _uiState.update { it.copy(restEndsAt = null) }

    override fun onFinishClick() {
        val workout = _uiState.value.workout ?: return
        if (!_uiState.value.canFinish) {
            _effects.trySend(WorkoutEffect.ShowMessage(uiText(R.string.workout_finish_needs_set)))
            return
        }
        _uiState.update { it.copy(isFinishing = true, restEndsAt = null) }
        viewModelScope.launch {
            val records = workoutRepository.finishWorkout(workout)
            val finished = workout.copy(endedAt = System.currentTimeMillis())
            _uiState.update {
                it.copy(
                    isFinishing = false,
                    workout = finished,
                    summary = WorkoutSummary(
                        finished.durationMinutes,
                        finished.doneSets,
                        finished.volumeKg,
                        CalorieBurnEstimator.estimate(finished, it.bodyWeightKg),
                        records,
                    ),
                )
            }
        }
    }

    override fun onSummaryDismiss() {
        _uiState.update { it.copy(summary = null) }
        _effects.trySend(WorkoutEffect.Close)
    }

    override fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirm = true) }

    override fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    override fun onDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
        viewModelScope.launch {
            workoutRepository.deleteWorkout(route.workoutId)
            _effects.send(WorkoutEffect.Close)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                WorkoutViewModel(
                    createSavedStateHandle(),
                    appContainer.workoutRepository,
                    appContainer.settingsStore,
                    appContainer.weightRepository,
                )
            }
        }
    }
}

private fun Workout.allSets(): List<WorkoutSet> = exercises.flatMap { it.sets }

private fun WorkoutSet.toInputs() = SetInputs(
    weight = weightKg.takeIf { it > 0 }?.compact().orEmpty(),
    reps = reps.takeIf { it > 0 }?.toString().orEmpty(),
    minutes = durationSec.takeIf { it > 0 }?.let { (it / SECONDS_PER_MINUTE).compact() }.orEmpty(),
    km = distanceM.takeIf { it > 0 }?.let { (it / METERS_PER_KM).compact() }.orEmpty(),
)
