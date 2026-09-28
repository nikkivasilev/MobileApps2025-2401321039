package com.macrosnap.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme

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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(330, 500, 750).forEach { amount ->
                            FilterChip(
                                colors = appChipColors(),
                                shape = CircleShape,
                                selected = customInput == amount.toString(),
                                onClick = { customInput = amount.toString() },
                                label = { Text("$amount ml") },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it.filter(Char::isDigit).take(4) },
                        label = { Text("Millilitres") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
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


    val tint = AppTheme.colors.water
    AppCard(modifier, onClick = { showCustom = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(AppIcons.WaterDrop, tint, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text("Water", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text((totalMl / 1000.0).compact(), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = " / ${(goalMl / 1000.0).compact()} L",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        PillBar(
            progress = if (goalMl > 0) totalMl.toFloat() / goalMl else 0f,
            color = tint,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp),
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }
            Button(
                onClick = { onAdd(250) },
                colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = AppTheme.colors.onData),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
            ) { Text("+250 ml", maxLines = 1) }
        }
    }
}
