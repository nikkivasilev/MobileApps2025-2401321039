package com.macrosnap.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.ui.compact

/** Every tracked nutrient against its daily target; collapsed to a one-line summary by default. */
@Composable
fun NutrientsCard(totals: Micros, targets: Micros, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onTrack = Nutrient.entries.count { it.isOnTrack(totals[it] ?: 0.0, targets[it] ?: 0.0) }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Vitamins & minerals", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$onTrack of ${Nutrient.entries.size} on track · estimates from food + supplements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Nutrient.entries.forEach { nutrient ->
                        NutrientRow(nutrient, totals[nutrient] ?: 0.0, targets[nutrient] ?: 0.0)
                    }
                    Text(
                        text = "Caffeine is a daily limit; the others are targets. Photo-based values are rough " +
                            "(think ±50%), so use these to spot what you're consistently missing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        !nutrient.isLimit && amount >= target -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nutrient.label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${amount.compact()} / ${target.compact()} ${nutrient.unit}" + if (nutrient.isLimit) " max" else "",
                style = MaterialTheme.typography.labelLarge,
                color = if (nutrient.isLimit && over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

private fun Nutrient.isOnTrack(amount: Double, target: Double): Boolean =
    if (isLimit) amount <= target else target <= 0 || amount >= target
