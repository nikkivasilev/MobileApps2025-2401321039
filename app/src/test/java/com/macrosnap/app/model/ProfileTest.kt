package com.macrosnap.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {

    @Test
    fun `activity multipliers grow with activity, starting from sedentary`() {
        val multipliers = ActivityLevel.entries.map { it.multiplier }
        assertEquals(1.2, multipliers.first(), 0.0)
        assertEquals(multipliers.sorted(), multipliers)
        assertEquals(multipliers.size, multipliers.toSet().size)
    }

    @Test
    fun `losing eats below maintenance and gaining above it`() {
        assertEquals(1.0, GoalType.MAINTAIN.calorieFactor, 0.0)
        assertTrue(GoalType.LOSE.calorieFactor < 1.0)
        assertTrue(GoalType.GAIN.calorieFactor > 1.0)
    }

    @Test
    fun `a cut asks for the most protein to keep muscle`() {
        assertTrue(GoalType.LOSE.proteinPerKg > GoalType.MAINTAIN.proteinPerKg)
        assertTrue(GoalType.LOSE.proteinPerKg > GoalType.GAIN.proteinPerKg)
    }
}
