package com.macrosnap.app.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import com.macrosnap.app.ui.components.appChipColors

/** Text inputs for an exercise's defaults; null from the parsers means the input isn't usable yet. */
fun parseDefaultSets(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it in Exercise.SETS_RANGE }

fun parseDefaultReps(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it in Exercise.REPS_RANGE }

fun defaultSetsFor(type: ExerciseType): Int = if (type == ExerciseType.CARDIO) 1 else Exercise.DEFAULT_SETS

/**
 * Muscle group, logging type and default sets/reps: everything about an exercise except its name,
 * which callers lay out themselves (the picker takes it from the search box).
 */
@Composable
fun ExerciseDetailsFields(
    group: MuscleGroup,
    onGroupChange: (MuscleGroup) -> Unit,
    type: ExerciseType,
    onTypeChange: (ExerciseType) -> Unit,
    setsInput: String,
    onSetsChange: (String) -> Unit,
    repsInput: String,
    onRepsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Wrapped rather than scrolled, so the selected group is always in view.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuscleGroup.entries.forEach { option ->
                FilterChip(colors = appChipColors(), shape = CircleShape, selected = group == option, onClick = { onGroupChange(option) }, label = { Text(option.label) })
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = setsInput,
                onValueChange = { onSetsChange(it.filter(Char::isDigit).take(2)) },
                label = { Text("Default sets") },
                singleLine = true,
                isError = parseDefaultSets(setsInput) == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            if (type == ExerciseType.STRENGTH) {
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { onRepsChange(it.filter(Char::isDigit).take(3)) },
                    label = { Text("Default reps") },
                    singleLine = true,
                    isError = parseDefaultReps(repsInput) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = "Used when you add this exercise to a routine, or to a workout before you've logged it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
