package com.macrosnap.app.ui

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

private const val MIN_RECHECK_MILLIS = 1_000L
private const val MAX_RECHECK_MILLIS = 60_000L

/**
 * Today's date, re-emitted when the clock passes midnight. Checks at least once a minute rather
 * than sleeping until midnight: a delay can run late while the phone is asleep.
 */
fun currentDates(): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now()
        emit(today)
        val untilMidnight = Duration.between(LocalDateTime.now(), today.plusDays(1).atStartOfDay()).toMillis()
        delay(untilMidnight.coerceIn(MIN_RECHECK_MILLIS, MAX_RECHECK_MILLIS))
    }
}.distinctUntilChanged()
