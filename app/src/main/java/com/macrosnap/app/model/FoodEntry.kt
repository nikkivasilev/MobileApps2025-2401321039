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

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    ;

    companion object {
        private val BREAKFAST_HOURS = 4..10
        private val LUNCH_HOURS = 11..15
        private val DINNER_HOURS = 17..21

        fun forTime(time: LocalTime): MealType = when (time.hour) {
            in BREAKFAST_HOURS -> BREAKFAST
            in LUNCH_HOURS -> LUNCH
            in DINNER_HOURS -> DINNER
            else -> SNACK
        }
    }
}

enum class EntrySource { PHOTO, BARCODE, MANUAL, TEXT, RECIPE, HISTORY }
