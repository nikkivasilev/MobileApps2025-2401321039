package com.macrosnap.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.macrosnap.app.model.EntrySource
import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.theme.MacroColors
import com.macrosnap.app.ui.whole
import java.io.File

/** Read-only breakdown of one logged entry; editing is a deliberate extra tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailSheet(
    entry: FoodEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogAgain: () -> Unit,
    /** The entry is from an earlier day, so logging it again lands on today, not beside it. */
    logsToToday: Boolean = false,
    onDismiss: () -> Unit,
) {
    val item = entry.item
    val total = item.total
    val micros = item.totalMicros
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("${item.name} (${total.kcal.whole()} kcal) will be removed from the log.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            entry.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${item.grams.compact()} g · ${entry.mealType.label} · ${entry.source.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("${total.kcal.whole()} kcal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroTile("Protein", total.protein, MacroColors.Protein, Modifier.weight(1f))
                MacroTile("Carbs", total.carbs, MacroColors.Carbs, Modifier.weight(1f))
                MacroTile("Fat", total.fat, MacroColors.Fat, Modifier.weight(1f))
            }
            Text(
                text = "Per 100 g: ${item.per100g.kcal.whole()} kcal · P ${item.per100g.protein.compact()} · " +
                    "C ${item.per100g.carbs.compact()} · F ${item.per100g.fat.compact()} g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text("Vitamins & minerals", style = MaterialTheme.typography.titleMedium)
            if (micros.isEmpty()) {
                Text(
                    text = "No estimate for this entry. Entries logged before this feature, and manual " +
                        "entries without values, don't have one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Nutrient.entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) {
                        pair.forEach { nutrient ->
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(nutrient.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${(micros[nutrient] ?: 0.0).compact()} ${nutrient.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 16.dp),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                Button(onClick = onLogAgain, modifier = Modifier.weight(1f)) { Text(if (logsToToday) "Log today" else "Log again") }
            }
        }
    }
}

@Composable
private fun MacroTile(label: String, grams: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${grams.whole()} g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val EntrySource.label: String
    get() = when (this) {
        EntrySource.PHOTO -> "from photo"
        EntrySource.BARCODE -> "from barcode"
        EntrySource.MANUAL -> "entered manually"
        EntrySource.TEXT -> "from description"
        EntrySource.RECIPE -> "from recipe"
        EntrySource.HISTORY -> "from your foods"
    }
