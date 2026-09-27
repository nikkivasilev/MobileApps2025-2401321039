package com.macrosnap.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.macrosnap.app.model.DataError
import com.macrosnap.app.ui.components.DescribeField
import com.macrosnap.app.ui.components.DraftItemCard
import com.macrosnap.app.ui.components.EditorTarget
import com.macrosnap.app.ui.components.ItemEditorSheet
import com.macrosnap.app.ui.components.MealTypeChips
import com.macrosnap.app.ui.summary
import com.macrosnap.app.ui.userMessage
import com.macrosnap.app.ui.whole
import java.io.File

@Composable
fun ReviewScreen(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    ReviewEffect.Saved -> onClose()
                    is ReviewEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ReviewContent(uiState = uiState, actions = viewModel, snackbarHostState = snackbarHostState, onClose = onClose, onOpenSettings = onOpenSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewContent(
    uiState: ReviewUiState,
    snackbarHostState: SnackbarHostState,
    actions: ReviewActions,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Review meal") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = { SaveBar(uiState, onSave = actions::onSave) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.photoPath?.let { path ->
                item(key = "photo") {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Meal photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            }

            item(key = "meal") {
                MealTypeChips(selected = uiState.mealType, onSelect = actions::onMealTypeSelect)
            }

            if (uiState.awaitingFirstAnalysis) {
                item(key = "first-analysis") {
                    FirstAnalysisCard(hint = uiState.hint, onHintChange = actions::onHintChange, onAnalyze = actions::onReanalyze)
                }
            }

            when (val status = uiState.status) {
                is ReviewStatus.Loading -> item(key = "loading") { LoadingRow(status.label) }
                is ReviewStatus.Failed -> item(key = "error") {
                    ErrorCard(
                        error = status.error,
                        canRetry = uiState.canReanalyze,
                        onRetry = actions::onReanalyze,
                        onOpenSettings = onOpenSettings,
                    )
                }
                ReviewStatus.Ready -> Unit
            }

            uiState.notes?.let { notes ->
                item(key = "notes") {
                    Text(
                        text = "Note: $notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.labelWarnings.isNotEmpty()) {
                item(key = "label-warnings") { LabelWarningsCard(uiState.labelWarnings) }
            }

            uiState.microsNote?.let { note ->
                item(key = "micros-note") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.isEstimatingMicros) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(uiState.items, key = { it.id }) { draft ->
                DraftItemCard(
                    draft = draft,
                    onClick = { actions.onItemClick(draft) },
                    onGramsStep = { actions.onGramsStep(draft.id, it) },
                    onRemove = { actions.onItemRemove(draft.id) },
                    onServingsStep = { actions.onServingsStep(draft.id, it) },
                )
            }

            if (uiState.status !is ReviewStatus.Loading) {
                item(key = "describe") {
                    DescribeField(
                        value = uiState.describeInput,
                        onValueChange = actions::onDescribeChange,
                        onSubmit = actions::onDescribeSubmit,
                        status = uiState.describeStatus,
                        label = "Describe what to add",
                    )
                }
                item(key = "add") {
                    OutlinedButton(onClick = actions::onAddItemClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add an item manually")
                    }
                }
            }

            if (uiState.photoPath != null && uiState.hasAnalyzed) {
                item(key = "hint") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.hint,
                            onValueChange = actions::onHintChange,
                            label = { Text("Wrong guess? Give the AI a hint") },
                            placeholder = { Text("e.g. turkey mince, cooked in butter, 3 eggs") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(onClick = actions::onReanalyze, enabled = uiState.canReanalyze) {
                            Text("Analyze again")
                        }
                    }
                }
            }
        }
    }

    uiState.editor?.let { target ->
        ItemEditorSheet(
            title = if (target is EditorTarget.Existing) "Edit item" else "Add item",
            initial = (target as? EditorTarget.Existing)?.draft?.item,
            initialMealType = null,
            onSave = { item, _ -> actions.onEditorSave(item) },
            onDelete = null,
            onDismiss = actions::onEditorDismiss,
            onDescribe = actions::describeItem,
        )
    }
}

@Composable
private fun FirstAnalysisCard(hint: String, onHintChange: (String) -> Unit, onAnalyze: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("What's on the plate?", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "A few words help the AI a lot: the dish, how it was cooked, anything hidden (oil, sauce, sugar). Leave it empty to let it guess.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = hint,
                onValueChange = onHintChange,
                label = { Text("Hint for the AI (optional)") },
                placeholder = { Text("e.g. chicken curry with basmati rice, cooked in ghee") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) {
                Text(if (hint.isBlank()) "Analyze photo" else "Analyze with hint")
            }
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorCard(error: DataError, canRetry: Boolean, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    val needsSettings = error is DataError.NoApiKey || error is DataError.InvalidApiKey || error is DataError.ModelUnavailable
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(error.userMessage(), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsSettings) {
                    Button(onClick = onOpenSettings) { Text("Open Settings") }
                }
                if (canRetry && !needsSettings) {
                    Button(onClick = onRetry) { Text("Try again") }
                }
            }
        }
    }
}

/** Crowd-sourced listings can be wrong; say what looked off rather than log it silently. */
@Composable
private fun LabelWarningsCard(warnings: List<String>) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Check this against the label", style = MaterialTheme.typography.titleSmall)
            warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Text("Tap the item to correct any value.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SaveBar(uiState: ReviewUiState, onSave: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${uiState.totals.kcal.whole()} kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = uiState.totals.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onSave, enabled = uiState.canSave) {
                Text(if (uiState.items.size > 1) "Log ${uiState.items.size} items" else "Log it")
            }
        }
    }
}
