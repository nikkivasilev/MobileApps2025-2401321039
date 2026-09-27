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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.WeightEntry
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.components.formatSleepDuration
import com.macrosnap.app.ui.whole
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StrengthCard(uiState, onExerciseSelect = viewModel::onExerciseSelect)
            WeekCard(uiState)
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
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Strength", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Workouts this week: ${uiState.workoutDays.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.trainedExercises.isEmpty()) {
                Text(
                    text = "Finish a workout and each exercise gets a chart here: estimated one-rep max per session, best set and volume.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            var expanded by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = uiState.selectedExercise?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exercise") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    uiState.trainedExercises.forEach { exercise ->
                        DropdownMenuItem(
                            text = { Text(exercise.name) },
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
                    text = "Estimated 1RM per session (${sessions.size} sessions)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.bestRecord?.let { best ->
                    Text(
                        text = "Best: ${best.weightKg.compact()} kg × ${best.reps} (est. 1RM ${best.estimated1Rm.compact()} kg) on ${best.date.format(DATE_FORMAT)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                sessions.lastOrNull()?.let { last ->
                    val first = sessions.first()
                    Text(
                        text = "Last session: top set ${last.topWeight.compact()} kg, volume ${last.volume.whole()} kg" +
                            if (sessions.size > 1 && first.best1Rm > 0) " · 1RM ${((last.best1Rm / first.best1Rm - 1) * 100).whole()}% since first session" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LineChart(values = sessions.map { it.distanceM / 1000 }, lineColor = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Distance per session (km), ${sessions.size} sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                sessions.lastOrNull()?.let { last ->
                    Text(
                        text = "Last: ${(last.distanceM / 1000).compact()} km in ${last.durationSec / 60} min",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Double>, lineColor: Color) {
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
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        values.indices.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = pointAt(it)) }
    }
}

@Composable
private fun SleepWeekCard(uiState: ProgressUiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sleep", style = MaterialTheme.typography.titleMedium)
            val avg = uiState.averageSleepHours
            Text(
                text = if (avg != null) {
                    "Average ${formatSleepDuration((avg * 60).toLong())} · quality ${uiState.averageSleepQuality?.compact()}/5 · ${uiState.sleepWeek.size} nights logged"
                } else {
                    "Log a few nights from the Today screen to see your week."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val today = LocalDate.now()
            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val byDay = uiState.sleepWeek.associateBy { it.date }
            WeekBars(
                days = days,
                values = days.map { byDay[it]?.hours ?: 0.0 },
                goal = uiState.sleepGoalHours,
                overIsBad = false,
            )
            Text(
                text = "Dashed line: your ${uiState.sleepGoalHours.compact()} h goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WaterWeekCard(uiState: ProgressUiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Water", style = MaterialTheme.typography.titleMedium)
            val avg = uiState.averageWaterMl
            Text(
                text = if (avg != null) "Average ${(avg / 1000).compact()} L on logged days" else "Tap the water buttons on Today to fill this in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val days = uiState.waterWeek.map { it.date }.ifEmpty { (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) } }
            WeekBars(
                days = days,
                values = uiState.waterWeek.map { it.totalMl / 1000.0 }.ifEmpty { days.map { 0.0 } },
                goal = uiState.waterGoalMl / 1000.0,
                overIsBad = false,
            )
        }
    }
}

@Composable
private fun WeekCard(uiState: ProgressUiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Calories, last 7 days", style = MaterialTheme.typography.titleMedium)
            val averageKcal = uiState.averageKcal
            val averageProtein = uiState.averageProtein
            Text(
                text = if (averageKcal != null && averageProtein != null) {
                    "Average on logged days: ${averageKcal.whole()} kcal · ${averageProtein.whole()} g protein"
                } else {
                    "Log a few meals to see your weekly average."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WeekBars(
                days = uiState.week.map { it.date },
                values = uiState.week.map { it.total.kcal },
                goal = uiState.goals.kcal.toDouble(),
                overIsBad = true,
                workoutDays = uiState.workoutDays,
            )
            Text(
                text = "Dashed line: your ${uiState.goals.kcal} kcal goal · dot: gym day",
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
    workoutDays: Set<LocalDate> = emptySet(),
) {
    val barColor = MaterialTheme.colorScheme.primary
    val lowColor = MaterialTheme.colorScheme.tertiary
    val overColor = MaterialTheme.colorScheme.error
    val goalColor = MaterialTheme.colorScheme.outline
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            if (days.isEmpty()) return@Canvas
            val maxValue = maxOf(values.maxOrNull() ?: 0.0, goal * 1.15, 0.001)
            val slot = size.width / days.size
            val barWidth = slot * 0.55f
            values.forEachIndexed { index, value ->
                val barHeight = (value / maxValue).toFloat() * size.height
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
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
                if (days[index] in workoutDays) {
                    drawCircle(goalColor, radius = 3.dp.toPx(), center = Offset(slot * index + slot / 2, size.height - 4.dp.toPx()))
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
                    text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
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

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Body weight", style = MaterialTheme.typography.titleMedium)

            val latest = uiState.latestWeight
            if (latest != null) {
                val change = uiState.weightChange
                Text(
                    text = buildString {
                        append("${latest.weightKg.compact()} kg")
                        if (change != null) append("  (${if (change > 0) "+" else ""}${change.compact()} kg since you started)")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.weightInput,
                    onValueChange = onInputChange,
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onLog, enabled = uiState.parsedWeight != null) { Text("Log") }
            }
            TextButton(onClick = { showDatePicker = true }, contentPadding = PaddingValues(0.dp)) {
                Text(uiState.weightDate?.let { "For ${it.format(DATE_FORMAT)}" } ?: "For today · change date")
            }

            if (uiState.weights.size >= 2) WeightChart(uiState.weights)

            uiState.weights.takeLast(RECENT_WEIGHTS).asReversed().forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.date.format(DATE_FORMAT), modifier = Modifier.weight(1f))
                    Text("${entry.weightKg.compact()} kg", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { onDelete(entry.date) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete weight from ${entry.date.format(DATE_FORMAT)}")
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
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>) {
    val lineColor = MaterialTheme.colorScheme.primary
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
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        weights.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = pointFor(it)) }
    }
}

private const val RECENT_WEIGHTS = 7
private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM")
