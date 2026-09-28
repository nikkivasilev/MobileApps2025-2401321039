package com.macrosnap.app.ui.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.macrosnap.app.model.PendingPhoto
import com.macrosnap.app.model.Recipe
import com.macrosnap.app.model.SupplementStatus
import com.macrosnap.app.model.sum
import com.macrosnap.app.ui.ReviewRoute
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.components.CalorieHero
import com.macrosnap.app.ui.components.CardHeader
import com.macrosnap.app.ui.components.EntryDetailSheet
import com.macrosnap.app.ui.components.IconBadge
import com.macrosnap.app.ui.components.ItemEditorSheet
import com.macrosnap.app.ui.components.MacroDots
import com.macrosnap.app.ui.components.MacroTile
import com.macrosnap.app.ui.components.NutrientsCard
import com.macrosnap.app.ui.components.ProgressRing
import com.macrosnap.app.ui.components.SectionTitle
import com.macrosnap.app.ui.components.SleepCard
import com.macrosnap.app.ui.components.SleepSheet
import com.macrosnap.app.ui.components.WaterCard
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme
import com.macrosnap.app.ui.theme.MacroColors
import com.macrosnap.app.ui.whole
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

internal enum class AddMethod(val icon: ImageVector, val label: String, val title: String, val subtitle: String) {
    CAMERA(AppIcons.Camera, "Snap", "Take a photo", "AI estimates the items and macros"),
    DESCRIBE(AppIcons.ChatBubble, "Describe", "Describe in words", "\"2 eggs, 50 g oats, a banana\" - AI fills in the rest"),
    BARCODE(AppIcons.Barcode, "Barcode", "Scan a barcode", "Exact values for packaged food"),
    GALLERY(AppIcons.Image, "Gallery", "Choose from gallery", "Analyze a photo you already took"),
    RECIPE(AppIcons.Restaurant, "Recipe", "From my recipes", "Log a serving of a dish you've saved"),
    MANUAL(Icons.Filled.Edit, "Manual", "Enter manually", "Type the name and nutrition yourself"),
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
        onResumePending = { onOpenReview(ReviewRoute(it.date.toEpochDay(), photoPath = it.photoPath, resumePending = true)) },
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
    onResumePending: (PendingPhoto) -> Unit,
    onDescribe: (String) -> Unit,
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add food", modifier = Modifier.size(30.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { DayHeader(uiState, actions) }
            item(key = "week") {
                WeekStrip(
                    days = uiState.weekDays,
                    selected = uiState.date,
                    kcalByDay = uiState.weekKcal,
                    goalKcal = uiState.goals.kcal,
                    onSelect = actions::onSelectDate,
                )
            }
            item(key = "summary") {
                CalorieHero(
                    consumedKcal = uiState.totals.kcal,
                    goalKcal = uiState.effectiveKcalGoal,
                    burnedKcal = uiState.workoutBurnKcal,
                    burnAddedToGoal = uiState.addWorkoutBurn,
                )
            }
            item(key = "macros") {
                val totals = uiState.totals
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroTile("Protein", MacroColors.Protein, totals.protein, uiState.goals.protein, Modifier.weight(1f))
                    MacroTile("Carbs", MacroColors.Carbs, totals.carbs, uiState.goals.carbs, Modifier.weight(1f))
                    MacroTile("Fat", MacroColors.Fat, totals.fat, uiState.goals.fat, Modifier.weight(1f))
                }
            }

            if (uiState.pendingPhotos.isNotEmpty()) {
                item(key = "pending") {
                    PendingPhotosCard(
                        photos = uiState.pendingPhotos,
                        onResume = onResumePending,
                        onDiscard = actions::onDiscardPendingPhoto,
                    )
                }
            }

            item(key = "meals-title") {
                SectionTitle("Meals") {
                    if (uiState.entries.isNotEmpty()) {
                        Text(
                            text = "${uiState.entries.size} ${if (uiState.entries.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (uiState.entries.isEmpty()) {
                item(key = "empty") { EmptyMealsCard(isToday = uiState.isToday, onAdd = { showAddSheet = true }) }
            }
            uiState.entriesByMeal.forEach { (mealType, entries) ->
                item(key = "meal-${mealType.name}") {
                    MealCard(mealType, entries, onEntryClick = actions::onEntryClick)
                }
            }

            item(key = "habits-title") { SectionTitle("Habits") }
            item(key = "water-sleep") {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WaterCard(
                        totalMl = uiState.water?.totalMl ?: 0,
                        goalMl = uiState.waterGoalMl,
                        canUndo = (uiState.water?.entries ?: 0) > 0,
                        onAdd = actions::onWaterAdd,
                        onUndo = actions::onWaterUndo,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    SleepCard(
                        sleep = uiState.sleep,
                        goalHours = uiState.sleepGoalHours,
                        onClick = actions::onSleepClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
            if (uiState.supplements.isNotEmpty()) {
                item(key = "supplements") {
                    SupplementsCard(
                        supplements = uiState.supplements,
                        onTake = actions::onSupplementTaken,
                        onUntake = actions::onSupplementUntaken,
                    )
                }
            }
            item(key = "nutrients") { NutrientsCard(totals = uiState.nutrientTotals, targets = uiState.nutrientTargets) }
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

/** "Today" (or the day being browsed) with the full date under it, and week-by-week arrows. */
@Composable
private fun DayHeader(uiState: TodayUiState, actions: TodayActions) {
    val today = LocalDate.now()
    val lastWeekDay = uiState.weekDays.last()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(uiState.date.friendlyLabel(), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = uiState.date.format(FULL_DAY_FORMAT),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!uiState.isToday) {
            FilledTonalButton(
                onClick = actions::onJumpToToday,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp),
            ) { Text("Today") }
        }
        IconButton(onClick = { actions.onSelectDate(uiState.date.minusWeeks(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        IconButton(
            onClick = { actions.onSelectDate(minOf(uiState.date.plusWeeks(1), today)) },
            enabled = lastWeekDay < today,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

/** Monday to Sunday, each day a small ring of calories against the goal; tap a day to open it. */
@Composable
private fun WeekStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    kcalByDay: Map<LocalDate, Double>,
    goalKcal: Int,
    onSelect: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val isSelected = day == selected
            val isFuture = day > today
            val kcal = kcalByDay[day] ?: 0.0
            val over = goalKcal > 0 && kcal > goalKcal * 1.05
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = !isFuture) { onSelect(day) }
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                ProgressRing(
                    progress = if (goalKcal > 0) (kcal / goalKcal).toFloat() else 0f,
                    color = if (over) MaterialTheme.colorScheme.error else AppTheme.colors.calories,
                    strokeWidth = 3.dp,
                    trackColor = if (isFuture) Color.Transparent else AppTheme.colors.track,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (day == today || isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isFuture -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Photos Gemini couldn't take when they were sent; each can be sent again or thrown away. */
@Composable
private fun PendingPhotosCard(photos: List<PendingPhoto>, onResume: (PendingPhoto) -> Unit, onDiscard: (PendingPhoto) -> Unit) {
    var discarding by remember { mutableStateOf<PendingPhoto?>(null) }
    AppCard {
        CardHeader(
            title = "Waiting to analyze",
            icon = Icons.Filled.Refresh,
            tint = AppTheme.colors.carbs,
            subtitle = "Gemini couldn't take ${if (photos.size == 1) "this photo" else "these photos"} earlier. Try again now.",
        )
        photos.forEach { photo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodThumbnail(photo.photoPath, photo.mealType.label)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${photo.mealType.label} · ${photo.date.friendlyLabel()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = photo.hint.ifBlank { "No hint" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { discarding = photo }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Discard photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(
                    onClick = { onResume(photo) },
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(36.dp),
                ) { Text("Analyze") }
            }
        }
    }

    discarding?.let { photo ->
        AlertDialog(
            onDismissRequest = { discarding = null },
            title = { Text("Discard this photo?") },
            text = { Text("It's deleted without being analyzed. Nothing was logged from it.") },
            confirmButton = {
                TextButton(onClick = {
                    discarding = null
                    onDiscard(photo)
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { discarding = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyMealsCard(isToday: Boolean, onAdd: () -> Unit) {
    AppCard(contentPadding = PaddingValues(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(AppIcons.Camera, MaterialTheme.colorScheme.onSurface, size = 56.dp)
            Spacer(Modifier.height(12.dp))
            Text(if (isToday) "Nothing logged yet" else "Nothing logged this day", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Snap your plate, scan a barcode or just describe it. The AI works out the macros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Button(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add food")
            }
        }
    }
}

/** One meal: a header with its calories, then each entry with its photo (or a placeholder). */
@Composable
private fun MealCard(mealType: MealType, entries: List<FoodEntry>, onEntryClick: (FoodEntry) -> Unit) {
    val subtotal = entries.map { it.item.total }.sum()
    AppCard(contentPadding = PaddingValues(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(mealType.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = "${subtotal.kcal.whole()} kcal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 82.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            EntryRow(entry, onClick = { onEntryClick(entry) })
        }
    }
}

@Composable
private fun EntryRow(entry: FoodEntry, onClick: () -> Unit) {
    val total = entry.item.total
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodThumbnail(entry.photoPath, entry.item.name)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.item.grams.compact()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                MacroDots(total)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(total.kcal.whole(), style = MaterialTheme.typography.titleMedium)
            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The entry's photo, or its initial on a tile when it was logged without one. */
@Composable
private fun FoodThumbnail(photoPath: String?, name: String) {
    val shape = RoundedCornerShape(14.dp)
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(AppTheme.colors.tile, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(recipes: List<Recipe>, onSelect: (Recipe) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "My recipes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            if (recipes.isEmpty()) {
                Text(
                    text = "No recipes yet. Create one in the Recipes tab.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                )
            }
            recipes.forEach { recipe ->
                AppCard(onClick = { onSelect(recipe) }) {
                    Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${recipe.servingGrams.whole()} g serving · ${recipe.perServing.kcal.whole()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                    )
                    MacroDots(recipe.perServing)
                }
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

    AppCard {
        CardHeader(
            title = "Supplements",
            icon = AppIcons.Pill,
            tint = AppTheme.colors.micros,
            subtitle = if (done == supplements.size) "All taken today" else "$done of ${supplements.size} taken",
            modifier = Modifier.clickable(enabled = done > 0) { showTaken = !showTaken },
        ) {
            if (done > 0) {
                Icon(
                    imageVector = if (showTaken) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (showTaken) "Hide taken" else "Show taken",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (visible.isNotEmpty()) Spacer(Modifier.height(8.dp))
        visible.forEach { status ->
            val supplement = status.supplement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(supplement.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = supplement.doseLabel + if (supplement.timesPerDay > 1) " · ${supplement.timesPerDay}× a day" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (supplement.timesPerDay == 1) {
                    CheckCircle(
                        checked = status.isDone,
                        onToggle = { if (status.isDone) onUntake(supplement.id) else onTake(supplement.id) },
                        contentDescription = if (status.isDone) "Mark ${supplement.name} not taken" else "Mark ${supplement.name} taken",
                    )
                } else {
                    Stepper(
                        value = "${status.takenCount}/${supplement.timesPerDay}",
                        onMinus = { onUntake(supplement.id) },
                        onPlus = { onTake(supplement.id) },
                        minusEnabled = status.takenCount > 0,
                        plusEnabled = !status.isDone,
                    )
                }
            }
        }
    }
}

/** A round check that fills green when done; larger and friendlier than a checkbox. */
@Composable
private fun CheckCircle(checked: Boolean, onToggle: () -> Unit, contentDescription: String) {
    val green = AppTheme.colors.micros
    IconButton(onClick = onToggle) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (checked) green else Color.Transparent, CircleShape)
                .border(2.dp, if (checked) green else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, contentDescription = contentDescription, tint = AppTheme.colors.onData, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, minusEnabled: Boolean, plusEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.size(36.dp)) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 10.dp))
        FilledTonalIconButton(onClick = onPlus, enabled = plusEnabled, modifier = Modifier.size(36.dp)) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private val FULL_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM")

private fun LocalDate.friendlyLabel(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> format(DAY_FORMAT)
    }
}
