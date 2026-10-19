package com.macrosnap.app.ui.review

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
import com.macrosnap.app.data.FoodAnalyzer
import com.macrosnap.app.data.FoodRepository
import com.macrosnap.app.data.PhotoStore
import com.macrosnap.app.data.ProductRepository
import com.macrosnap.app.data.RecipeRepository
import com.macrosnap.app.data.ScannedProduct
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.EntrySource
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.acceptSuggestion
import com.macrosnap.app.model.isTemporary
import com.macrosnap.app.ui.ReviewRoute
import com.macrosnap.app.ui.SAVE_FAILED_MESSAGE
import com.macrosnap.app.ui.components.DescribeStatus
import com.macrosnap.app.ui.components.DraftItem
import com.macrosnap.app.ui.components.EditorTarget
import com.macrosnap.app.ui.components.SERVINGS_STEP
import com.macrosnap.app.ui.trySave
import com.macrosnap.app.ui.uiText
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Stable
interface ReviewActions {
    fun onHintChange(hint: String)
    fun onReanalyze()
    fun onDescribeChange(text: String)
    fun onDescribeSubmit()
    fun onSuggestionClick(food: PastFood)
    fun onMealTypeSelect(mealType: MealType)
    fun onGramsStep(draftId: Long, deltaGrams: Double)
    fun onServingsStep(draftId: Long, deltaServings: Double)
    fun onItemClick(draft: DraftItem)
    fun onAddItemClick()
    fun onItemRemove(draftId: Long)
    fun onEditorSave(item: FoodItem)
    fun onEditorDismiss()
    fun onSave()
    suspend fun describeItem(text: String): Outcome<AnalysisResult>
}

class ReviewViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val foodAnalyzer: FoodAnalyzer,
    private val productRepository: ProductRepository,
    private val recipeRepository: RecipeRepository,
    private val foodRepository: FoodRepository,
    private val photoStore: PhotoStore,
) : ViewModel(),
    ReviewActions {

    private val route = savedStateHandle.toRoute<ReviewRoute>()
    private var nextDraftId = 0L
    private var lookupJob: Job? = null
    private var microsJob: Job? = null
    private var photoKept = false
    private var pendingUpdateJob: Job? = null

    private val restored = restoreReview(savedStateHandle[SAVED_REVIEW_KEY], route.photoPath)

    private val _uiState = MutableStateFlow(
        restored?.let { it.copy(savedForLater = it.savedForLater || route.resumePending) } ?: ReviewUiState(
            mealType = MealType.forTime(LocalTime.now()),
            photoPath = route.photoPath,
            describeInput = route.describeText.orEmpty(),
            editor = if (route.isBlankManualEntry) EditorTarget.NewItem else null,
            // A resubmitted photo already has its hint; go straight to the request instead of asking again.
            hasAnalyzed = route.resumePending,
            status = if (route.resumePending) {
                ReviewStatus.Loading(
                    uiText(R.string.review_analyzing),
                )
            } else {
                ReviewStatus.Ready
            },
            savedForLater = route.resumePending,
        ),
    )
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ReviewEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val ReviewRoute.isBlankManualEntry: Boolean
        get() = photoPath == null &&
            barcode == null &&
            recipeId == null &&
            pastFoodKey == null &&
            !isPastMeal &&
            !describe

    private val ReviewRoute.isPastMeal: Boolean
        get() = pastMealKey != null || copyMealEpochDay != null

    init {
        if (restored != null) {
            nextDraftId = (restored.items.maxOfOrNull { it.id } ?: -1) + 1
        } else {
            // A photo waits for the user's hint before the first request: one well-aimed call beats
            // a blind guess plus a retry, and the free tier's daily quota is small.
            when {
                route.barcode != null -> lookUpBarcode(route.barcode)
                route.recipeId != null -> loadRecipe(route.recipeId)
                route.pastFoodKey != null -> loadPastFood(route.pastFoodKey)
                route.pastMealKey != null -> loadPastMeal(route.pastMealKey)
                route.copyMealEpochDay != null -> copyMeal(
                    LocalDate.ofEpochDay(route.copyMealEpochDay),
                    route.copyMealType,
                )
            }
        }
        route.photoPath?.let { path ->
            viewModelScope.launch {
                // Also covers a review restored after process death that was already kept for later.
                val pending = foodRepository.pendingPhoto(path)
                if (pending != null) _uiState.update { it.copy(savedForLater = true) }
                if (route.resumePending && restored == null) {
                    if (pending != null) _uiState.update { it.copy(hint = pending.hint, mealType = pending.mealType) }
                    analyzePhoto(path)
                }
            }
        }
        viewModelScope.launch {
            foodRepository.observePastFoods().collect { foods -> _uiState.update { it.copy(pastFoods = foods) } }
        }
        viewModelScope.launch {
            // Only settled states are kept: a restore mid-lookup would show an empty list with no
            // request running, whereas no saved state simply starts the lookup again.
            _uiState.collect { state ->
                if (state.status == ReviewStatus.Ready) savedStateHandle[SAVED_REVIEW_KEY] = state.toSavedJson()
            }
        }
    }

    private fun analyzePhoto(photoPath: String) {
        lookupJob?.cancel()
        _uiState.update {
            it.copy(status = ReviewStatus.Loading(uiText(R.string.review_analyzing)), hasAnalyzed = true)
        }
        lookupJob = viewModelScope.launch {
            val hint = _uiState.value.hint.takeIf { it.isNotBlank() }
            when (val outcome = foodAnalyzer.analyzePhoto(photoPath, hint)) {
                is Outcome.Success -> _uiState.update { state ->
                    state.copy(
                        status = ReviewStatus.Ready,
                        notes = outcome.value.notes,
                        // Keep what the user added by hand or by describing; only the photo's own guesses are replaced.
                        items = state.items.filterNot { it.fromPhoto } +
                            outcome.value.items.map {
                                DraftItem(nextDraftId++, it.item, it.confidence, fromPhoto = true)
                            },
                    )
                }
                is Outcome.Failure -> {
                    _uiState.update { it.copy(status = ReviewStatus.Failed(outcome.error)) }
                    if (outcome.error.isTemporary) keepForLater(photoPath)
                }
            }
        }
    }

    /** Gemini couldn't take the photo right now: put it on Today's waiting list rather than lose it. */
    private suspend fun keepForLater(photoPath: String) {
        val state = _uiState.value
        val kept = trySave {
            foodRepository.keepPendingPhoto(photoPath, LocalDate.ofEpochDay(route.epochDay), state.mealType, state.hint)
        }
        if (kept) _uiState.update { it.copy(savedForLater = true) }
    }

    /** Keeps a waiting photo's meal and hint in step with the screen, so a resubmit uses the latest. */
    private fun updatePendingPhoto() {
        val path = route.photoPath ?: return
        if (!_uiState.value.savedForLater) return
        pendingUpdateJob?.cancel()
        pendingUpdateJob = viewModelScope.launch {
            delay(PENDING_UPDATE_DELAY_MILLIS)
            if (photoKept) return@launch
            val state = _uiState.value
            trySave {
                foodRepository.keepPendingPhoto(path, LocalDate.ofEpochDay(route.epochDay), state.mealType, state.hint)
            }
        }
    }

    private fun lookUpBarcode(barcode: String) {
        _uiState.update { it.copy(status = ReviewStatus.Loading(uiText(R.string.review_looking_up_barcode, barcode))) }
        lookupJob = viewModelScope.launch {
            when (val outcome = productRepository.lookup(barcode)) {
                is Outcome.Success -> {
                    val product = outcome.value
                    val draft = DraftItem(nextDraftId++, product.item)
                    _uiState.update { state ->
                        state.copy(status = ReviewStatus.Ready, items = listOf(draft), labelWarnings = product.warnings)
                    }
                    if (product.missingMicros.isNotEmpty()) fillMissingMicros(product, draft.id)
                }
                is Outcome.Failure -> _uiState.update { it.copy(status = ReviewStatus.Failed(outcome.error)) }
            }
        }
    }

    /** Label values show straight away; the vitamins and minerals the listing lacks follow when estimated. */
    private fun fillMissingMicros(product: ScannedProduct, draftId: Long) {
        val fromLabel = product.item.micros100g.isNotEmpty()
        // Gemini estimates from the name, so without one there's nothing to ask.
        if (product.productName == null) {
            _uiState.update { it.copy(microsNote = MicrosNote.NoProductName) }
            return
        }
        _uiState.update { it.copy(isEstimatingMicros = true, microsNote = MicrosNote.Estimating) }
        microsJob = viewModelScope.launch {
            val outcome = productRepository.estimateMissingMicros(product)
            _uiState.update { state ->
                when (outcome) {
                    is Outcome.Success -> state.copy(
                        isEstimatingMicros = false,
                        // Merged into whatever the item holds now, so edits made while waiting survive.
                        items = state.items.map { draft ->
                            if (draft.id != draftId) return@map draft
                            draft.copy(item = draft.item.copy(micros100g = outcome.value + draft.item.micros100g))
                        },
                        microsNote = if (fromLabel) MicrosNote.FromLabelAndGemini else MicrosNote.FromGemini,
                    )
                    is Outcome.Failure -> state.copy(
                        isEstimatingMicros = false,
                        microsNote = MicrosNote.Failed(outcome.error),
                    )
                }
            }
        }
    }

    private fun loadRecipe(recipeId: Long) {
        _uiState.update { it.copy(status = ReviewStatus.Loading(uiText(R.string.review_loading_recipe))) }
        lookupJob = viewModelScope.launch {
            val recipe = recipeRepository.get(recipeId)
            if (recipe == null) {
                _uiState.update {
                    it.copy(
                        status = ReviewStatus.Failed(
                            DataError.Unexpected(null),
                            uiText(R.string.review_recipe_missing),
                        ),
                    )
                }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    status = ReviewStatus.Ready,
                    items = listOf(
                        DraftItem(
                            nextDraftId++,
                            recipe.portion(recipe.servingGrams),
                            servingGrams = recipe.servingGrams,
                        ),
                    ),
                    recipeServing = RecipeServing(
                        recipe.name,
                        recipe.servingGrams,
                        recipe.totalWeight,
                        recipe.servings,
                    ),
                )
            }
        }
    }

    private fun loadPastFood(key: String) {
        _uiState.update { it.copy(status = ReviewStatus.Loading(uiText(R.string.review_loading))) }
        lookupJob = viewModelScope.launch {
            val food = foodRepository.pastFood(key)
            _uiState.update { state ->
                if (food == null) {
                    state.copy(
                        status = ReviewStatus.Failed(DataError.Unexpected(null), uiText(R.string.review_food_missing)),
                    )
                } else {
                    state.copy(status = ReviewStatus.Ready, items = listOf(DraftItem(nextDraftId++, food.item)))
                }
            }
        }
    }

    private fun loadPastMeal(key: String) {
        _uiState.update { it.copy(status = ReviewStatus.Loading(uiText(R.string.review_loading))) }
        lookupJob = viewModelScope.launch {
            showMeal(foodRepository.pastMeal(key)?.items.orEmpty())
        }
    }

    /** Another day's meal, logged again: it keeps its meal, since the button sat on that meal's card. */
    private fun copyMeal(date: LocalDate, mealTypeName: String?) {
        val mealType = MealType.entries.firstOrNull { it.name == mealTypeName } ?: return
        _uiState.update { it.copy(status = ReviewStatus.Loading(uiText(R.string.review_loading)), mealType = mealType) }
        lookupJob = viewModelScope.launch {
            showMeal(foodRepository.mealOn(date, mealType))
        }
    }

    private fun showMeal(items: List<FoodItem>) = _uiState.update { state ->
        if (items.isEmpty()) {
            state.copy(status = ReviewStatus.Failed(DataError.Unexpected(null), uiText(R.string.review_meal_missing)))
        } else {
            state.copy(status = ReviewStatus.Ready, items = items.map { DraftItem(nextDraftId++, it) })
        }
    }

    override fun onSuggestionClick(food: PastFood) = _uiState.update { state ->
        val (item, remaining) = acceptSuggestion(state.describeInput, food)
        state.copy(
            items = state.items + DraftItem(nextDraftId++, item),
            describeInput = remaining,
            describeStatus = DescribeStatus.Idle,
            status = if (state.status is ReviewStatus.Failed) ReviewStatus.Ready else state.status,
        )
    }

    override fun onHintChange(hint: String) {
        _uiState.update { it.copy(hint = hint) }
        updatePendingPhoto()
    }

    override fun onReanalyze() {
        route.photoPath?.let(::analyzePhoto)
    }

    override fun onDescribeChange(text: String) = _uiState.update {
        it.copy(
            describeInput = text,
            describeStatus = if (it.describeStatus is DescribeStatus.Failed) DescribeStatus.Idle else it.describeStatus,
        )
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
                        notes = outcome.value.notes ?: state.notes,
                        items = state.items + outcome.value.items.map {
                            DraftItem(nextDraftId++, it.item, it.confidence)
                        },
                        status = if (state.status is ReviewStatus.Failed) ReviewStatus.Ready else state.status,
                    )
                }
                is Outcome.Failure -> _uiState.update { it.copy(describeStatus = DescribeStatus.Failed(outcome.error)) }
            }
        }
    }

    override fun onMealTypeSelect(mealType: MealType) {
        _uiState.update { it.copy(mealType = mealType) }
        updatePendingPhoto()
    }

    override fun onGramsStep(draftId: Long, deltaGrams: Double) = _uiState.update { state ->
        state.copy(
            items = state.items.map { draft ->
                if (draft.id != draftId) return@map draft
                draft.copy(item = draft.item.copy(grams = (draft.item.grams + deltaGrams).coerceAtLeast(MIN_GRAMS)))
            },
        )
    }

    override fun onServingsStep(draftId: Long, deltaServings: Double) = _uiState.update { state ->
        state.copy(
            items = state.items.map { draft ->
                val servingGrams = draft.servingGrams
                if (draft.id != draftId || servingGrams == null) return@map draft
                // Snap to the step grid so 300 g + ½ serving reads "1.5 servings", not "1.49".
                val current = draft.item.grams / servingGrams
                val next = (
                    Math.round(
                        (current + deltaServings) / SERVINGS_STEP,
                    ) * SERVINGS_STEP
                    ).coerceAtLeast(SERVINGS_STEP)
                draft.copy(item = draft.item.copy(grams = next * servingGrams))
            },
        )
    }

    override fun onItemClick(draft: DraftItem) = _uiState.update { it.copy(editor = EditorTarget.Existing(draft)) }

    override fun onAddItemClick() = _uiState.update { it.copy(editor = EditorTarget.NewItem) }

    override fun onItemRemove(draftId: Long) = _uiState.update { state ->
        state.copy(items = state.items.filterNot { it.id == draftId })
    }

    override fun onEditorSave(item: FoodItem) = _uiState.update { state ->
        val items = when (val target = state.editor) {
            is EditorTarget.Existing -> state.items.map { if (it.id == target.draft.id) it.copy(item = item) else it }
            EditorTarget.NewItem, null -> state.items + DraftItem(nextDraftId++, item)
        }
        // A manual item also clears a failed analysis: the user has moved on from it.
        state.copy(
            items = items,
            editor = null,
            status = if (state.status is ReviewStatus.Failed) ReviewStatus.Ready else state.status,
        )
    }

    override fun onEditorDismiss() = _uiState.update { it.copy(editor = null) }

    override suspend fun describeItem(text: String): Outcome<AnalysisResult> = foodAnalyzer.analyzeText(text)

    override fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        pendingUpdateJob?.cancel()
        viewModelScope.launch {
            // Give a vitamin estimate that's nearly done the chance to land; a slow one isn't worth the wait.
            microsJob?.let { job -> withTimeoutOrNull(MICROS_WAIT_MILLIS) { job.join() } }
            val items = _uiState.value.items
            val saved = trySave {
                foodRepository.addAll(
                    items = items.map { it.item },
                    date = LocalDate.ofEpochDay(route.epochDay),
                    mealType = state.mealType,
                    source = when {
                        route.photoPath != null -> EntrySource.PHOTO
                        route.barcode != null -> EntrySource.BARCODE
                        route.recipeId != null -> EntrySource.RECIPE
                        route.pastFoodKey != null || route.isPastMeal -> EntrySource.HISTORY
                        route.describe -> EntrySource.TEXT
                        else -> EntrySource.MANUAL
                    },
                    photoPath = route.photoPath,
                )
            }
            if (saved) {
                photoKept = true
                _effects.send(ReviewEffect.Saved)
            } else {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(ReviewEffect.ShowMessage(SAVE_FAILED_MESSAGE))
            }
        }
    }

    override fun onCleared() {
        if (!photoKept && !_uiState.value.savedForLater) route.photoPath?.let(photoStore::deleteNow)
    }

    companion object {
        private const val MIN_GRAMS = 5.0
        private const val SAVED_REVIEW_KEY = "review"
        private const val MICROS_WAIT_MILLIS = 15_000L
        private const val PENDING_UPDATE_DELAY_MILLIS = 500L

        val Factory = viewModelFactory {
            initializer {
                ReviewViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    foodAnalyzer = appContainer.foodAnalyzer,
                    productRepository = appContainer.productRepository,
                    recipeRepository = appContainer.recipeRepository,
                    foodRepository = appContainer.foodRepository,
                    photoStore = appContainer.photoStore,
                )
            }
        }
    }
}
