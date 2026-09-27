package com.macrosnap.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macrosnap.app.ui.whole
import kotlin.math.abs

@Composable
fun CalorieRing(consumedKcal: Double, goalKcal: Int, modifier: Modifier = Modifier) {
    val remaining = goalKcal - consumedKcal
    val isOver = remaining < 0
    val progress = if (goalKcal > 0) (consumedKcal / goalKcal).coerceIn(0.0, 1.0).toFloat() else 0f
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            if (progress > 0f) {
                drawArc(progressColor, -90f, 360f * progress, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = abs(remaining).whole(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isOver) "kcal over" else "kcal left",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MacroBar(label: String, color: Color, consumed: Double, goal: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${consumed.whole()} / $goal g",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { if (goal > 0) (consumed / goal).coerceIn(0.0, 1.0).toFloat() else 0f },
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
