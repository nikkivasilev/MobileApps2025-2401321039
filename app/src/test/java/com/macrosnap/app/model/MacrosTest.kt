package com.macrosnap.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MacrosTest {

    private fun assertMacros(expected: Macros, actual: Macros) {
        assertEquals(expected.kcal, actual.kcal, 1e-9)
        assertEquals(expected.protein, actual.protein, 1e-9)
        assertEquals(expected.carbs, actual.carbs, 1e-9)
        assertEquals(expected.fat, actual.fat, 1e-9)
    }

    @Test
    fun `plus adds every field independently`() {
        assertMacros(Macros(300.0, 25.0, 40.0, 9.0), Macros(100.0, 5.0, 30.0, 1.0) + Macros(200.0, 20.0, 10.0, 8.0))
    }

    @Test
    fun `times scales every field`() {
        assertMacros(Macros(150.0, 7.5, 30.0, 3.0), Macros(100.0, 5.0, 20.0, 2.0) * 1.5)
        assertMacros(Macros.ZERO, Macros(100.0, 5.0, 20.0, 2.0) * 0.0)
    }

    @Test
    fun `zero is the identity for plus`() {
        val macros = Macros(250.0, 12.0, 30.0, 8.0)
        assertEquals(macros, macros + Macros.ZERO)
        assertEquals(macros, Macros.ZERO + macros)
    }

    @Test
    fun `sum of nothing is zero`() {
        assertEquals(Macros.ZERO, emptyList<Macros>().sum())
    }

    @Test
    fun `sum adds all entries`() {
        val total = listOf(Macros(100.0, 1.0, 2.0, 3.0), Macros(50.0, 4.0, 5.0, 6.0), Macros(25.0, 0.5, 0.0, 1.0)).sum()
        assertMacros(Macros(175.0, 5.5, 7.0, 10.0), total)
    }
}
