package com.macrosnap.app.model

import java.time.LocalDate
import java.time.LocalTime

data class FoodEntry(
    val id: Long,
    val date: LocalDate,
    val createdAt: Long,
    val mealType: MealType,
    val item: FoodItem,
    val source: EntrySource,
    val photoPath: String?,
)

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack");

    companion object {
        fun forTime(time: LocalTime): MealType = when (time.hour) {
            in 4..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 17..21 -> DINNER
            else -> SNACK
        }
    }
}

enum class EntrySource { PHOTO, BARCODE, MANUAL, TEXT, RECIPE, HISTORY }
