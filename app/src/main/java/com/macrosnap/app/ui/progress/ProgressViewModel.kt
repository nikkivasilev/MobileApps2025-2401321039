package com.macrosnap.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.FoodRepository
import com.macrosnap.app.data.WeightRepository
import com.macrosnap.app.data.WellnessRepository
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.SetRecord
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.model.WaterDay
import com.macrosnap.app.model.sum
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class WellnessWeek(val sleep: List<SleepLog>, val water: List<WaterDay>, val workoutDays: Set<LocalDate>)

private data class Strength(val trained: List<Exercise>, val selected: Exercise?, val records: List<SetRecord>)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val weightRepository: WeightRepository,
    foodRepository: FoodRepository,
    wellnessRepository: WellnessRepository,
    workoutRepository: WorkoutRepository,
    settingsStore: SettingsStore,
) : ViewModel() {

    private val weightInput = MutableStateFlow("")
    private val selectedExerciseId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ProgressUiState> = run {
        val today = LocalDate.now()
        val weekStart = today.minusDays(DAYS_SHOWN - 1)

        val wellness = combine(
            wellnessRepository.observeSleepRange(weekStart, today),
            wellnessRepository.observeWaterRange(weekStart, today),
            workoutRepository.observeWorkoutDays(weekStart, today),
        ) { sleep, water, days -> WellnessWeek(sleep, water, days) }

        val trained = combine(workoutRepository.observeExercises(), workoutRepository.observeExerciseIdsWithHistory()) { all, ids ->
            all.filter { it.id in ids.toSet() }
        }
        val strength = combine(trained, selectedExerciseId) { list, selectedId ->
            list to (list.firstOrNull { it.id == selectedId } ?: list.firstOrNull())
        }.flatMapLatest { (list, selected) ->
            if (selected == null) {
                flowOf(Strength(list, null, emptyList()))
            } else {
                workoutRepository.observeRecords(selected.id).map { Strength(list, selected, it) }
            }
        }

        combine(
            weightInput,
            weightRepository.observeAll(),
            foodRepository.observeRange(weekStart, today),
            settingsStore.settings,
            combine(wellness, strength) { w, s -> w to s },
        ) { input, weights, entries, settings, (wellnessWeek, strengthData) ->
            val totalsByDate = entries.groupBy { it.date }.mapValues { (_, day) -> day.map { it.item.total }.sum() }
            ProgressUiState(
                weightInput = input,
                weights = weights,
                week = (0 until DAYS_SHOWN).map { offset ->
                    val date = weekStart.plusDays(offset)
                    DayTotal(date, totalsByDate[date] ?: Macros.ZERO)
                },
                goals = settings.goals,
                sleepWeek = wellnessWeek.sleep,
                sleepGoalHours = settings.sleepGoalHours,
                waterWeek = wellnessWeek.water,
                waterGoalMl = settings.waterGoalMl,
                workoutDays = wellnessWeek.workoutDays,
                trainedExercises = strengthData.trained,
                selectedExercise = strengthData.selected,
                records = strengthData.records,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
    }

    fun onWeightInputChange(input: String) {
        weightInput.value = input
    }

    fun onLogWeight() {
        val weight = uiState.value.parsedWeight ?: return
        weightInput.value = ""
        viewModelScope.launch { weightRepository.log(LocalDate.now(), weight) }
    }

    fun onDeleteWeight(date: LocalDate) {
        viewModelScope.launch { weightRepository.delete(date) }
    }

    fun onExerciseSelect(exercise: Exercise) {
        selectedExerciseId.value = exercise.id
    }

    companion object {
        private const val DAYS_SHOWN = 7L

        val Factory = viewModelFactory {
            initializer {
                ProgressViewModel(
                    appContainer.weightRepository,
                    appContainer.foodRepository,
                    appContainer.wellnessRepository,
                    appContainer.workoutRepository,
                    appContainer.settingsStore,
                )
            }
        }
    }
}
