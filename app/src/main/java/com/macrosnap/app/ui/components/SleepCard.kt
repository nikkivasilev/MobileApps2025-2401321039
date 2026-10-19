package com.macrosnap.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macrosnap.app.R
import com.macrosnap.app.model.MAX_SLEEP_QUALITY
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.ui.DatePatterns
import com.macrosnap.app.ui.formatted
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun formatSleepDuration(minutes: Long): String =
    stringResource(R.string.today_sleep_duration, minutes / 60, (minutes % 60).toString().padStart(2, '0'))

@Composable
fun SleepCard(sleep: SleepLog?, goalHours: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = AppTheme.colors.sleep
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(AppIcons.Moon, tint, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.today_sleep), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        if (sleep == null) {
            Text(
                "—",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.today_sleep_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(
                        R.string.today_sleep_short,
                        sleep.minutes / 60,
                        (sleep.minutes % 60).toString().padStart(2, '0'),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                text = "${sleep.bedtime.toLocalTime().formatted(
                    DatePatterns.TIME,
                )} → ${sleep.wakeTime.toLocalTime().formatted(DatePatterns.TIME)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillBar(
                progress = if (goalHours > 0) (sleep.hours / goalHours).toFloat() else 0f,
                color = tint,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )
            Row {
                repeat(MAX_SLEEP_QUALITY) { index ->
                    Icon(
                        imageVector = if (index < sleep.quality) Icons.Filled.Star else AppIcons.StarBorder,
                        contentDescription = null,
                        tint = if (index < sleep.quality) {
                            AppTheme.colors.highlight
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(18.dp),
                    )
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
            title = { Text(stringResource(if (which == "bed") R.string.today_bedtime else R.string.today_woke_up_at)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = LocalTime.of(state.hour, state.minute)
                        if (which == "bed") bedtime = picked else wakeTime = picked
                        editing = null
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) }
            },
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
        containerColor = MaterialTheme.colorScheme.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.today_sleep_sheet_title, date.formatted(DatePatterns.SHORT)),
                style = MaterialTheme.typography.titleLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { editing = "bed" }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.today_bed_at, bedtime.formatted(DatePatterns.TIME)))
                }
                OutlinedButton(onClick = { editing = "wake" }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.today_up_at, wakeTime.formatted(DatePatterns.TIME)))
                }
            }
            Text(
                text = stringResource(R.string.today_slept, formatSleepDuration(minutes.toLong())),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(R.string.today_sleep_quality), style = MaterialTheme.typography.labelLarge)
            Row {
                repeat(MAX_SLEEP_QUALITY) { index ->
                    IconButton(onClick = { quality = index + 1 }) {
                        Icon(
                            imageVector = if (index < quality) Icons.Filled.Star else AppIcons.StarBorder,
                            contentDescription = pluralStringResource(R.plurals.today_stars, index + 1, index + 1),
                            tint = if (index < quality) {
                                AppTheme.colors.highlight
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { onSave(bedtime, wakeTime, quality) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
