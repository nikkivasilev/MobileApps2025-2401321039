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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.data.ExerciseUsage
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.ui.asString
import com.macrosnap.app.ui.components.appChipColors
import com.macrosnap.app.ui.label

@Composable
fun ExercisesScreen(
    onClose: () -> Unit,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ExercisesEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
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
    val names = displayNames(uiState.exercises)
    val filtered = uiState.filtered(names)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_exercises_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                // A search that found nothing is usually the name of the exercise the user is about to add.
                onClick = { actions.onNewClick(if (filtered.isEmpty()) uiState.query.trim() else "") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.workout_new_exercise)) },
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
                label = { Text(stringResource(R.string.workout_search_exercises)) },
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
                FilterChip(selected = uiState.groupFilter == null, onClick = {
                    actions.onGroupFilterChange(null)
                }, label = { Text(stringResource(R.string.workout_filter_all)) })
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
            if (filtered.isEmpty()) {
                Text(
                    text = if (uiState.query.isBlank()) {
                        stringResource(R.string.workout_no_exercises_in_group)
                    } else {
                        stringResource(R.string.workout_no_match, uiState.query.trim())
                    },
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
                    val name = names[exercise.id] ?: exercise.name
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(exercise.summary()) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { actions.onExerciseClick(exercise, name) },
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

@Composable
private fun Exercise.summary(): String = listOfNotNull(
    muscleGroup.label,
    if (type == ExerciseType.CARDIO) {
        pluralStringResource(
            R.plurals.workout_sets,
            defaultSets,
            defaultSets,
        )
    } else {
        "$defaultSets × $defaultReps"
    },
    if (isCustom) stringResource(R.string.workout_custom) else null,
).joinToString(" · ")

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
            Text(
                stringResource(if (editor.isNew) R.string.workout_new_exercise else R.string.workout_edit_exercise),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = { actions.onEditorChange(editor.copy(name = it)) },
                label = { Text(stringResource(R.string.workout_exercise_name)) },
                singleLine = true,
                isError = nameTaken,
                supportingText = if (nameTaken) {
                    { Text(stringResource(R.string.workout_name_taken)) }
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
                    val sets = if (parseDefaultSets(editor.setsInput) == defaultSetsFor(editor.type)) {
                        defaultSetsFor(type).toString()
                    } else {
                        editor.setsInput
                    }
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
                    TextButton(onClick = actions::onDeleteClick) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(onClick = actions::onEditorSave, enabled = canSave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (editor.isNew) R.string.workout_add_exercise else R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun DeleteExerciseDialog(name: String, usage: ExerciseUsage, actions: ExercisesActions) {
    AlertDialog(
        onDismissRequest = actions::onDeleteDismiss,
        title = { Text(stringResource(R.string.workout_delete_exercise_title, name)) },
        text = {
            val workouts = pluralStringResource(R.plurals.workout_count_workouts, usage.workouts, usage.workouts)
            val routines = pluralStringResource(R.plurals.workout_count_routines, usage.routines, usage.routines)
            Text(
                when {
                    usage.workouts > 0 && usage.routines > 0 -> stringResource(
                        R.string.workout_delete_exercise_in_both,
                        workouts,
                        routines,
                    )
                    usage.workouts > 0 -> stringResource(R.string.workout_delete_exercise_in_workouts, workouts)
                    usage.routines > 0 -> stringResource(R.string.workout_delete_exercise_in_routines, routines)
                    else -> stringResource(R.string.workout_delete_exercise_unused)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = actions::onDeleteConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = actions::onDeleteDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
