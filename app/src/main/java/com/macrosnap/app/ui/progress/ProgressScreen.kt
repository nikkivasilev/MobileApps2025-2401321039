package com.macrosnap.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.WeightEntry
import com.macrosnap.app.ui.DatePatterns
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.components.CardHeader
import com.macrosnap.app.ui.components.formatSleepDuration
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.currentLocale
import com.macrosnap.app.ui.formatted
import com.macrosnap.app.ui.theme.AppTheme
import com.macrosnap.app.ui.whole
import com.macrosnap.app.ui.workouts.displayName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_progress)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WeekCard(uiState)
            StrengthCard(uiState, onExerciseSelect = viewModel::onExerciseSelect)
            SleepWeekCard(uiState)
            WaterWeekCard(uiState)
            WeightCard(
                uiState = uiState,
                onInputChange = viewModel::onWeightInputChange,
                onDateChange = viewModel::onWeightDateChange,
                onLog = viewModel::onLogWeight,
                onDelete = viewModel::onDeleteWeight,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StrengthCard(uiState: ProgressUiState, onExerciseSelect: (Exercise) -> Unit) {
    AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(
                title = stringResource(R.string.progress_strength_title),
                icon = AppIcons.Dumbbell,
                tint = MaterialTheme.colorScheme.onSurface,
                subtitle = pluralStringResource(R.plurals.progress_workouts_this_week, uiState.workoutDays.size, uiState.workoutDays.size),
            )
            if (uiState.trainedExercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.progress_strength_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            var expanded by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = uiState.selectedExercise?.displayName.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.progress_exercise)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    uiState.trainedExercises.forEach { exercise ->
                        DropdownMenuItem(
                            text = { Text(exercise.displayName) },
                            onClick = {
                                onExerciseSelect(exercise)
                                expanded = false
                            },
                        )
                    }
                }
            }

            val exercise = uiState.selectedExercise ?: return@Column
            val sessions = uiState.sessions
            if (exercise.type == ExerciseType.STRENGTH) {
                val values = sessions.map { it.best1Rm }
                LineChart(values = values, lineColor = MaterialTheme.colorScheme.primary)
                Text(
                    text = pluralStringResource(R.plurals.progress_1rm_caption, sessions.size, sessions.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.bestRecord?.let { best ->
                    Text(
                        text = stringResource(
                            R.string.progress_best_set,
                            best.weightKg.compact(),
                            best.reps,
                            best.estimated1Rm.compact(),
                            best.date.formatted(DatePatterns.SHORT),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                sessions.lastOrNull()?.let { last ->
                    val first = sessions.first()
                    Text(
                        text = if (sessions.size > 1 && first.best1Rm > 0) {
                            stringResource(
                                R.string.progress_last_session_with_change,
                                last.topWeight.compact(),
                                last.volume.whole(),
                                ((last.best1Rm / first.best1Rm - 1) * 100).whole(),
                            )
                        } else {
                            stringResource(R.string.progress_last_session, last.topWeight.compact(), last.volume.whole())
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LineChart(values = sessions.map { it.distanceM / 1000 }, lineColor = MaterialTheme.colorScheme.primary)
                Text(
                    text = pluralStringResource(R.plurals.progress_distance_caption, sessions.size, sessions.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                sessions.lastOrNull()?.let { last ->
                    Text(
                        text = stringResource(R.string.progress_last_cardio, (last.distanceM / 1000).compact(), last.durationSec / 60),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Double>, lineColor: Color) {
    val cardColor = AppTheme.colors.card
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        if (values.isEmpty()) return@Canvas
        val min = values.min()
        val span = (values.max() - min).coerceAtLeast(1.0)
        val inset = 6.dp.toPx()
        fun pointAt(index: Int) = Offset(
            x = if (values.size == 1) size.width / 2 else inset + index.toFloat() / (values.size - 1) * (size.width - 2 * inset),
            y = size.height - inset - ((values[index] - min) / span).toFloat() * (size.height - 2 * inset),
        )
        val path = Path()
        values.indices.forEach { i -> val p = pointAt(i); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawAreaUnder(path, pointAt(0).x, pointAt(values.lastIndex).x, lineColor)
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        values.indices.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = pointAt(it))
        values.indices.forEach { drawCircle(cardColor, radius = 2.dp.toPx(), center = pointAt(it)) } }
    }
}

@Composable
private fun SleepWeekCard(uiState: ProgressUiState) {
    AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(
                title = stringResource(R.string.progress_sleep_title),
                icon = AppIcons.Moon,
                tint = AppTheme.colors.sleep,
                subtitle = stringResource(R.string.progress_last_7_nights),
            )
            val avg = uiState.averageSleepHours
            if (avg != null) {
                HeadlineStat(
                    value = formatSleepDuration((avg * 60).toLong()),
                    caption = pluralStringResource(
                        R.plurals.progress_sleep_caption,
                        uiState.sleepWeek.size,
                        uiState.sleepWeek.size,
                        uiState.averageSleepQuality?.compact().orEmpty(),
                    ),
                )
            } else {
                EmptyHint(stringResource(R.string.progress_sleep_empty))
            }
            val today = LocalDate.now()
            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val byDay = uiState.sleepWeek.associateBy { it.date }
            WeekBars(
                days = days,
                values = days.map { byDay[it]?.hours ?: 0.0 },
                goal = uiState.sleepGoalHours,
                overIsBad = false,
                barColor = AppTheme.colors.sleep,
            )
            Text(
                text = stringResource(R.string.progress_sleep_goal_line, uiState.sleepGoalHours.compact()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WaterWeekCard(uiState: ProgressUiState) {
    AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(
                title = stringResource(R.string.progress_water_title),
                icon = AppIcons.WaterDrop,
                tint = AppTheme.colors.water,
                subtitle = stringResource(R.string.progress_last_7_days),
            )
            val avg = uiState.averageWaterMl
            if (avg != null) {
                HeadlineStat(
                    value = stringResource(R.string.progress_amount_l, (avg / 1000).compact()),
                    caption = stringResource(R.string.progress_water_caption),
                )
            } else {
                EmptyHint(stringResource(R.string.progress_water_empty))
            }
            val days = uiState.waterWeek.map { it.date }.ifEmpty { (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) } }
            WeekBars(
                days = days,
                values = uiState.waterWeek.map { it.totalMl / 1000.0 }.ifEmpty { days.map { 0.0 } },
                goal = uiState.waterGoalMl / 1000.0,
                overIsBad = false,
                barColor = AppTheme.colors.water,
            )
        }
    }
}

@Composable
private fun WeekCard(uiState: ProgressUiState) {
    AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(
                title = stringResource(R.string.calories),
                icon = AppIcons.Flame,
                tint = AppTheme.colors.calories,
                subtitle = stringResource(R.string.progress_last_7_days),
            )
            val averageKcal = uiState.averageKcal
            val averageProtein = uiState.averageProtein
            if (averageKcal != null && averageProtein != null) {
                HeadlineStat(
                    value = stringResource(R.string.amount_kcal, averageKcal.whole()),
                    caption = stringResource(R.string.progress_calories_caption, averageProtein.whole()),
                )
            } else {
                EmptyHint(stringResource(R.string.progress_calories_empty))
            }
            WeekBars(
                days = uiState.week.map { it.date },
                values = uiState.week.map { it.total.kcal },
                goal = uiState.goals.kcal.toDouble(),
                overIsBad = true,
                barColor = AppTheme.colors.calories,
                workoutDays = uiState.workoutDays,
            )
            Text(
                text = stringResource(R.string.progress_calories_goal_line, uiState.goals.kcal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekBars(
    days: List<LocalDate>,
    values: List<Double>,
    goal: Double,
    overIsBad: Boolean,
    barColor: Color,
    workoutDays: Set<LocalDate> = emptySet(),
) {
    // Short of a goal you want to reach: the same hue, faded, so the week still reads as one series.
    val lowColor = barColor.copy(alpha = 0.4f)
    val overColor = MaterialTheme.colorScheme.error
    val goalColor = MaterialTheme.colorScheme.outline
    val trackColor = AppTheme.colors.track
    val locale = currentLocale()
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            if (days.isEmpty()) return@Canvas
            val maxValue = maxOf(values.maxOrNull() ?: 0.0, goal * 1.15, 0.001)
            val slot = size.width / days.size
            val barWidth = slot * 0.42f
            val radius = CornerRadius(barWidth / 2)
            days.indices.forEach { index ->
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(slot * index + (slot - barWidth) / 2, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius,
                )
            }
            values.forEachIndexed { index, value ->
                // Never shorter than it is wide, so a small value still reads as a rounded bar.
                val barHeight = ((value / maxValue).toFloat() * size.height).let { if (it > 0f) it.coerceAtLeast(barWidth) else 0f }
                if (barHeight > 0f) {
                    val color = when {
                        overIsBad && value > goal * 1.05 -> overColor
                        !overIsBad && value < goal -> lowColor
                        else -> barColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(slot * index + (slot - barWidth) / 2, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                    )
                }
                if (days[index] in workoutDays) {
                    drawCircle(goalColor, radius = 3.dp.toPx(), center = Offset(slot * index + slot / 2, 4.dp.toPx()))
                }
            }
            val goalY = size.height - (goal / maxValue).toFloat() * size.height
            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeightCard(
    uiState: ProgressUiState,
    onInputChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onLog: () -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    AppCard(Modifier, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(title = stringResource(R.string.progress_weight_title), icon = AppIcons.Scale, tint = MaterialTheme.colorScheme.onSurface)

            val latest = uiState.latestWeight
            if (latest != null) {
                val change = uiState.weightChange
                HeadlineStat(
                    value = stringResource(R.string.amount_kg, latest.weightKg.compact()),
                    caption = if (change != null) {
                        stringResource(R.string.progress_weight_change, "${if (change > 0) "+" else ""}${change.compact()}")
                    } else {
                        stringResource(R.string.progress_weight_latest)
                    },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.weightInput,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.progress_weight_input)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onLog, enabled = uiState.parsedWeight != null) { Text(stringResource(R.string.progress_log_weight)) }
            }
            TextButton(onClick = { showDatePicker = true }, contentPadding = PaddingValues(0.dp)) {
                Text(
                    uiState.weightDate?.let { stringResource(R.string.progress_weight_for_date, it.formatted(DatePatterns.SHORT)) }
                        ?: stringResource(R.string.progress_weight_for_today),
                )
            }

            if (uiState.weights.size >= 2) WeightChart(uiState.weights)

            uiState.weights.takeLast(RECENT_WEIGHTS).asReversed().forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val date = entry.date.formatted(DatePatterns.SHORT)
                    Text(date, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.amount_kg, entry.weightKg.compact()), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { onDelete(entry.date) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.progress_delete_weight, date))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        WeightDatePicker(
            initial = uiState.weightDate ?: LocalDate.now(),
            onPick = {
                onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightDatePicker(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The picker works in UTC midnights; converting through UTC keeps the chosen calendar day intact.
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        ?: onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val cardColor = AppTheme.colors.card
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val firstDay = weights.first().date.toEpochDay()
        val daySpan = (weights.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
        val minKg = weights.minOf { it.weightKg }
        // A floor on the range keeps normal daily fluctuation from looking like a cliff.
        val kgSpan = (weights.maxOf { it.weightKg } - minKg).coerceAtLeast(1.0)
        val inset = 6.dp.toPx()

        fun pointFor(entry: WeightEntry) = Offset(
            x = inset + (entry.date.toEpochDay() - firstDay).toFloat() / daySpan * (size.width - 2 * inset),
            y = size.height - inset - ((entry.weightKg - minKg) / kgSpan).toFloat() * (size.height - 2 * inset),
        )

        val path = Path()
        weights.forEachIndexed { index, entry ->
            val point = pointFor(entry)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawAreaUnder(path, pointFor(weights.first()).x, pointFor(weights.last()).x, lineColor)
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        weights.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = pointFor(it)) }
        weights.forEach { drawCircle(cardColor, radius = 2.dp.toPx(), center = pointFor(it)) }
    }
}

private const val RECENT_WEIGHTS = 7

/** A big figure with a line of context under it, the lead of each Progress card. */
@Composable
private fun HeadlineStat(value: String, caption: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Fills from the line down to the chart's floor with the line colour, fading out downwards. */
private fun DrawScope.drawAreaUnder(line: Path, startX: Float, endX: Float, color: Color) {
    val area = Path().apply {
        addPath(line)
        lineTo(endX, size.height)
        lineTo(startX, size.height)
        close()
    }
    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0f))))
}
