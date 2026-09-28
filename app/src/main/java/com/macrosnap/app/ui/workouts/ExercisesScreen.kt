package com.macrosnap.app.ui.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.data.ExerciseUsage
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.ui.components.appChipColors

@Composable
fun ExercisesScreen(
    onClose: () -> Unit,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ExercisesEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ExercisesContent(uiState = uiState, actions = viewModel, snackbarHostState = snackbarHostState, onClose = onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisesContent(
    uiState: ExercisesUiState,
    actions: ExercisesActions,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = actions::onNewClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New exercise") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = actions::onQueryChange,
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = uiState.groupFilter == null, onClick = { actions.onGroupFilterChange(null) }, label = { Text("All") })
                MuscleGroup.entries.forEach { option ->
                    FilterChip(
                        colors = appChipColors(),
                        shape = CircleShape,
                        selected = uiState.groupFilter == option,
                        onClick = { actions.onGroupFilterChange(if (uiState.groupFilter == option) null else option) },
                        label = { Text(option.label) },
                    )
                }
            }
            val filtered = uiState.filtered
            if (filtered.isEmpty()) {
                Text(
                    text = if (uiState.query.isBlank()) "No exercises in this group yet." else "No match. Tap \"New exercise\" to add \"${uiState.query.trim()}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(exercise.summary()) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { actions.onExerciseClick(exercise) },
                    )
                }
            }
        }
    }

    uiState.editor?.let { editor ->
        ExerciseEditorSheet(
            editor = editor,
            nameTaken = uiState.editorNameTaken,
            canSave = uiState.canSaveEditor,
            actions = actions,
        )
    }

    val usage = uiState.deleteUsage
    val editor = uiState.editor
    if (usage != null && editor != null) {
        DeleteExerciseDialog(name = editor.name.trim(), usage = usage, actions = actions)
    }
}

private fun Exercise.summary(): String = buildString {
    append(muscleGroup.label)
    append(" · ")
    append(if (type == ExerciseType.CARDIO) "$defaultSets ${if (defaultSets == 1) "set" else "sets"}" else "$defaultSets × $defaultReps")
    if (isCustom) append(" · custom")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseEditorSheet(
    editor: ExerciseEditorState,
    nameTaken: Boolean,
    canSave: Boolean,
    actions: ExercisesActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions::onEditorDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (editor.isNew) "New exercise" else "Edit exercise", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = editor.name,
                onValueChange = { actions.onEditorChange(editor.copy(name = it)) },
                label = { Text("Name") },
                singleLine = true,
                isError = nameTaken,
                supportingText = if (nameTaken) {
                    { Text("You already have an exercise with this name.") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            ExerciseDetailsFields(
                group = editor.group,
                onGroupChange = { actions.onEditorChange(editor.copy(group = it)) },
                type = editor.type,
                onTypeChange = { type ->
                    // Follow the type's usual set count unless the user already typed their own.
                    val sets = if (parseDefaultSets(editor.setsInput) == defaultSetsFor(editor.type)) defaultSetsFor(type).toString() else editor.setsInput
                    actions.onEditorChange(editor.copy(type = type, setsInput = sets))
                },
                setsInput = editor.setsInput,
                onSetsChange = { actions.onEditorChange(editor.copy(setsInput = it)) },
                repsInput = editor.repsInput,
                onRepsChange = { actions.onEditorChange(editor.copy(repsInput = it)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!editor.isNew) {
                    TextButton(onClick = actions::onDeleteClick) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                Button(onClick = actions::onEditorSave, enabled = canSave, modifier = Modifier.weight(1f)) {
                    Text(if (editor.isNew) "Add exercise" else "Save")
                }
            }
        }
    }
}

@Composable
private fun DeleteExerciseDialog(name: String, usage: ExerciseUsage, actions: ExercisesActions) {
    AlertDialog(
        onDismissRequest = actions::onDeleteDismiss,
        title = { Text("Delete $name?") },
        text = {
            Text(
                if (usage.isUsed) {
                    val parts = listOfNotNull(
                        usage.workouts.takeIf { it > 0 }?.let { "its sets in $it ${if (it == 1) "workout" else "workouts"}" },
                        usage.routines.takeIf { it > 0 }?.let { "it from $it ${if (it == 1) "routine" else "routines"}" },
                    )
                    "This also removes ${parts.joinToString(" and ")}, including its progress history. This can't be undone."
                } else {
                    "It isn't used in any workout or routine."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = actions::onDeleteConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = actions::onDeleteDismiss) { Text("Cancel") } },
    )
}
