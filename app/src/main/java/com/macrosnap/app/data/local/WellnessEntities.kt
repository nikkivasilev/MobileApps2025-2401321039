package com.macrosnap.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs", indices = [Index("epochDay")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val ml: Int,
    val loggedAt: Long,
)

/** Keyed by the morning of waking; bedtime/wakeTime are epoch millis. */
@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val epochDay: Long,
    val bedtime: Long,
    val wakeTime: Long,
    val quality: Int,
)
