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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.model.Routine
import com.macrosnap.app.model.Workout
import com.macrosnap.app.ui.RoutineEditorRoute
import com.macrosnap.app.ui.WorkoutRoute
import com.macrosnap.app.ui.whole
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onOpenWorkout: (WorkoutRoute) -> Unit,
    onOpenRoutine: (RoutineEditorRoute) -> Unit,
    onOpenExercises: () -> Unit,
    viewModel: WorkoutsViewModel = viewModel(factory = WorkoutsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                title = { Text("Gym") },
                actions = { TextButton(onClick = onOpenExercises) { Text("Exercises") } },
            )
        },
        floatingActionButton = {
            if (uiState.active == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onStartEmpty,
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    text = { Text("Start workout") },
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        onClick = { onOpenWorkout(WorkoutRoute(active.id)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Workout in progress", style = MaterialTheme.typography.labelLarge)
                                Text(active.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${active.durationMinutes} min · ${active.doneSets} sets done",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Button(onClick = { onOpenWorkout(WorkoutRoute(active.id)) }) { Text("Continue") }
                        }
                    }
                }
            }

            item(key = "routines-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Routines", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onOpenRoutine(RoutineEditorRoute()) }) { Text("New routine") }
                }
            }
            if (uiState.routines.isEmpty()) {
                item(key = "routines-empty") {
                    Text(
                        text = "Save your usual sessions (Push, Pull, Legs…) as routines. Starting one pre-fills every " +
                            "set with what you lifted last time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                Text("History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            }
            if (uiState.history.isEmpty()) {
                item(key = "history-empty") {
                    Text(
                        text = "Finished workouts show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    ElevatedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = routine.exercises.joinToString(" · ") { it.exercise.name }.ifEmpty { "No exercises yet" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            OutlinedButton(onClick = onStart, enabled = canStart && routine.exercises.isNotEmpty()) { Text("Start") }
        }
    }
}

@Composable
private fun HistoryCard(workout: Workout, burnKcal: Int?, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = workout.date.format(DATE_FORMAT) + " · " +
                        workout.exercises.joinToString(", ") { it.exercise.name }.ifEmpty { "no exercises" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${workout.durationMinutes} min", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${workout.doneSets} sets · ${workout.volumeKg.whole()} kg" + (burnKcal?.let { " · ≈$it kcal" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
