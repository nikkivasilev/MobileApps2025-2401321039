package com.macrosnap.app.model

import java.time.LocalDate

/**
 * A meal photo whose analysis failed for a reason that passes (Gemini overloaded, quota, no
 * connection), kept with the day, meal and hint it was taken for so it can be resubmitted later.
 */
data class PendingPhoto(
    val photoPath: String,
    val date: LocalDate,
    val mealType: MealType,
    val hint: String,
    val savedAt: Long,
)
