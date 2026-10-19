package com.macrosnap.app.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WellnessTest {

    private val morning = LocalDate.of(2026, 3, 10)

    private fun sleep(bedtime: LocalDateTime, wakeTime: LocalDateTime) =
        SleepLog(morning, bedtime, wakeTime, quality = MAX_SLEEP_QUALITY)

    @Test
    fun `sleep across midnight is counted in full`() {
        val log = sleep(LocalDateTime.of(2026, 3, 9, 23, 30), LocalDateTime.of(2026, 3, 10, 7, 0))
        assertEquals(450, log.minutes)
        assertEquals(7.5, log.hours, 1e-9)
    }

    @Test
    fun `partial minutes are dropped`() {
        val log = sleep(LocalDateTime.of(2026, 3, 10, 1, 0, 0), LocalDateTime.of(2026, 3, 10, 1, 59, 59))
        assertEquals(59, log.minutes)
    }

    @Test
    fun `waking before bedtime counts as no sleep rather than negative`() {
        val log = sleep(LocalDateTime.of(2026, 3, 10, 8, 0), LocalDateTime.of(2026, 3, 10, 6, 0))
        assertEquals(0, log.minutes)
        assertEquals(0.0, log.hours, 0.0)
    }
}
