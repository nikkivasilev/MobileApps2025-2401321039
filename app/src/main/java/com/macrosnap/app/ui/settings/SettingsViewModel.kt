package com.macrosnap.app.ui.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrosnap.app.R
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.ApiKeyChecker
import com.macrosnap.app.data.SupplementRepository
import com.macrosnap.app.data.settings.AppSettings
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.Sex
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.toInputs
import com.macrosnap.app.ui.uiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_GOAL_DIGITS = 5
private const val MAX_AGE_DIGITS = 3
private const val MAX_REST_SECONDS_DIGITS = 3

@Stable
interface SettingsActions {
    fun onApiKeyChange(value: String)
    fun onModelsChange(value: String)
    fun onResetModels()
    fun onSaveConnection()
    fun onGoalInputChange(field: GoalField, value: String)
    fun onSaveGoals()
    fun onSexSelect(sex: Sex)
    fun onAgeChange(value: String)
    fun onHeightChange(value: String)
    fun onWeightChange(value: String)
    fun onActivitySelect(activity: ActivityLevel)
    fun onGoalTypeSelect(goalType: GoalType)
    fun onApplySuggestedGoals()
    fun onNutrientTargetChange(nutrient: Nutrient, value: String)
    fun onSaveNutrientTargets()
    fun onResetNutrientTargets()
    fun onWaterGoalChange(value: String)
    fun onSleepGoalChange(value: String)
    fun onRestSecondsChange(value: String)
    fun onSaveWellness()
    fun onAddWorkoutBurnChange(enabled: Boolean)
}

