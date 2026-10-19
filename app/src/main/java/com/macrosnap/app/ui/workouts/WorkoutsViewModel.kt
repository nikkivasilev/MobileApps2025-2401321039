package com.macrosnap.app.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.WeightRepository
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.domain.CalorieBurnEstimator
import com.macrosnap.app.model.Routine
import com.macrosnap.app.model.Workout
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutsUiState(
    val active: Workout? = null,
    val routines: List<Routine> = emptyList(),
    val history: List<Workout> = emptyList(),
    val burnKcal: Map<Long, Int> = emptyMap(),
)

sealed interface WorkoutsEffect {
    data class OpenWorkout(val workoutId: Long) : WorkoutsEffect
}

class WorkoutsViewModel(
    private val workoutRepository: WorkoutRepository,
    weightRepository: WeightRepository,
    settingsStore: SettingsStore,
) : ViewModel() {

    private val bodyWeight = combine(weightRepository.observeLatestKg(), settingsStore.settings) { latest, settings ->
        latest ?: settings.profile?.weightKg ?: CalorieBurnEstimator.DEFAULT_BODY_WEIGHT_KG
    }

    val uiState: StateFlow<WorkoutsUiState> = combine(
        workoutRepository.observeActiveWorkout(),
        workoutRepository.observeRoutines(),
        workoutRepository.observeRecentWorkouts(),
        bodyWeight,
    ) { active, routines, recent, weightKg ->
        val history = recent.filter { !it.isActive }
        WorkoutsUiState(
            active = active,
            routines = routines,
            history = history,
            burnKcal = history.associate { it.id to CalorieBurnEstimator.estimate(it, weightKg) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

    private val _effects = Channel<WorkoutsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** [name] comes from the screen so the stored name is in the language the workout was started in. */
    fun onStartEmpty(name: String) {
        viewModelScope.launch {
            val id = workoutRepository.startWorkout(name, null)
            _effects.send(WorkoutsEffect.OpenWorkout(id))
        }
    }

    fun onStartRoutine(routine: Routine) {
        viewModelScope.launch {
            val id = workoutRepository.startWorkout(routine.name, routine)
            _effects.send(WorkoutsEffect.OpenWorkout(id))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                WorkoutsViewModel(
                    appContainer.workoutRepository,
                    appContainer.weightRepository,
                    appContainer.settingsStore,
                )
            }
        }
    }
}
