package com.macrosnap.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.ui.theme.AppIcons
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatSleepDuration(minutes: Long): String = "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')} min"

@Composable
fun SleepCard(sleep: SleepLog?, goalHours: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sleep", style = MaterialTheme.typography.titleMedium)
                if (sleep == null) {
                    Text(
                        text = "Tap to log last night",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "${sleep.bedtime.toLocalTime().format(TIME_FORMAT)} → ${sleep.wakeTime.toLocalTime().format(TIME_FORMAT)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (sleep != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatSleepDuration(sleep.minutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (sleep.hours >= goalHours) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < sleep.quality) Icons.Filled.Star else AppIcons.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Bedtime, wake-up time and a 1-5 quality rating for the night ending on [date]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSheet(
    date: LocalDate,
    initial: SleepLog?,
    onSave: (bedtime: LocalTime, wakeTime: LocalTime, quality: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var bedtime by rememberSaveable { mutableStateOf(initial?.bedtime?.toLocalTime() ?: LocalTime.of(23, 0)) }
    var wakeTime by rememberSaveable { mutableStateOf(initial?.wakeTime?.toLocalTime() ?: LocalTime.of(7, 0)) }
    var quality by rememberSaveable { mutableIntStateOf(initial?.quality ?: 3) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    editing?.let { which ->
        val current = if (which == "bed") bedtime else wakeTime
        val state = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (which == "bed") "Bedtime" else "Woke up at") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = LocalTime.of(state.hour, state.minute)
                        if (which == "bed") bedtime = picked else wakeTime = picked
                        editing = null
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }

    // Bedtime later in the clock than wake-up means it was the previous evening.
    val minutes = run {
        val bedMinutes = bedtime.toSecondOfDay() / 60
        val wakeMinutes = wakeTime.toSecondOfDay() / 60
        if (bedMinutes > wakeMinutes) 24 * 60 - bedMinutes + wakeMinutes else wakeMinutes - bedMinutes
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Sleep · night before ${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))}", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { editing = "bed" }, modifier = Modifier.weight(1f)) {
                    Text("Bed ${bedtime.format(TIME_FORMAT)}")
                }
                OutlinedButton(onClick = { editing = "wake" }, modifier = Modifier.weight(1f)) {
                    Text("Up ${wakeTime.format(TIME_FORMAT)}")
                }
            }
            Text(
                text = "Slept ${formatSleepDuration(minutes.toLong())}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text("How did you sleep?", style = MaterialTheme.typography.labelLarge)
            Row {
                repeat(5) { index ->
                    IconButton(onClick = { quality = index + 1 }) {
                        Icon(
                            imageVector = if (index < quality) Icons.Filled.Star else AppIcons.StarBorder,
                            contentDescription = "${index + 1} stars",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                Button(onClick = { onSave(bedtime, wakeTime, quality) }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
