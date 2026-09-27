package com.macrosnap.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries", indices = [Index("epochDay")])
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val createdAt: Long,
    val mealType: String,
    val name: String,
    val grams: Double,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val source: String,
    val photoPath: String?,
    val micros: String? = null,
)
