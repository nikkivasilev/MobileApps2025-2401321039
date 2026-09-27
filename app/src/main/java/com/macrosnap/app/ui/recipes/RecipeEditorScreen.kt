package com.macrosnap.app.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.ui.components.DescribeField
import com.macrosnap.app.ui.components.DraftItemCard
import com.macrosnap.app.ui.components.EditorTarget
import com.macrosnap.app.ui.components.ItemEditorSheet
import com.macrosnap.app.ui.summary
import com.macrosnap.app.ui.whole

@Composable
fun RecipeEditorScreen(
    onClose: () -> Unit,
    viewModel: RecipeEditorViewModel = viewModel(factory = RecipeEditorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    RecipeEditorEffect.Close -> onClose()
                    is RecipeEditorEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    RecipeEditorContent(uiState = uiState, actions = viewModel, snackbarHostState = snackbarHostState, onClose = onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeEditorContent(uiState: RecipeEditorUiState, snackbarHostState: SnackbarHostState, actions: RecipeEditorActions, onClose: () -> Unit) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isExisting) "Edit recipe" else "New recipe") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isExisting) {
                        IconButton(onClick = actions::onDeleteClick) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete recipe")
                        }
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
            item(key = "name") {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = actions::onNameChange,
                    label = { Text("Recipe name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "portions") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.servingsInput,
                        onValueChange = actions::onServingsChange,
                        label = { Text("Servings") },
                        singleLine = true,
                        isError = uiState.servings == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = uiState.cookedWeightInput,
                        onValueChange = actions::onCookedWeightChange,
                        label = { Text("Cooked weight (g)") },
                        placeholder = { Text(uiState.preview.ingredientWeight.whole()) },
                        singleLine = true,
                        isError = !uiState.cookedWeightValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
            item(key = "portions-help") {
                Text(
                    text = "Weigh the finished dish if you can: rice and stews gain water, roasts lose it, " +
                        "so this is what makes the per-100 g values right. Leave it blank to use the ingredient total.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item(key = "ingredients-title") {
                Text("Ingredients", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            item(key = "describe") {
                DescribeField(
                    value = uiState.describeInput,
                    onValueChange = actions::onDescribeChange,
                    onSubmit = actions::onDescribeSubmit,
                    status = uiState.describeStatus,
                    label = "Describe ingredients to add",
                )
            }
            items(uiState.ingredients, key = { it.id }) { draft ->
                DraftItemCard(
                    draft = draft,
                    onClick = { actions.onIngredientClick(draft) },
                    onGramsStep = { actions.onGramsStep(draft.id, it) },
                    onRemove = { actions.onIngredientRemove(draft.id) },
                )
            }
            item(key = "add") {
                OutlinedButton(onClick = actions::onAddIngredientClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add an ingredient manually")
                }
            }
        }
    }

    uiState.editor?.let { target ->
        ItemEditorSheet(
            title = if (target is EditorTarget.Existing) "Edit ingredient" else "Add ingredient",
            initial = (target as? EditorTarget.Existing)?.draft?.item,
            initialMealType = null,
            onSave = { item, _ -> actions.onEditorSave(item) },
            onDelete = null,
            onDismiss = actions::onEditorDismiss,
            onDescribe = actions::describeItem,
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = actions::onDeleteDismiss,
            title = { Text("Delete this recipe?") },
            text = { Text("Meals you already logged from it stay in your log.") },
            confirmButton = {
                TextButton(onClick = actions::onDeleteConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = actions::onDeleteDismiss) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SaveBar(uiState: RecipeEditorUiState, onSave: () -> Unit) {
    val preview = uiState.preview
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Per serving: ${preview.perServing.kcal.whole()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${preview.servingGrams.whole()} g · ${preview.perServing.summary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Whole recipe: ${preview.totalWeight.whole()} g · ${preview.total.kcal.whole()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onSave, enabled = uiState.canSave) { Text("Save") }
        }
    }
}
