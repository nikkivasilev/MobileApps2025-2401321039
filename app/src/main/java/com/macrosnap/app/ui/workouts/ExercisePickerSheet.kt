package com.macrosnap.app.ui.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macrosnap.app.R
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.ui.components.appChipColors
import com.macrosnap.app.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onCreate: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    var newGroup by rememberSaveable { mutableStateOf(MuscleGroup.OTHER) }
    var newType by rememberSaveable { mutableStateOf(ExerciseType.STRENGTH) }
    var newSets by rememberSaveable { mutableStateOf(Exercise.DEFAULT_SETS.toString()) }
    var newReps by rememberSaveable { mutableStateOf(Exercise.DEFAULT_REPS.toString()) }
    var showCreate by rememberSaveable { mutableStateOf(false) }

    val trimmed = query.trim()
    val names = displayNames(exercises)
    val filtered = exercises.filter { exercise ->
        (group == null || exercise.muscleGroup == group) && exercise.matches(trimmed, names[exercise.id].orEmpty())
    }.sortedByDisplayName(names)
    val exactMatch = exercises.any {
        it.name.equals(trimmed, ignoreCase = true) || names[it.id].equals(trimmed, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.workout_search_exercises)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = group == null,
                    onClick = { group = null },
                    label = { Text(stringResource(R.string.workout_filter_all)) },
                    shape = CircleShape,
                    colors = appChipColors(),
                )
                MuscleGroup.entries.forEach { option ->
                    FilterChip(
                        selected = group == option,
                        onClick = { group = if (group == option) null else option },
                        label = { Text(option.label) },
                        shape = CircleShape,
                        colors = appChipColors(),
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filtered, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(names[exercise.id] ?: exercise.name) },
                        supportingContent = {
                            Text(
                                exercise.muscleGroup.label + if (exercise.isCustom) {
                                    " · " + stringResource(
                                        R.string.workout_custom,
                                    )
                                } else {
                                    ""
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(exercise) },
                    )
                }
                // After the matches, and folded until asked for, so it never hides an existing exercise.
                if (trimmed.isNotEmpty() && !exactMatch) {
                    item(key = "create") {
                        if (!showCreate) {
                            OutlinedButton(
                                onClick = { showCreate = true },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                Text(stringResource(R.string.workout_add_as_new, trimmed))
                            }
                        } else {
                            val sets = parseDefaultSets(newSets)
                            val reps = if (newType == ExerciseType.CARDIO) {
                                Exercise.DEFAULT_REPS
                            } else {
                                parseDefaultReps(
                                    newReps,
                                )
                            }
                            CreateExerciseRow(
                                name = trimmed,
                                group = newGroup,
                                onGroupChange = { newGroup = it },
                                type = newType,
                                onTypeChange = {
                                    // Follow the type's usual set count unless the user already typed their own.
                                    if (parseDefaultSets(newSets) == defaultSetsFor(newType)) {
                                        newSets = defaultSetsFor(it).toString()
                                    }
                                    newType = it
                                },
                                setsInput = newSets,
                                onSetsChange = { newSets = it },
                                repsInput = newReps,
                                onRepsChange = { newReps = it },
                                canCreate = sets != null && reps != null,
                                onCreate = {
                                    if (sets != null && reps != null) {
                                        onCreate(
                                            Exercise(
                                                id = 0,
                                                name = trimmed,
                                                muscleGroup = newGroup,
                                                type = newType,
                                                isCustom = true,
                                                defaultSets = sets,
                                                defaultReps = reps,
                                            ),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateExerciseRow(
    name: String,
    group: MuscleGroup,
    onGroupChange: (MuscleGroup) -> Unit,
    type: ExerciseType,
    onTypeChange: (ExerciseType) -> Unit,
    setsInput: String,
    onSetsChange: (String) -> Unit,
    repsInput: String,
    onRepsChange: (String) -> Unit,
    canCreate: Boolean,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.workout_new_exercise_named, name), style = MaterialTheme.typography.titleSmall)
        ExerciseDetailsFields(
            group = group,
            onGroupChange = onGroupChange,
            type = type,
            onTypeChange = onTypeChange,
            setsInput = setsInput,
            onSetsChange = onSetsChange,
            repsInput = repsInput,
            onRepsChange = onRepsChange,
        )
        Button(onClick = onCreate, enabled = canCreate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_create_and_add))
        }
    }
}
