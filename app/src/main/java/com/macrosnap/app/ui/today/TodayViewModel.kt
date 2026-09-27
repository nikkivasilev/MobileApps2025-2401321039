package com.macrosnap.app.ui.today

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.FoodAnalyzer
import com.macrosnap.app.data.FoodRepository
import com.macrosnap.app.data.PhotoStore
import com.macrosnap.app.data.RecipeRepository
import com.macrosnap.app.data.SupplementRepository
import com.macrosnap.app.data.WeightRepository
import com.macrosnap.app.data.WellnessRepository
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.domain.CalorieBurnEstimator
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.model.SupplementStatus
import com.macrosnap.app.model.WaterDay
import com.macrosnap.app.model.Workout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Stable
interface TodayActions {
    fun onPreviousDay()
    fun onNextDay()
    fun onJumpToToday()
    fun onEntryClick(entry: FoodEntry)
    fun onDetailDismiss()
    fun onEditClick()
    fun onEditorDismiss()
    fun onEntryEdited(item: FoodItem, mealType: MealType?)
    fun onEntryDeleted()
    fun onSupplementTaken(supplementId: Long)
    fun onSupplementUntaken(supplementId: Long)
    fun onWaterAdd(ml: Int)
    fun onWaterUndo()
    fun onSleepClick()
    fun onSleepDismiss()
    fun onSleepSave(bedtime: LocalTime, wakeTime: LocalTime, quality: Int)
    fun onSleepDelete()
    suspend fun describeItem(text: String): Outcome<AnalysisResult>
}

data class PhotoTarget(val path: String, val uri: Uri)

private data class DayData(
    val date: LocalDate,
    val entries: List<FoodEntry>,
    val supplements: List<SupplementStatus>,
    val water: WaterDay,
    val sleep: SleepLog?,
    val workouts: List<Workout>,
)

