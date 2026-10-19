package com.macrosnap.app.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.model.Routine
import com.macrosnap.app.model.Workout
import com.macrosnap.app.ui.DatePatterns
import com.macrosnap.app.ui.RoutineEditorRoute
import com.macrosnap.app.ui.WorkoutRoute
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.components.CardHeader
import com.macrosnap.app.ui.components.SectionTitle
import com.macrosnap.app.ui.formatted
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.whole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onOpenWorkout: (WorkoutRoute) -> Unit,
    onOpenRoutine: (RoutineEditorRoute) -> Unit,
    onOpenExercises: () -> Unit,
    viewModel: WorkoutsViewModel = viewModel(factory = WorkoutsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultWorkoutName = stringResource(R.string.workout_default_name)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is WorkoutsEffect.OpenWorkout -> onOpenWorkout(WorkoutRoute(effect.workoutId))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_gym)) },
                actions = { TextButton(onClick = onOpenExercises) { Text(stringResource(R.string.workout_exercises_title)) } },
            )
        },
        floatingActionButton = {
            if (uiState.active == null) {
                ExtendedFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { viewModel.onStartEmpty(defaultWorkoutName) },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    text = { Text(stringResource(R.string.workout_start)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.active?.let { active ->
                item(key = "active") {
                    AppCard(
                        onClick = { onOpenWorkout(WorkoutRoute(active.id)) },
                        color = MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.workout_in_progress),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                )
                                Text(active.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                                Text(
                                    text = stringResource(R.string.workout_minutes, active.durationMinutes) + " · " +
                                        pluralStringResource(R.plurals.workout_sets_done, active.doneSets, active.doneSets),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                )
                            }
                            Button(
                                onClick = { onOpenWorkout(WorkoutRoute(active.id)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) { Text(stringResource(R.string.workout_continue)) }
                        }
                    }
                }
            }

            item(key = "routines-header") {
                SectionTitle(stringResource(R.string.workout_routines)) {
                    TextButton(onClick = { onOpenRoutine(RoutineEditorRoute()) }) { Text(stringResource(R.string.workout_new_routine)) }
                }
            }
            if (uiState.routines.isEmpty()) {
                item(key = "routines-empty") {
                    AppCard {
                        CardHeader(
                            title = stringResource(R.string.workout_no_routines),
                            icon = AppIcons.Dumbbell,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.workout_no_routines_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            items(uiState.routines, key = { "routine-${it.id}" }) { routine ->
                RoutineCard(
                    routine = routine,
                    canStart = uiState.active == null,
                    onStart = { viewModel.onStartRoutine(routine) },
                    onEdit = { onOpenRoutine(RoutineEditorRoute(routine.id)) },
                )
            }

            item(key = "history-header") {
                SectionTitle(stringResource(R.string.workout_history))
            }
            if (uiState.history.isEmpty()) {
                item(key = "history-empty") {
                    Text(
                        text = stringResource(R.string.workout_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(uiState.history, key = { "workout-${it.id}" }) { workout ->
                HistoryCard(workout = workout, burnKcal = uiState.burnKcal[workout.id], onClick = { onOpenWorkout(WorkoutRoute(workout.id)) })
            }
        }
    }
}

@Composable
private fun RoutineCard(routine: Routine, canStart: Boolean, onStart: () -> Unit, onEdit: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = routine.exercises.map { it.exercise.displayName }.joinToString(" · ").ifEmpty { stringResource(R.string.workout_routine_no_exercises) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            OutlinedButton(onClick = onStart, enabled = canStart && routine.exercises.isNotEmpty()) { Text(stringResource(R.string.workout_start_routine)) }
        }
    }
}

@Composable
private fun HistoryCard(workout: Workout, burnKcal: Int?, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = workout.date.formatted(DatePatterns.SHORT) + " · " +
                        workout.exercises.map { it.exercise.displayName }.joinToString(", ").ifEmpty { stringResource(R.string.workout_history_no_exercises) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.workout_minutes, workout.durationMinutes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listOfNotNull(
                        pluralStringResource(R.plurals.workout_sets, workout.doneSets, workout.doneSets),
                        stringResource(R.string.amount_kg, workout.volumeKg.whole()),
                        burnKcal?.let { stringResource(R.string.workout_kcal_approx, it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
