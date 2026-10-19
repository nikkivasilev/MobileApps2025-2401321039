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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.PersonalRecord
import com.macrosnap.app.model.WorkoutExercise
import com.macrosnap.app.model.WorkoutSet
import com.macrosnap.app.ui.DatePatterns
import com.macrosnap.app.ui.asString
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.formatted
import com.macrosnap.app.ui.label
import com.macrosnap.app.ui.whole
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(
    onClose: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    WorkoutEffect.Close -> onClose()
                    is WorkoutEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }

    WorkoutContent(uiState = uiState, actions = viewModel, snackbarHostState = snackbarHostState, onClose = onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutContent(
    uiState: WorkoutUiState,
    actions: WorkoutActions,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
) {
    val workout = uiState.workout

    // One clock for the elapsed time and the rest countdown.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(uiState.isActive) {
        while (uiState.isActive) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val restRemaining = uiState.restEndsAt?.let { ((it - now) / 1000).toInt() }
    LaunchedEffect(restRemaining) {
        if (restRemaining != null && restRemaining <= 0) actions.onRestElapsed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            workout == null -> ""
                            workout.isActive -> stringResource(R.string.workout_minutes, (now - workout.startedAt) / 60_000)
                            else -> workout.date.formatted(DatePatterns.SHORT)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                actions = {
                    if (workout != null && workout.isActive) {
                        Button(onClick = actions::onFinishClick, enabled = uiState.canFinish, modifier = Modifier.padding(end = 8.dp)) {
                            Text(stringResource(R.string.workout_finish))
                        }
                    } else if (workout != null) {
                        IconButton(onClick = actions::onDeleteClick) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.workout_delete_cd)) }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (workout == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            if (restRemaining != null && restRemaining > 0) {
                RestBar(remainingSeconds = restRemaining, onExtend = actions::onRestExtend, onSkip = actions::onRestSkip)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "name") {
                    OutlinedTextField(
                        value = workout.name,
                        onValueChange = actions::onNameChange,
                        label = { Text(stringResource(R.string.workout_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!workout.isActive) {
                    item(key = "done-summary") {
                        Text(
                            text = stringResource(
                                R.string.workout_finished_note,
                                workoutStats(workout.durationMinutes, workout.doneSets, workout.volumeKg, uiState.burnKcal),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                workout.exercises.forEach { entry ->
                    item(key = "exercise-${entry.exercise.id}") {
                        ExerciseCard(
                            entry = entry,
                            inputs = uiState.inputs,
                            lastSets = uiState.lastSets[entry.exercise.id].orEmpty(),
                            actions = actions,
                        )
                    }
                }
                item(key = "add-exercise") {
                    OutlinedButton(onClick = actions::onAddExerciseClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.workout_add_exercise))
                    }
                }
                if (workout.isActive) {
                    item(key = "cancel") {
                        TextButton(onClick = actions::onDeleteClick, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.workout_discard), color = MaterialTheme.colorScheme.error)
                        }
                    }
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

    uiState.summary?.let { summary ->
        AlertDialog(
            onDismissRequest = actions::onSummaryDismiss,
            title = { Text(stringResource(R.string.workout_done_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(workoutStats(summary.durationMinutes, summary.sets, summary.volumeKg, summary.burnKcal))
                    if (summary.records.isEmpty()) {
                        Text(stringResource(R.string.workout_no_records))
                    } else {
                        Text(stringResource(R.string.workout_new_records), style = MaterialTheme.typography.titleSmall)
                        summary.records.forEach { Text(stringResource(R.string.workout_record_line, it.exercise.displayName, it.description())) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = actions::onSummaryDismiss) { Text(stringResource(R.string.workout_summary_ok)) } },
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = actions::onDeleteDismiss,
            title = { Text(stringResource(if (uiState.isActive) R.string.workout_discard_title else R.string.workout_delete_title)) },
            text = { Text(stringResource(R.string.workout_delete_body)) },
            confirmButton = {
                TextButton(onClick = actions::onDeleteConfirm) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = actions::onDeleteDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun RestBar(remainingSeconds: Int, onExtend: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.workout_rest, remainingSeconds / 60, remainingSeconds % 60),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onExtend) { Text(stringResource(R.string.workout_rest_extend)) }
            TextButton(onClick = onSkip) { Text(stringResource(R.string.workout_rest_skip)) }
        }
    }
}

@Composable
private fun ExerciseCard(
    entry: WorkoutExercise,
    inputs: Map<Long, SetInputs>,
    lastSets: List<WorkoutSet>,
    actions: WorkoutActions,
) {
    val exercise = entry.exercise
    val name = exercise.displayName
    AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (lastSets.isEmpty()) {
                            exercise.muscleGroup.label
                        } else {
                            stringResource(R.string.workout_last_time, lastSets.map { it.label(exercise.type) }.joinToString(", "))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { actions.onRemoveExercise(exercise.id) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.workout_remove_exercise_cd, name))
                }
            }
            Row(Modifier.padding(end = 8.dp, top = 4.dp)) {
                Text(stringResource(R.string.workout_col_set), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
                Text(stringResource(R.string.workout_col_last), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                Text(
                    text = stringResource(if (exercise.type == ExerciseType.STRENGTH) R.string.unit_kg else R.string.unit_min),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(if (exercise.type == ExerciseType.STRENGTH) R.string.workout_col_reps else R.string.unit_km),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(48.dp))
            }
            entry.sets.forEachIndexed { index, set ->
                SetRow(
                    index = index + 1,
                    set = set,
                    type = exercise.type,
                    inputs = inputs[set.id] ?: SetInputs(),
                    previous = lastSets.getOrNull(index),
                    onInputChange = { actions.onSetInputChange(set.id, it) },
                    onDone = { actions.onSetDone(set.id, it) },
                )
            }
            Row {
                TextButton(onClick = { actions.onAddSet(exercise.id) }) { Text(stringResource(R.string.workout_add_set)) }
                TextButton(onClick = { actions.onRemoveLastSet(exercise.id) }) { Text(stringResource(R.string.workout_remove_set)) }
            }
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: WorkoutSet,
    type: ExerciseType,
    inputs: SetInputs,
    previous: WorkoutSet?,
    onInputChange: (SetInputs) -> Unit,
    onDone: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$index", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(24.dp))
        Text(
            text = previous?.label(type) ?: "–",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        if (type == ExerciseType.STRENGTH) {
            NumberField(inputs.weight, { onInputChange(inputs.copy(weight = it)) }, Modifier.weight(1f))
            NumberField(inputs.reps, { onInputChange(inputs.copy(reps = it.filter(Char::isDigit).take(3))) }, Modifier.weight(1f))
        } else {
            NumberField(inputs.minutes, { onInputChange(inputs.copy(minutes = it)) }, Modifier.weight(1f))
            NumberField(inputs.km, { onInputChange(inputs.copy(km = it)) }, Modifier.weight(1f))
        }
        Checkbox(checked = set.isDone, onCheckedChange = onDone)
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun workoutStats(minutes: Long, sets: Int, volumeKg: Double, burnKcal: Int): String =
    pluralStringResource(R.plurals.workout_stats, sets, minutes, sets, volumeKg.whole(), burnKcal)

@Composable
private fun PersonalRecord.description(): String = when (this) {
    is PersonalRecord.Estimated1Rm ->
        stringResource(R.string.workout_record_one_rep_max, set.weightKg.compact(), set.reps, set.estimated1Rm.compact(), previous.compact())
    is PersonalRecord.HeaviestSet -> stringResource(R.string.workout_record_heaviest, weightKg.compact(), previous.compact())
    is PersonalRecord.LongestDistance -> stringResource(R.string.workout_record_distance, distanceKm.compact(), previousKm.compact())
}

@Composable
private fun WorkoutSet.label(type: ExerciseType): String = when (type) {
    ExerciseType.STRENGTH -> "${weightKg.compact()}×$reps"
    ExerciseType.CARDIO -> listOfNotNull(
        stringResource(R.string.workout_minutes, durationSec / 60).takeIf { durationSec > 0 },
        stringResource(R.string.workout_amount_km, (distanceM / 1000).compact()).takeIf { distanceM > 0 },
    ).joinToString(" ").ifEmpty { "–" }
}
