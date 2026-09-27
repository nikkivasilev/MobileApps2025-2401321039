package com.macrosnap.app.ui.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.Recipe
import com.macrosnap.app.model.SupplementStatus
import com.macrosnap.app.model.sum
import com.macrosnap.app.ui.ReviewRoute
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.CalorieRing
import com.macrosnap.app.ui.components.EntryDetailSheet
import com.macrosnap.app.ui.components.ItemEditorSheet
import com.macrosnap.app.ui.components.MacroBar
import com.macrosnap.app.ui.components.NutrientsCard
import com.macrosnap.app.ui.components.SleepCard
import com.macrosnap.app.ui.components.SleepSheet
import com.macrosnap.app.ui.components.WaterCard
import com.macrosnap.app.ui.summary
import com.macrosnap.app.ui.theme.MacroColors
import com.macrosnap.app.ui.whole
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal enum class AddMethod(val emoji: String, val title: String, val subtitle: String) {
    CAMERA("📷", "Take a photo", "AI estimates the items and macros"),
    GALLERY("🖼️", "Choose from gallery", "Analyze a photo you already took"),
    DESCRIBE("💬", "Describe in words", "\"2 eggs, 50 g oats, a banana\" - AI fills in the rest"),
    RECIPE("🍲", "From my recipes", "Log a serving of a dish you've saved"),
    BARCODE("🏷️", "Scan a barcode", "Exact values for packaged food"),
    MANUAL("✏️", "Enter manually", "Type the name and nutrition yourself"),
}

