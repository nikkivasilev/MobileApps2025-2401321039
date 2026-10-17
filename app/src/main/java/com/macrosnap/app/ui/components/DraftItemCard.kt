package com.macrosnap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macrosnap.app.R
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.theme.AppTheme
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
    AppCard(modifier, onClick = onClick, contentPadding = PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = draft.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = stringResource(R.string.amount_kcal, total.kcal.whole()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(10.dp))
                    MacroDots(total)
                }
                if (draft.confidence == Confidence.LOW) {
                    Tag(stringResource(R.string.review_low_confidence), MaterialTheme.colorScheme.error, Modifier.padding(top = 6.dp))
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.review_remove_item, draft.item.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (servings != null && onServingsStep != null) {
                AmountStepper(
                    // Fractions such as 1.5 take the plural, so only exactly one counts as "one".
                    text = pluralStringResource(R.plurals.review_servings, if (servings == 1.0) 1 else 2, servings.compact()),
                    onMinus = { onServingsStep(-SERVINGS_STEP) },
                    onPlus = { onServingsStep(SERVINGS_STEP) },
                    minusEnabled = servings > SERVINGS_STEP,
                    modifier = Modifier.weight(1f),
                )
            }
            AmountStepper(
                text = stringResource(R.string.amount_g, draft.item.grams.compact()),
                onMinus = { onGramsStep(-GRAMS_STEP) },
                onPlus = { onGramsStep(GRAMS_STEP) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "−  150 g  +" on a single rounded tile. */
@Composable
fun AmountStepper(
    text: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.background(AppTheme.colors.tile, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinus, enabled = minusEnabled) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onPlus) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
