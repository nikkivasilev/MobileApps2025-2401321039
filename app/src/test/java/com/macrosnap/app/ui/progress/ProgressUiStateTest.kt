package com.macrosnap.app.ui.progress

import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.SetRecord
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.model.WaterDay
import com.macrosnap.app.model.WeightEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressUiStateTest {

    private val monday = LocalDate.of(2026, 10, 12)

    private fun day(offset: Long, kcal: Double, protein: Double = 0.0) =
        DayTotal(monday.plusDays(offset), Macros(kcal, protein, 0.0, 0.0))

    private fun sleep(offset: Long, hours: Long, quality: Int): SleepLog {
        val wake = monday.plusDays(offset).atTime(7, 0)
        return SleepLog(monday.plusDays(offset), wake.minusHours(hours), wake, quality)
    }

    private fun record(workoutId: Long, offset: Long, weight: Double, reps: Int, km: Double = 0.0, sec: Int = 0) =
        SetRecord(monday.plusDays(offset), workoutId, weight, reps, sec, km * 1000)

    @Test
    fun `weight input accepts a decimal comma within a plausible range`() {
        assertEquals(72.5, ProgressUiState(weightInput = " 72,5 ").parsedWeight!!, 0.0)
        assertEquals(20.0, ProgressUiState(weightInput = "20").parsedWeight!!, 0.0)
        assertNull(ProgressUiState(weightInput = "19.9").parsedWeight)
        assertNull(ProgressUiState(weightInput = "400.1").parsedWeight)
        assertNull(ProgressUiState(weightInput = "heavy").parsedWeight)
    }

    @Test
    fun `weight change needs two weigh-ins and runs first to last`() {
        val one = ProgressUiState(weights = listOf(WeightEntry(monday, 80.0)))
        assertNull(one.weightChange)
        assertEquals(WeightEntry(monday, 80.0), one.latestWeight)

        val three = ProgressUiState(
            weights = listOf(
                WeightEntry(monday, 80.0),
                WeightEntry(monday.plusDays(3), 81.0),
                WeightEntry(monday.plusDays(6), 78.5),
            ),
        )
        assertEquals(-1.5, three.weightChange!!, 1e-9)
        assertEquals(78.5, three.latestWeight!!.weightKg, 0.0)
        assertNull(ProgressUiState().latestWeight)
    }

    @Test
    fun `calorie and protein averages skip days with nothing logged`() {
        val state = ProgressUiState(week = listOf(day(0, 2000.0, 120.0), day(1, 0.0), day(2, 2400.0, 160.0)))

        assertEquals(2200.0, state.averageKcal!!, 1e-9)
        assertEquals(140.0, state.averageProtein!!, 1e-9)
        assertNull(ProgressUiState(week = listOf(day(0, 0.0))).averageKcal)
        assertNull(ProgressUiState().averageProtein)
    }

    @Test
    fun `sleep averages cover every logged night`() {
        val state = ProgressUiState(sleepWeek = listOf(sleep(0, 7, 3), sleep(1, 9, 5), sleep(2, 8, 4)))

        assertEquals(8.0, state.averageSleepHours!!, 1e-9)
        assertEquals(4.0, state.averageSleepQuality!!, 1e-9)
        assertNull(ProgressUiState().averageSleepHours)
        assertNull(ProgressUiState().averageSleepQuality)
    }

    @Test
    fun `water average skips days with no water`() {
        val state = ProgressUiState(
            waterWeek = listOf(
                WaterDay(monday, 2000, 4),
                WaterDay(monday.plusDays(1), 0, 0),
                WaterDay(monday.plusDays(2), 3000, 6),
            ),
        )

        assertEquals(2500.0, state.averageWaterMl!!, 1e-9)
        assertNull(ProgressUiState(waterWeek = listOf(WaterDay(monday, 0, 0))).averageWaterMl)
    }

    @Test
    fun `sessions summarise each workout oldest first`() {
        val state = ProgressUiState(
            records = listOf(
                record(workoutId = 2, offset = 4, weight = 100.0, reps = 5),
                record(workoutId = 1, offset = 1, weight = 90.0, reps = 5),
                record(workoutId = 2, offset = 4, weight = 110.0, reps = 1),
                record(workoutId = 1, offset = 1, weight = 80.0, reps = 10),
            ),
        )

        val sessions = state.sessions
        assertEquals(listOf(monday.plusDays(1), monday.plusDays(4)), sessions.map { it.date })

        val first = sessions[0]
        assertEquals(80.0 * (1 + 10 / 30.0), first.best1Rm, 1e-9)
        assertEquals(90.0, first.topWeight, 0.0)
        assertEquals(90.0 * 5 + 80.0 * 10, first.volume, 1e-9)

        val second = sessions[1]
        assertEquals(100.0 * (1 + 5 / 30.0), second.best1Rm, 1e-9)
        assertEquals(110.0, second.topWeight, 0.0)
        assertEquals(610.0, second.volume, 1e-9)
    }

    @Test
    fun `cardio sessions add up distance and time`() {
        val state = ProgressUiState(
            records = listOf(
                record(workoutId = 5, offset = 0, weight = 0.0, reps = 0, km = 3.0, sec = 900),
                record(workoutId = 5, offset = 0, weight = 0.0, reps = 0, km = 2.0, sec = 700),
            ),
        )

        val session = state.sessions.single()
        assertEquals(5000.0, session.distanceM, 1e-9)
        assertEquals(1600, session.durationSec)
        assertEquals(0.0, session.best1Rm, 0.0)
    }

    @Test
    fun `best record is the set with the highest estimated one-rep max`() {
        val heavySingle = record(workoutId = 1, offset = 0, weight = 110.0, reps = 1)
        val strongFive = record(workoutId = 2, offset = 2, weight = 100.0, reps = 5)

        assertEquals(strongFive, ProgressUiState(records = listOf(heavySingle, strongFive)).bestRecord)
        assertNull(ProgressUiState().bestRecord)
    }
}
