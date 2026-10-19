package com.macrosnap.app.ui.workouts

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.ui.asString
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.label

@Composable
fun RoutineEditorScreen(
    onClose: () -> Unit,
    viewModel: RoutineEditorViewModel = viewModel(factory = RoutineEditorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    RoutineEditorEffect.Close -> onClose()
                    is RoutineEditorEffect.ShowMessage -> snackbarHostState.showSnackbar(
                        effect.message.asString(context),
                    )
                }
            }
        }
    }

    RoutineEditorContent(
        uiState = uiState,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineEditorContent(
    uiState: RoutineEditorUiState,
    snackbarHostState: SnackbarHostState,
    actions: RoutineEditorActions,
    onClose: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isExisting) R.string.workout_edit_routine else R.string.workout_new_routine,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (uiState.isExisting) {
                        IconButton(onClick = actions::onDeleteClick) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.workout_delete_routine_cd),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(onClick = actions::onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.workout_save_routine))
                    }
                }
            }
        },
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
                    label = { Text(stringResource(R.string.workout_routine_name)) },
                    placeholder = { Text(stringResource(R.string.workout_routine_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "help") {
                Text(
                    text = stringResource(R.string.workout_routine_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(uiState.drafts, key = { index, draft -> "${draft.exercise.id}-$index" }) { index, draft ->
                val name = draft.exercise.displayName
                AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                draft.exercise.muscleGroup.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = draft.setsInput,
                            onValueChange = {
                                actions.onDraftChange(index, draft.copy(setsInput = it.filter(Char::isDigit).take(2)))
                            },
                            label = { Text(stringResource(R.string.workout_sets_label)) },
                            singleLine = true,
                            isError = draft.sets == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                        OutlinedTextField(
                            value = draft.repsInput,
                            onValueChange = {
                                actions.onDraftChange(index, draft.copy(repsInput = it.filter(Char::isDigit).take(3)))
                            },
                            label = { Text(stringResource(R.string.workout_reps_label)) },
                            singleLine = true,
                            isError = draft.reps == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                        IconButton(onClick = { actions.onDraftRemove(index) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.workout_remove_exercise_cd, name),
                            )
                        }
                    }
                }
            }
            item(key = "add") {
                OutlinedButton(onClick = actions::onAddExerciseClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.workout_add_exercise))
                }
            }
        }
    }

    if (uiState.showPicker) {
        ExercisePickerSheet(
            exercises = uiState.exercises,
            onSelect = actions::onExerciseSelected,
            onCreate = actions::onExerciseCreated,
            onDismiss = actions::onPickerDismiss,
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = actions::onDeleteDismiss,
            title = { Text(stringResource(R.string.workout_delete_routine_title)) },
            text = { Text(stringResource(R.string.workout_delete_routine_body)) },
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
}