enum class GoalField { KCAL, PROTEIN, CARBS, FAT }

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val apiKeyChecker: ApiKeyChecker,
    supplementRepository: SupplementRepository,
) : ViewModel(),
    SettingsActions {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update { state ->
                    // Inputs are seeded once; afterwards only the snapshot follows the store,
                    // so a save elsewhere never overwrites what the user is typing.
                    if (state.saved == null) state.seededFrom(settings) else state.copy(saved = settings)
                }
            }
        }
        viewModelScope.launch {
            supplementRepository.observeAll().collect { supplements ->
                _uiState.update { it.copy(supplements = supplements) }
            }
        }
    }

    private fun SettingsUiState.seededFrom(settings: AppSettings) = copy(
        saved = settings,
        apiKeyInput = settings.apiKey,
        modelsInput = settings.models.joinToString(", "),
        sex = settings.profile?.sex ?: sex,
        ageInput = settings.profile?.ageYears?.toString().orEmpty(),
        heightInput = settings.profile?.heightCm?.compact().orEmpty(),
        weightInput = settings.profile?.weightKg?.compact().orEmpty(),
        activity = settings.profile?.activity ?: activity,
        goalType = settings.profile?.goal ?: goalType,
        nutrientInputs = settings.nutrientTargets.toInputs(),
        waterGoalInput = settings.waterGoalMl.toString(),
        sleepGoalInput = settings.sleepGoalHours.compact(),
        restSecondsInput = settings.restSeconds.toString(),
    ).withGoalInputs(settings.goals)

    private fun SettingsUiState.withGoalInputs(goals: Goals) = copy(
        kcalInput = goals.kcal.toString(),
        proteinInput = goals.protein.toString(),
        carbsInput = goals.carbs.toString(),
        fatInput = goals.fat.toString(),
    )

    override fun onApiKeyChange(value: String) = _uiState.update {
        it.copy(apiKeyInput = value, keyCheck = KeyCheckState.Idle)
    }

    override fun onModelsChange(value: String) = _uiState.update {
        it.copy(modelsInput = value, keyCheck = KeyCheckState.Idle)
    }

    override fun onResetModels() = _uiState.update {
        it.copy(modelsInput = AppSettings.DEFAULT_MODELS.joinToString(", "), keyCheck = KeyCheckState.Idle)
    }

    override fun onSaveConnection() {
        val state = _uiState.value
        val apiKey = state.apiKeyInput.trim()
        val models = state.models.ifEmpty { AppSettings.DEFAULT_MODELS }
        viewModelScope.launch {
            val hadKey = settingsStore.settings.first().apiKey.isNotBlank()
            settingsStore.setApiKey(apiKey)
            settingsStore.setModels(models)
            if (apiKey.isEmpty()) {
                // An empty field removes the key; say so rather than let it look like nothing happened.
                if (hadKey) _effects.send(SettingsEffect.ShowMessage(uiText(R.string.settings_key_removed)))
                return@launch
            }

            _uiState.update { it.copy(keyCheck = KeyCheckState.Checking) }
            val result = when (val outcome = apiKeyChecker.check(apiKey, models)) {
                is Outcome.Success -> KeyCheckState.Valid(outcome.value)
                is Outcome.Failure -> KeyCheckState.Failed(outcome.error)
            }
            _uiState.update { it.copy(keyCheck = result) }
        }
    }

    override fun onGoalInputChange(field: GoalField, value: String) = _uiState.update {
        val digits = value.filter(Char::isDigit).take(MAX_GOAL_DIGITS)
        when (field) {
            GoalField.KCAL -> it.copy(kcalInput = digits)
            GoalField.PROTEIN -> it.copy(proteinInput = digits)
            GoalField.CARBS -> it.copy(carbsInput = digits)
            GoalField.FAT -> it.copy(fatInput = digits)
        }
    }

    override fun onSaveGoals() {
        val goals = _uiState.value.enteredGoals ?: return
        viewModelScope.launch {
            settingsStore.setGoals(goals)
            _effects.send(SettingsEffect.ShowMessage(uiText(R.string.settings_goals_saved)))
        }
    }

    override fun onSexSelect(sex: Sex) = _uiState.update { it.copy(sex = sex) }

    override fun onAgeChange(value: String) = _uiState.update {
        it.copy(ageInput = value.filter(Char::isDigit).take(MAX_AGE_DIGITS))
    }

    override fun onHeightChange(value: String) = _uiState.update { it.copy(heightInput = value) }

    override fun onWeightChange(value: String) = _uiState.update { it.copy(weightInput = value) }

    override fun onActivitySelect(activity: ActivityLevel) = _uiState.update { it.copy(activity = activity) }

    override fun onGoalTypeSelect(goalType: GoalType) = _uiState.update { it.copy(goalType = goalType) }

    override fun onApplySuggestedGoals() {
        val state = _uiState.value
        val profile = state.enteredProfile ?: return
        val goals = state.suggestedGoals ?: return
        _uiState.update { it.withGoalInputs(goals) }
        viewModelScope.launch {
            settingsStore.setProfile(profile)
            settingsStore.setGoals(goals)
            _effects.send(SettingsEffect.ShowMessage(uiText(R.string.settings_goals_updated, goals.kcal)))
        }
    }

    override fun onNutrientTargetChange(nutrient: Nutrient, value: String) = _uiState.update {
        it.copy(nutrientInputs = it.nutrientInputs + (nutrient to value))
    }

    override fun onSaveNutrientTargets() {
        val state = _uiState.value
        val targets = state.enteredNutrientTargets ?: return
        // Only values that differ from the RDA are stored, so a later sex change still moves the rest.
        val defaults = Nutrient.defaultTargets(state.saved?.profile?.sex)
        val overrides = targets.filter { (nutrient, amount) -> amount != defaults[nutrient] }
        viewModelScope.launch {
            settingsStore.setNutrientOverrides(overrides)
            _effects.send(SettingsEffect.ShowMessage(uiText(R.string.settings_nutrient_targets_saved)))
        }
    }

    override fun onResetNutrientTargets() {
        val defaults = Nutrient.defaultTargets(_uiState.value.saved?.profile?.sex)
        _uiState.update { it.copy(nutrientInputs = defaults.toInputs()) }
        viewModelScope.launch { settingsStore.setNutrientOverrides(emptyMap()) }
    }

    override fun onWaterGoalChange(value: String) = _uiState.update {
        it.copy(waterGoalInput = value.filter(Char::isDigit).take(MAX_GOAL_DIGITS))
    }

    override fun onSleepGoalChange(value: String) = _uiState.update { it.copy(sleepGoalInput = value) }

    override fun onRestSecondsChange(value: String) = _uiState.update {
        it.copy(restSecondsInput = value.filter(Char::isDigit).take(MAX_REST_SECONDS_DIGITS))
    }

    override fun onAddWorkoutBurnChange(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAddWorkoutBurn(enabled) }
    }

    override fun onSaveWellness() {
        val state = _uiState.value
        val water = state.waterGoal ?: return
        val sleep = state.sleepGoal ?: return
        val rest = state.restSeconds ?: return
        viewModelScope.launch {
            settingsStore.setWellnessGoals(water, sleep, rest)
            _effects.send(SettingsEffect.ShowMessage(uiText(R.string.settings_wellness_saved)))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    appContainer.settingsStore,
                    appContainer.apiKeyChecker,
                    appContainer.supplementRepository,
                )
            }
        }
    }
}