@Composable
fun TodayScreen(
    onOpenReview: (ReviewRoute) -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val epochDay = uiState.date.toEpochDay()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is TodayEffect.OpenPhotoReview -> onOpenReview(ReviewRoute(effect.epochDay, photoPath = effect.photoPath))
                    is TodayEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // Saveable because a mid-range phone may kill the app while the camera is in front.
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path != null) {
            if (saved) onOpenReview(ReviewRoute(epochDay, photoPath = path)) else viewModel.onPhotoDiscarded(path)
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    var showRecipePicker by rememberSaveable { mutableStateOf(false) }

    fun onAdd(method: AddMethod) {
        when (method) {
            AddMethod.CAMERA -> {
                val target = viewModel.newPhotoTarget()
                pendingPhotoPath = target.path
                takePicture.launch(target.uri)
            }
            AddMethod.GALLERY -> pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            AddMethod.DESCRIBE -> onOpenReview(ReviewRoute(epochDay, describe = true))
            AddMethod.RECIPE -> showRecipePicker = true
            AddMethod.BARCODE -> {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                    .build()
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { barcode ->
                        barcode.rawValue?.let { onOpenReview(ReviewRoute(epochDay, barcode = it)) }
                    }
                    .addOnFailureListener { viewModel.onScanFailed() }
            }
            AddMethod.MANUAL -> onOpenReview(ReviewRoute(epochDay))
        }
    }

    TodayContent(
        uiState = uiState,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onAdd = ::onAdd,
        onPastFood = { onOpenReview(ReviewRoute(epochDay, pastFoodKey = it.key)) },
        onDescribe = { onOpenReview(ReviewRoute(epochDay, describe = true, describeText = it)) },
    )

    if (showRecipePicker) {
        RecipePickerSheet(
            recipes = uiState.recipes,
            onSelect = { recipe ->
                showRecipePicker = false
                onOpenReview(ReviewRoute(epochDay, recipeId = recipe.id))
            },
            onDismiss = { showRecipePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayContent(
    uiState: TodayUiState,
    actions: TodayActions,
    snackbarHostState: SnackbarHostState,
    onAdd: (AddMethod) -> Unit,
    onPastFood: (PastFood) -> Unit,
    onDescribe: (String) -> Unit,
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.date.friendlyLabel(),
                        modifier = Modifier.clickable(enabled = !uiState.isToday, onClick = actions::onJumpToToday),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions::onPreviousDay) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                },
                actions = {
                    IconButton(onClick = actions::onNextDay, enabled = !uiState.isToday) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add food") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "summary") { SummaryCard(uiState) }
            if (uiState.supplements.isNotEmpty()) {
                item(key = "supplements") {
                    SupplementsCard(
                        supplements = uiState.supplements,
                        onTake = actions::onSupplementTaken,
                        onUntake = actions::onSupplementUntaken,
                    )
                }
            }
            item(key = "water") {
                WaterCard(
                    totalMl = uiState.water?.totalMl ?: 0,
                    goalMl = uiState.waterGoalMl,
                    canUndo = (uiState.water?.entries ?: 0) > 0,
                    onAdd = actions::onWaterAdd,
                    onUndo = actions::onWaterUndo,
                )
            }
            item(key = "sleep") {
                SleepCard(sleep = uiState.sleep, goalHours = uiState.sleepGoalHours, onClick = actions::onSleepClick)
            }
            item(key = "nutrients") { NutrientsCard(totals = uiState.nutrientTotals, targets = uiState.nutrientTargets) }

            if (uiState.entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Nothing logged yet. Tap \"Add food\" and snap your plate.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp, horizontal = 8.dp),
                    )
                }
            }

            uiState.entriesByMeal.forEach { (mealType, entries) ->
                item(key = "header-${mealType.name}") { MealHeader(mealType, entries) }
                items(entries, key = { it.id }) { entry ->
                    EntryRow(entry, onClick = { actions.onEntryClick(entry) })
                }
            }
        }
    }

    if (showAddSheet) {
        AddFoodSheet(
            pastFoods = uiState.pastFoods,
            onMethod = {
                showAddSheet = false
                onAdd(it)
            },
            onPastFood = {
                showAddSheet = false
                onPastFood(it)
            },
            onDescribe = {
                showAddSheet = false
                onDescribe(it)
            },
            onHide = actions::onHidePastFood,
            onDismiss = { showAddSheet = false },
        )
    }

    if (uiState.showSleepSheet) {
        SleepSheet(
            date = uiState.date,
            initial = uiState.sleep,
            onSave = actions::onSleepSave,
            onDelete = if (uiState.sleep != null) actions::onSleepDelete else null,
            onDismiss = actions::onSleepDismiss,
        )
    }

    uiState.viewing?.let { entry ->
        EntryDetailSheet(
            entry = entry,
            onEdit = actions::onEditClick,
            onDelete = actions::onEntryDeleted,
            onLogAgain = actions::onLogAgain,
            logsToToday = !uiState.isToday,
            onDismiss = actions::onDetailDismiss,
        )
    }

    uiState.editing?.let { entry ->
        ItemEditorSheet(
            title = "Edit entry",
            initial = entry.item,
            initialMealType = entry.mealType,
            onSave = actions::onEntryEdited,
            onDelete = actions::onEntryDeleted,
            onDismiss = actions::onEditorDismiss,
            onDescribe = actions::describeItem,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(recipes: List<Recipe>, onSelect: (Recipe) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "My recipes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (recipes.isEmpty()) {
                Text(
                    text = "No recipes yet. Create one in the Recipes tab.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
            recipes.forEach { recipe ->
                ListItem(
                    headlineContent = { Text(recipe.name) },
                    supportingContent = {
                        Text("${recipe.servingGrams.whole()} g serving · ${recipe.perServing.kcal.whole()} kcal · ${recipe.perServing.summary()}")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelect(recipe) },
                )
            }
        }
    }
}

@Composable
private fun SupplementsCard(
    supplements: List<SupplementStatus>,
    onTake: (Long) -> Unit,
    onUntake: (Long) -> Unit,
) {
    // Taken doses fold away so the card only takes room for what's still due.
    var showTaken by rememberSaveable { mutableStateOf(false) }
    val done = supplements.count { it.isDone }
    val visible = if (showTaken) supplements else supplements.filterNot { it.isDone }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = done > 0) { showTaken = !showTaken },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Supplements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = if (done == supplements.size) "All taken" else "$done / ${supplements.size} taken",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (done > 0) {
                    Icon(
                        imageVector = if (showTaken) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (showTaken) "Hide taken" else "Show taken",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            visible.forEach { status ->
                val supplement = status.supplement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(supplement.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = supplement.doseLabel + if (supplement.timesPerDay > 1) " · ${supplement.timesPerDay}× a day" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (supplement.timesPerDay == 1) {
                        Checkbox(
                            checked = status.isDone,
                            onCheckedChange = { checked -> if (checked) onTake(supplement.id) else onUntake(supplement.id) },
                        )
                    } else {
                        FilledTonalIconButton(onClick = { onUntake(supplement.id) }, enabled = status.takenCount > 0) {
                            Text("−", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "${status.takenCount}/${supplement.timesPerDay}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        FilledTonalIconButton(onClick = { onTake(supplement.id) }, enabled = !status.isDone) {
                            Text("+", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(uiState: TodayUiState) {
    val totals = uiState.totals
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalorieRing(
                consumedKcal = totals.kcal,
                goalKcal = uiState.effectiveKcalGoal,
                modifier = Modifier.size(132.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${totals.kcal.whole()} of ${uiState.effectiveKcalGoal} kcal",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (uiState.workoutBurnKcal > 0) {
                    Text(
                        text = "Gym: ≈${uiState.workoutBurnKcal} kcal burned" + if (uiState.addWorkoutBurn) " (added to goal)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MacroBar("Protein", MacroColors.Protein, totals.protein, uiState.goals.protein)
                MacroBar("Carbs", MacroColors.Carbs, totals.carbs, uiState.goals.carbs)
                MacroBar("Fat", MacroColors.Fat, totals.fat, uiState.goals.fat)
            }
        }
    }
}

@Composable
private fun MealHeader(mealType: MealType, entries: List<FoodEntry>) {
    val subtotal = entries.map { it.item.total }.sum()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(mealType.label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${subtotal.kcal.whole()} kcal · ${subtotal.summary()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EntryRow(entry: FoodEntry, onClick: () -> Unit) {
    val total = entry.item.total
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            entry.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${entry.item.grams.compact()} g · ${total.summary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${total.kcal.whole()} kcal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM")

private fun LocalDate.friendlyLabel(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> format(DAY_FORMAT)
    }
}
