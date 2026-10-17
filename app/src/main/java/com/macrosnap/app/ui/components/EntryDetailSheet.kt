package com.macrosnap.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.macrosnap.app.R
import com.macrosnap.app.model.EntrySource
import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.label
import com.macrosnap.app.ui.theme.MacroColors
import com.macrosnap.app.ui.unitLabel
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
            title = { Text(stringResource(R.string.review_delete_entry_title)) },
            text = { Text(stringResource(R.string.review_delete_entry_named, item.name, total.kcal.whole())) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
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
                        .clip(MaterialTheme.shapes.large),
                )
            }
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${stringResource(R.string.amount_g, item.grams.compact())} · ${entry.mealType.label} · ${stringResource(entry.source.labelRes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.amount_kcal, total.kcal.whole()), style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroAmount(stringResource(R.string.protein), total.protein, MacroColors.Protein, Modifier.weight(1f))
                MacroAmount(stringResource(R.string.carbs), total.carbs, MacroColors.Carbs, Modifier.weight(1f))
                MacroAmount(stringResource(R.string.fat), total.fat, MacroColors.Fat, Modifier.weight(1f))
            }
            Text(
                text = stringResource(
                    R.string.review_per_100g,
                    item.per100g.kcal.whole(),
                    stringResource(R.string.macros_summary, item.per100g.protein.compact(), item.per100g.carbs.compact(), item.per100g.fat.compact()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text(stringResource(R.string.review_micros_title), style = MaterialTheme.typography.titleMedium)
            if (micros.isEmpty()) {
                Text(
                    text = stringResource(R.string.review_no_micros),
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
                                    text = "${(micros[nutrient] ?: 0.0).compact()} ${nutrient.unitLabel}",
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
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_edit)) }
                Button(onClick = onLogAgain, modifier = Modifier.weight(1f)) { Text(stringResource(if (logsToToday) R.string.review_log_today else R.string.review_log_again)) }
            }
        }
    }
}

@Composable
private fun MacroAmount(label: String, grams: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.amount_g, grams.whole()), style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val EntrySource.labelRes: Int
    @StringRes get() = when (this) {
        EntrySource.PHOTO -> R.string.review_source_photo
        EntrySource.BARCODE -> R.string.review_source_barcode
        EntrySource.MANUAL -> R.string.review_source_manual
        EntrySource.TEXT -> R.string.review_source_text
        EntrySource.RECIPE -> R.string.review_source_recipe
        EntrySource.HISTORY -> R.string.review_source_history
    }
