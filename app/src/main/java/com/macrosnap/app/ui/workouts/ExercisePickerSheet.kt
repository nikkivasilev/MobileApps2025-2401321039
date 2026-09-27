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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onCreate: (name: String, group: MuscleGroup, type: ExerciseType) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    var newGroup by rememberSaveable { mutableStateOf(MuscleGroup.OTHER) }
    var newType by rememberSaveable { mutableStateOf(ExerciseType.STRENGTH) }

    val trimmed = query.trim()
    val filtered = exercises.filter { exercise ->
        (group == null || exercise.muscleGroup == group) &&
            (trimmed.isEmpty() || exercise.name.contains(trimmed, ignoreCase = true))
    }
    val exactMatch = exercises.any { it.name.equals(trimmed, ignoreCase = true) }

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
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = group == null, onClick = { group = null }, label = { Text("All") })
                MuscleGroup.entries.forEach { option ->
                    FilterChip(selected = group == option, onClick = { group = if (group == option) null else option }, label = { Text(option.label) })
                }
            }
            if (trimmed.isNotEmpty() && !exactMatch) {
                CreateExerciseRow(
                    name = trimmed,
                    group = newGroup,
                    onGroupChange = { newGroup = it },
                    type = newType,
                    onTypeChange = { newType = it },
                    onCreate = { onCreate(trimmed, newGroup, newType) },
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filtered, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(exercise.muscleGroup.label + if (exercise.isCustom) " · custom" else "") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(exercise) },
                    )
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
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Not in the list? Add \"$name\"", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MuscleGroup.entries.forEach { option ->
                FilterChip(selected = group == option, onClick = { onGroupChange(option) }, label = { Text(option.label) })
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ExerciseType.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = type == option,
                    onClick = { onTypeChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, ExerciseType.entries.size),
                ) { Text(if (option == ExerciseType.STRENGTH) "Weight × reps" else "Time / distance") }
            }
        }
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Create and add") }
    }
}
