package com.macrosnap.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrosnap.app.ui.compact

@Composable
fun WaterCard(
    totalMl: Int,
    goalMl: Int,
    canUndo: Boolean,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var customInput by rememberSaveable { mutableStateOf("") }

    if (showCustom) {
        val custom = customInput.trim().toIntOrNull()?.takeIf { it in 1..5000 }
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("Add water") },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter(Char::isDigit).take(4) },
                    label = { Text("Millilitres") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        custom?.let(onAdd)
                        showCustom = false
                        customInput = ""
                    },
                    enabled = custom != null,
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Cancel") } },
        )
    }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Water", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${(totalMl / 1000.0).compact()} / ${(goalMl / 1000.0).compact()} L",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (totalMl >= goalMl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f },
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = { onAdd(250) }) { Text("+ 250 ml") }
                FilledTonalButton(onClick = { onAdd(500) }) { Text("+ 500 ml") }
                FilledTonalButton(onClick = { showCustom = true }) { Text("+ other") }
                TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            }
        }
    }
}