private data class Overlays(val viewing: FoodEntry?, val editing: FoodEntry?, val showSleepSheet: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val foodRepository: FoodRepository,
    private val photoStore: PhotoStore,
    private val supplementRepository: SupplementRepository,
    private val wellnessRepository: WellnessRepository,
    private val foodAnalyzer: FoodAnalyzer,
    settingsStore: SettingsStore,
    recipeRepository: RecipeRepository,
    workoutRepository: WorkoutRepository,
    weightRepository: WeightRepository,
) : ViewModel(), TodayActions {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val viewing = MutableStateFlow<FoodEntry?>(null)
    private val editing = MutableStateFlow<FoodEntry?>(null)
    private val showSleepSheet = MutableStateFlow(false)

    private val _effects = Channel<TodayEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val dayData = selectedDate.flatMapLatest { date ->
        combine(
            foodRepository.observeDay(date),
            supplementRepository.observeDay(date),
            wellnessRepository.observeWater(date),
            wellnessRepository.observeSleep(date),
            workoutRepository.observeWorkoutsOn(date),
        ) { entries, supplements, water, sleep, workouts -> DayData(date, entries, supplements, water, sleep, workouts) }
    }

    private val bodyWeight = combine(weightRepository.observeLatestKg(), settingsStore.settings) { latest, settings ->
        latest ?: settings.profile?.weightKg ?: CalorieBurnEstimator.DEFAULT_BODY_WEIGHT_KG
    }

    private val overlays = combine(viewing, editing, showSleepSheet) { v, e, s -> Overlays(v, e, s) }

    val uiState: StateFlow<TodayUiState> = combine(
        dayData,
        settingsStore.settings,
        recipeRepository.observeAll(),
        combine(overlays, bodyWeight) { o, w -> o to w },
    ) { day, settings, recipes, (overlays, weightKg) ->
        TodayUiState(
            date = day.date,
            goals = settings.goals,
            nutrientTargets = settings.nutrientTargets,
            waterGoalMl = settings.waterGoalMl,
            sleepGoalHours = settings.sleepGoalHours,
            entries = day.entries,
            supplements = day.supplements,
            water = day.water,
            sleep = day.sleep,
            recipes = recipes,
            viewing = overlays.viewing,
            editing = overlays.editing,
            showSleepSheet = overlays.showSleepSheet,
            workoutBurnKcal = day.workouts.filter { !it.isActive }.sumOf { CalorieBurnEstimator.estimate(it, weightKg) },
            addWorkoutBurn = settings.addWorkoutBurn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(date = LocalDate.now()))

    override fun onPreviousDay() = selectedDate.update { it.minusDays(1) }

    override fun onNextDay() = selectedDate.update { if (it < LocalDate.now()) it.plusDays(1) else it }

    override fun onJumpToToday() {
        selectedDate.value = LocalDate.now()
    }

    override fun onEntryClick(entry: FoodEntry) {
        viewing.value = entry
    }

    override fun onDetailDismiss() {
        viewing.value = null
    }

    override fun onEditClick() {
        editing.value = viewing.value
        viewing.value = null
    }

    override fun onEditorDismiss() {
        editing.value = null
    }

    override fun onEntryEdited(item: FoodItem, mealType: MealType?) {
        val entry = editing.value ?: return
        editing.value = null
        viewModelScope.launch {
            foodRepository.update(entry.copy(item = item, mealType = mealType ?: entry.mealType))
        }
    }

    override fun onEntryDeleted() {
        val entry = editing.value ?: viewing.value ?: return
        editing.value = null
        viewing.value = null
        viewModelScope.launch { foodRepository.delete(entry) }
    }

    override fun onSupplementTaken(supplementId: Long) {
        viewModelScope.launch { supplementRepository.take(supplementId, selectedDate.value) }
    }

    override fun onSupplementUntaken(supplementId: Long) {
        viewModelScope.launch { supplementRepository.untake(supplementId, selectedDate.value) }
    }

    override fun onWaterAdd(ml: Int) {
        viewModelScope.launch { wellnessRepository.addWater(selectedDate.value, ml) }
    }

    override fun onWaterUndo() {
        viewModelScope.launch { wellnessRepository.undoWater(selectedDate.value) }
    }

    override fun onSleepClick() {
        showSleepSheet.value = true
    }

    override fun onSleepDismiss() {
        showSleepSheet.value = false
    }

    override fun onSleepSave(bedtime: LocalTime, wakeTime: LocalTime, quality: Int) {
        val date = selectedDate.value
        showSleepSheet.value = false
        // Bedtime later in the clock than wake-up means it was the previous evening.
        val bedDate = if (bedtime > wakeTime) date.minusDays(1) else date
        viewModelScope.launch {
            wellnessRepository.logSleep(SleepLog(date, bedDate.atTime(bedtime), date.atTime(wakeTime), quality.coerceIn(1, 5)))
        }
    }

    override fun onSleepDelete() {
        showSleepSheet.value = false
        viewModelScope.launch { wellnessRepository.deleteSleep(selectedDate.value) }
    }

    override suspend fun describeItem(text: String): Outcome<AnalysisResult> = foodAnalyzer.analyzeText(text)

    fun newPhotoTarget(): PhotoTarget {
        val file = photoStore.newPhotoFile()
        return PhotoTarget(file.path, photoStore.shareableUri(file))
    }

    fun onPhotoDiscarded(path: String) {
        viewModelScope.launch { photoStore.delete(path) }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val file = photoStore.importFrom(uri)
            _effects.send(
                if (file != null) {
                    TodayEffect.OpenPhotoReview(selectedDate.value.toEpochDay(), file.path)
                } else {
                    TodayEffect.ShowMessage("That image couldn't be opened.")
                },
            )
        }
    }

    fun onScanFailed() {
        _effects.trySend(TodayEffect.ShowMessage("The barcode scanner isn't ready yet. Check your connection and try again in a minute."))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TodayViewModel(
                    appContainer.foodRepository,
                    appContainer.photoStore,
                    appContainer.supplementRepository,
                    appContainer.wellnessRepository,
                    appContainer.foodAnalyzer,
                    appContainer.settingsStore,
                    appContainer.recipeRepository,
                    appContainer.workoutRepository,
                    appContainer.weightRepository,
                )
            }
        }
    }
}
