package com.macrosnap.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val doseLabel: String,
    val timesPerDay: Int,
    val micros: String?,
    val isActive: Boolean,
    val createdAt: Long,
)

/** One row per dose taken; a supplement taken twice a day has two rows for that day. */
@Entity(
    tableName = "supplement_logs",
    foreignKeys = [
        ForeignKey(
            entity = SupplementEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("supplementId"), Index("epochDay")],
)
data class SupplementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementId: Long,
    val epochDay: Long,
    val takenAt: Long,
)
