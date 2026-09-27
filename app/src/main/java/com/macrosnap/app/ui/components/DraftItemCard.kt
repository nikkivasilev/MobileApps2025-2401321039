package com.macrosnap.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.summary
import com.macrosnap.app.ui.whole

const val GRAMS_STEP = 10.0
const val SERVINGS_STEP = 0.5

@Composable
fun DraftItemCard(
    draft: DraftItem,
    onClick: () -> Unit,
    onGramsStep: (Double) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onServingsStep: ((Double) -> Unit)? = null,
) {
    val total = draft.item.total
    val servings = draft.servings
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = draft.item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (draft.confidence == Confidence.LOW) {
                        Text(
                            text = "Low confidence - worth a check",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${draft.item.name}")
                }
            }
            if (servings != null && onServingsStep != null) {
                Row(
                    modifier = Modifier.padding(end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = { onServingsStep(-SERVINGS_STEP) }, enabled = servings > SERVINGS_STEP) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = "${servings.compact()} ${if (servings == 1.0) "serving" else "servings"}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledTonalIconButton(onClick = { onServingsStep(SERVINGS_STEP) }) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "½ serving steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = { onGramsStep(-GRAMS_STEP) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "${draft.item.grams.compact()} g",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { onGramsStep(GRAMS_STEP) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${total.kcal.whole()} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = total.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
