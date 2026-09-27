package com.macrosnap.app.model

data class Supplement(
    val id: Long,
    val name: String,
    /** How one dose is described on the label: "5 g", "1 capsule", "2 softgels". */
    val doseLabel: String,
    val timesPerDay: Int,
    /** Nutrient content of one dose. */
    val micros: Micros,
    val isActive: Boolean,
)

data class SupplementStatus(
    val supplement: Supplement,
    val takenCount: Int,
    /** What the doses taken that day contained, as recorded when each was taken. */
    val micros: Micros,
) {
    val isDone: Boolean get() = takenCount >= supplement.timesPerDay
}
