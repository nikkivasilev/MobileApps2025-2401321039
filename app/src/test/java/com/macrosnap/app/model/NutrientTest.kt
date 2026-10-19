package com.macrosnap.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientTest {

    @Test
    fun `rda picks the female value only for women`() {
        assertEquals(18.0, Nutrient.IRON.rda(Sex.FEMALE), 0.0)
        assertEquals(8.0, Nutrient.IRON.rda(Sex.MALE), 0.0)
    }

    @Test
    fun `rda without a sex falls back to the male value`() {
        Nutrient.entries.forEach { assertEquals(it.name, it.rdaMale, it.rda(null), 0.0) }
    }

    @Test
    fun `default targets cover every nutrient for the given sex`() {
        val female = Nutrient.defaultTargets(Sex.FEMALE)
        assertEquals(Nutrient.entries.toSet(), female.keys)
        assertEquals(75.0, female.getValue(Nutrient.VITAMIN_C), 0.0)
        assertEquals(90.0, Nutrient.defaultTargets(Sex.MALE).getValue(Nutrient.VITAMIN_C), 0.0)
        assertEquals(Nutrient.defaultTargets(Sex.MALE), Nutrient.defaultTargets(null))
    }

    @Test
    fun `only caffeine is a limit`() {
        assertEquals(listOf(Nutrient.CAFFEINE), Nutrient.entries.filter { it.isLimit })
    }

    @Test
    fun `schema and Open Food Facts keys are unique and scales match units`() {
        assertEquals(Nutrient.entries.size, Nutrient.entries.map { it.schemaKey }.toSet().size)
        val offKeys = Nutrient.entries.mapNotNull { it.offKey }
        assertEquals(offKeys.size, offKeys.toSet().size)
        // Open Food Facts stores grams; the scale converts to the nutrient's own unit.
        val expectedScale = mapOf("g" to 1.0, "mg" to 1e3, "µg" to 1e6)
        Nutrient.entries.forEach { assertEquals(it.name, expectedScale.getValue(it.unit), it.offScale, 0.0) }
    }

    @Test
    fun `rdas are positive`() {
        Nutrient.entries.forEach {
            assertTrue(it.name, it.rdaMale > 0)
            assertTrue(it.name, it.rdaFemale > 0)
        }
    }

    @Test
    fun `scaled multiplies every known amount and keeps unknowns absent`() {
        val scaled = mapOf(Nutrient.IRON to 2.0, Nutrient.ZINC to 1.5).scaled(3.0)
        assertEquals(mapOf(Nutrient.IRON to 6.0, Nutrient.ZINC to 4.5), scaled)
        assertTrue(emptyMap<Nutrient, Double>().scaled(10.0).isEmpty())
    }

    @Test
    fun `add sums shared nutrients and keeps the rest from both sides`() {
        val result = mapOf(Nutrient.IRON to 2.0, Nutrient.ZINC to 1.0)
            .add(mapOf(Nutrient.IRON to 3.0, Nutrient.CALCIUM to 100.0))
        assertEquals(mapOf(Nutrient.IRON to 5.0, Nutrient.ZINC to 1.0, Nutrient.CALCIUM to 100.0), result)
    }

    @Test
    fun `add with an empty side returns the other one unchanged`() {
        val micros = mapOf(Nutrient.IRON to 2.0)
        assertSame(micros, micros.add(emptyMap()))
        assertSame(micros, emptyMap<Nutrient, Double>().add(micros))
    }

    @Test
    fun `add does not mutate either input`() {
        val left = mutableMapOf(Nutrient.IRON to 2.0)
        val right = mapOf(Nutrient.IRON to 3.0)
        left.add(right)
        assertEquals(mapOf(Nutrient.IRON to 2.0), left)
        assertEquals(mapOf(Nutrient.IRON to 3.0), right)
    }

    @Test
    fun `an explicit zero is kept, it is not the same as unknown`() {
        val result = mapOf(Nutrient.IRON to 0.0).add(mapOf(Nutrient.ZINC to 1.0))
        assertTrue(Nutrient.IRON in result)
        assertFalse(Nutrient.CALCIUM in result)
    }

    @Test
    fun `sumMicros folds a list and is empty for no entries`() {
        assertTrue(emptyList<Micros>().sumMicros().isEmpty())
        val total = listOf(
            mapOf(Nutrient.VITAMIN_C to 10.0),
            emptyMap(),
            mapOf(Nutrient.VITAMIN_C to 5.0, Nutrient.IRON to 1.0),
            mapOf(Nutrient.IRON to 0.5),
        ).sumMicros()
        assertEquals(mapOf(Nutrient.VITAMIN_C to 15.0, Nutrient.IRON to 1.5), total)
    }
}
