package com.macrosnap.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macrosnap.app.R
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.label
import com.macrosnap.app.ui.unitLabel
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme

/** Every tracked nutrient against its daily target; collapsed to a one-line summary by default. */
@Composable
fun NutrientsCard(totals: Micros, targets: Micros, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onTrack = Nutrient.entries.count { it.isOnTrack(totals[it] ?: 0.0, targets[it] ?: 0.0) }

    AppCard(modifier) {
        CardHeader(
            title = stringResource(R.string.today_nutrients_title),
            icon = AppIcons.Leaf,
            tint = AppTheme.colors.micros,
            subtitle = stringResource(R.string.today_nutrients_subtitle, onTrack, Nutrient.entries.size),
            modifier = Modifier.clickable { expanded = !expanded },
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.today_collapse else R.string.today_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Nutrient.entries.forEach { nutrient ->
                    NutrientRow(nutrient, totals[nutrient] ?: 0.0, targets[nutrient] ?: 0.0)
                }
                Text(
                    text = stringResource(R.string.today_nutrients_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NutrientRow(nutrient: Nutrient, amount: Double, target: Double) {
    val fraction = if (target > 0) (amount / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val over = target > 0 && amount > target
    val color = when {
        nutrient.isLimit && over -> MaterialTheme.colorScheme.error
        !nutrient.isLimit && amount >= target -> AppTheme.colors.micros
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nutrient.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(
                    if (nutrient.isLimit) R.string.today_nutrient_amount_max else R.string.today_nutrient_amount,
                    amount.compact(),
                    target.compact(),
                    nutrient.unitLabel,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = if (nutrient.isLimit && over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PillBar(progress = fraction, color = color, height = 6.dp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun Nutrient.isOnTrack(amount: Double, target: Double): Boolean =
    if (isLimit) amount <= target else target <= 0 || amount >= target
