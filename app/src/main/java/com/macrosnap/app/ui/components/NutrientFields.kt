package com.macrosnap.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.label
import com.macrosnap.app.ui.toAmountOrNull
import com.macrosnap.app.ui.unitLabel

/** One text field per tracked nutrient, two to a row. Values are whatever unit [Nutrient.unit] says. */
@Composable
fun NutrientFields(
    values: Map<Nutrient, String>,
    onValueChange: (Nutrient, String) -> Unit,
    modifier: Modifier = Modifier,
    placeholders: Map<Nutrient, String> = emptyMap(),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Nutrient.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { nutrient ->
                    OutlinedTextField(
                        value = values[nutrient].orEmpty(),
                        onValueChange = { onValueChange(nutrient, it) },
                        label = { Text("${nutrient.label} (${nutrient.unitLabel})") },
                        placeholder = placeholders[nutrient]?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

fun Micros.toInputs(): Map<Nutrient, String> = mapValues { it.value.compact() }

/** Blank and unparsable fields are left out, which the model reads as "unknown". */
fun Map<Nutrient, String>.toMicros(): Micros =
    mapNotNull { (nutrient, text) -> text.toAmountOrNull()?.let { nutrient to it } }.toMap()
