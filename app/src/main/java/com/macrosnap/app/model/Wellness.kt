package com.macrosnap.app.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** One night's sleep, filed under the morning you woke up. */
data class SleepLog(
    val date: LocalDate,
    val bedtime: LocalDateTime,
    val wakeTime: LocalDateTime,
    /** 1 (terrible) to 5 (great). */
    val quality: Int,
) {
    val minutes: Long get() = Duration.between(bedtime, wakeTime).toMinutes().coerceAtLeast(0)
    val hours: Double get() = minutes / 60.0
}

data class WaterDay(
    val date: LocalDate,
    val totalMl: Int,
    val entries: Int,
)
