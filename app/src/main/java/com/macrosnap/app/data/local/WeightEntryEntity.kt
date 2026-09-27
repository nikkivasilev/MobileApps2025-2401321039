package com.macrosnap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val epochDay: Long,
    val weightKg: Double,
)
