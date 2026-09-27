package com.macrosnap.app.ui.recipes

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
import com.macrosnap.app.data.FoodRepository
import com.macrosnap.app.data.RecipeRepository
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.acceptSuggestion
import com.macrosnap.app.ui.RecipeEditorRoute
import com.macrosnap.app.ui.SAVE_FAILED_MESSAGE
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.DraftItem
import com.macrosnap.app.ui.components.EditorTarget
import com.macrosnap.app.ui.trySave
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
interface RecipeEditorActions {
    fun onNameChange(name: String)
    fun onServingsChange(value: String)
    fun onCookedWeightChange(value: String)
    fun onDescribeChange(text: String)
    fun onDescribeSubmit()
    fun onSuggestionClick(food: PastFood)
    fun onGramsStep(draftId: Long, deltaGrams: Double)
    fun onIngredientClick(draft: DraftItem)
    fun onAddIngredientClick()
    fun onIngredientRemove(draftId: Long)
    fun onEditorSave(item: FoodItem)
    fun onEditorDismiss()
    fun onSave()
    fun onDeleteClick()
    fun onDeleteConfirm()
    fun onDeleteDismiss()
    suspend fun describeItem(text: String): Outcome<AnalysisResult>
}

class RecipeEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val foodAnalyzer: FoodAnalyzer,
    foodRepository: FoodRepository,
) : ViewModel(), RecipeEditorActions {

    private val route = savedStateHandle.toRoute<RecipeEditorRoute>()
    private var nextDraftId = 0L

    private val _uiState = MutableStateFlow(RecipeEditorUiState(id = route.recipeId, isLoaded = route.recipeId == 0L))
    val uiState: StateFlow<RecipeEditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecipeEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            foodRepository.observePastFoods().collect { foods -> _uiState.update { it.copy(pastFoods = foods) } }
        }
        if (route.recipeId != 0L) {
            viewModelScope.launch {
                val recipe = recipeRepository.get(route.recipeId)
                if (recipe == null) {
                    _effects.send(RecipeEditorEffect.Close)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        name = recipe.name,
                        servingsInput = recipe.servings.toString(),
                        cookedWeightInput = recipe.cookedWeightGrams?.compact().orEmpty(),
                        ingredients = recipe.ingredients.map { item -> DraftItem(nextDraftId++, item) },
                        isLoaded = true,
                    )
                }
            }
        }
    }

    override fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    override fun onServingsChange(value: String) = _uiState.update { it.copy(servingsInput = value.filter(Char::isDigit).take(3)) }

    override fun onCookedWeightChange(value: String) = _uiState.update { it.copy(cookedWeightInput = value) }

    override fun onDescribeChange(text: String) = _uiState.update {
        it.copy(describeInput = text, describeStatus = if (it.describeStatus is DescribeStatus.Failed) DescribeStatus.Idle else it.describeStatus)
    }

    override fun onSuggestionClick(food: PastFood) = _uiState.update { state ->
        val (item, remaining) = acceptSuggestion(state.describeInput, food)
        state.copy(ingredients = state.ingredients + DraftItem(nextDraftId++, item), describeInput = remaining, describeStatus = DescribeStatus.Idle)
    }

    override fun onDescribeSubmit() {
        val description = _uiState.value.describeInput.trim()
        if (description.isEmpty() || _uiState.value.describeStatus is DescribeStatus.Loading) return
        _uiState.update { it.copy(describeStatus = DescribeStatus.Loading) }
        viewModelScope.launch {
            when (val outcome = foodAnalyzer.analyzeText(description)) {
                is Outcome.Success -> _uiState.update { state ->
                    state.copy(
                        describeInput = "",
                        describeStatus = DescribeStatus.Idle,
                        ingredients = state.ingredients + outcome.value.items.map { DraftItem(nextDraftId++, it.item, it.confidence) },
                    )
                }
                is Outcome.Failure -> _uiState.update { it.copy(describeStatus = DescribeStatus.Failed(outcome.error)) }
            }
        }
    }

    override fun onGramsStep(draftId: Long, deltaGrams: Double) = _uiState.update { state ->
        state.copy(
            ingredients = state.ingredients.map { draft ->
                if (draft.id != draftId) return@map draft
                draft.copy(item = draft.item.copy(grams = (draft.item.grams + deltaGrams).coerceAtLeast(MIN_GRAMS)))
            },
        )
    }

    override fun onIngredientClick(draft: DraftItem) = _uiState.update { it.copy(editor = EditorTarget.Existing(draft)) }

    override fun onAddIngredientClick() = _uiState.update { it.copy(editor = EditorTarget.NewItem) }

    override fun onIngredientRemove(draftId: Long) = _uiState.update { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.id == draftId })
    }

    override fun onEditorSave(item: FoodItem) = _uiState.update { state ->
        val ingredients = when (val target = state.editor) {
            is EditorTarget.Existing -> state.ingredients.map { if (it.id == target.draft.id) it.copy(item = item) else it }
            EditorTarget.NewItem, null -> state.ingredients + DraftItem(nextDraftId++, item)
        }
        state.copy(ingredients = ingredients, editor = null)
    }

    override fun onEditorDismiss() = _uiState.update { it.copy(editor = null) }

    override suspend fun describeItem(text: String): Outcome<AnalysisResult> = foodAnalyzer.analyzeText(text)

    override fun onSave() {
        val recipe = _uiState.value.takeIf { it.canSave }?.recipe ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (trySave { recipeRepository.save(recipe) }) {
                _effects.send(RecipeEditorEffect.Close)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(RecipeEditorEffect.ShowMessage(SAVE_FAILED_MESSAGE))
            }
        }
    }

    override fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirm = true) }

    override fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    override fun onDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, isSaving = true) }
        viewModelScope.launch {
            if (trySave { recipeRepository.delete(route.recipeId) }) {
                _effects.send(RecipeEditorEffect.Close)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(RecipeEditorEffect.ShowMessage(SAVE_FAILED_MESSAGE))
            }
        }
    }

    companion object {
        private const val MIN_GRAMS = 1.0

        val Factory = viewModelFactory {
            initializer {
                RecipeEditorViewModel(createSavedStateHandle(), appContainer.recipeRepository, appContainer.foodAnalyzer, appContainer.foodRepository)
            }
        }
    }
}
