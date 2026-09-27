package com.macrosnap.app.ui.progress

import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.SetRecord
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.model.WaterDay
import com.macrosnap.app.model.WeightEntry
import com.macrosnap.app.ui.toAmountOrNull
import java.time.LocalDate

data class ProgressUiState(
    val weightInput: String = "",
    /** Day the next weigh-in is logged for; null means today. */
    val weightDate: LocalDate? = null,
    val weights: List<WeightEntry> = emptyList(),
    val week: List<DayTotal> = emptyList(),
    val goals: Goals = Goals.DEFAULT,

    val sleepWeek: List<SleepLog> = emptyList(),
    val sleepGoalHours: Double = 8.0,
    val waterWeek: List<WaterDay> = emptyList(),
    val waterGoalMl: Int = 2500,
    val workoutDays: Set<LocalDate> = emptySet(),

    val trainedExercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val records: List<SetRecord> = emptyList(),
) {
    val parsedWeight: Double? get() = weightInput.toAmountOrNull()?.takeIf { it in 20.0..400.0 }
    val latestWeight: WeightEntry? get() = weights.lastOrNull()
    val weightChange: Double? get() = if (weights.size >= 2) weights.last().weightKg - weights.first().weightKg else null

    private val loggedDays: List<DayTotal> get() = week.filter { it.total.kcal > 0 }
    val averageKcal: Double? get() = loggedDays.takeIf { it.isNotEmpty() }?.map { it.total.kcal }?.average()
    val averageProtein: Double? get() = loggedDays.takeIf { it.isNotEmpty() }?.map { it.total.protein }?.average()

    val averageSleepHours: Double? get() = sleepWeek.takeIf { it.isNotEmpty() }?.map { it.hours }?.average()
    val averageSleepQuality: Double? get() = sleepWeek.takeIf { it.isNotEmpty() }?.map { it.quality.toDouble() }?.average()
    val averageWaterMl: Double? get() = waterWeek.filter { it.totalMl > 0 }.takeIf { it.isNotEmpty() }?.map { it.totalMl.toDouble() }?.average()

    /** Best estimated 1RM per workout, oldest first: the line on the strength chart. */
    val sessions: List<ExerciseSession>
        get() = records.groupBy { it.workoutId }.values.map { sets ->
            ExerciseSession(
                date = sets.first().date,
                best1Rm = sets.maxOf { it.estimated1Rm },
                topWeight = sets.maxOf { it.weightKg },
                volume = sets.sumOf { it.weightKg * it.reps },
                distanceM = sets.sumOf { it.distanceM },
                durationSec = sets.sumOf { it.durationSec },
            )
        }.sortedBy { it.date }

    val bestRecord: SetRecord? get() = records.maxByOrNull { it.estimated1Rm }
}

data class DayTotal(val date: LocalDate, val total: Macros)

data class ExerciseSession(
    val date: LocalDate,
    val best1Rm: Double,
    val topWeight: Double,
    val volume: Double,
    val distanceM: Double,
    val durationSec: Int,
)
