package com.macrosnap.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme
import com.macrosnap.app.ui.theme.HeroNumberStyle
import com.macrosnap.app.ui.whole
import kotlin.math.abs

private val ProgressSpring = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)

/** A ring filled clockwise from the top; [progress] is clamped to 0..1 and animates when it changes. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = AppTheme.colors.track,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), ProgressSpring, label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val style = Stroke(width = stroke, cap = StrokeCap.Round)
            drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = style)
            if (animated > 0.001f) {
                drawArc(color, -90f, 360f * animated, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = style)
            }
        }
        content()
    }
}

/** A rounded progress bar; animates like [ProgressRing]. */
@Composable
fun PillBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = AppTheme.colors.track,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), ProgressSpring, label = "bar")
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = radius)
        if (animated > 0.001f) {
            // Never narrower than a dot, so a sliver of progress still reads as a rounded pill.
            drawRoundRect(color, size = Size((size.width * animated).coerceAtLeast(size.height), size.height), cornerRadius = radius)
        }
    }
}

/** The Today hero: calories left as a big number, with what was eaten and burned beside the ring. */
@Composable
fun CalorieHero(consumedKcal: Double, goalKcal: Int, burnedKcal: Int, burnAddedToGoal: Boolean, modifier: Modifier = Modifier) {
    val remaining = goalKcal - consumedKcal
    val isOver = remaining < 0
    val ringColor = if (isOver) MaterialTheme.colorScheme.error else AppTheme.colors.calories

    AppCard(modifier, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = abs(remaining).whole(),
                    style = HeroNumberStyle,
                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isOver) "Calories over" else "Calories left",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressRing(
                progress = if (goalKcal > 0) (consumedKcal / goalKcal).toFloat() else 0f,
                color = ringColor,
                strokeWidth = 12.dp,
                modifier = Modifier.size(104.dp),
            ) {
                Icon(AppIcons.Flame, contentDescription = null, tint = ringColor, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroStat("Eaten", consumedKcal.whole(), Modifier.weight(1f))
            HeroStat(if (burnAddedToGoal && burnedKcal > 0) "Goal + gym" else "Goal", goalKcal.toString(), Modifier.weight(1f))
            HeroStat("Burned", if (burnedKcal > 0) "≈$burnedKcal" else "0", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(AppTheme.colors.tile, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** One macro against its goal: amount, goal, and a bar in the macro's colour. */
@Composable
fun MacroTile(label: String, color: Color, consumed: Double, goal: Int, modifier: Modifier = Modifier) {
    AppCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(consumed.whole(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = " / ${goal}g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        PillBar(progress = if (goal > 0) (consumed / goal).toFloat() else 0f, color = color, height = 6.dp)
    }
}
