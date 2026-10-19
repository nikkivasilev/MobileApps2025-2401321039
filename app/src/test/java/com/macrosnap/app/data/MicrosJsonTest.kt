package com.macrosnap.app.data

import com.macrosnap.app.model.Nutrient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrosJsonTest {

    @Test
    fun `empty micros are stored as null`() {
        assertNull(MicrosJson.encode(emptyMap()))
    }

    @Test
    fun `encodes keyed by enum name`() {
        assertEquals("""{"VITAMIN_C":90.0}""", MicrosJson.encode(mapOf(Nutrient.VITAMIN_C to 90.0)))
    }

    @Test
    fun `every nutrient survives a round trip`() {
        val micros = Nutrient.entries.mapIndexed { index, nutrient -> nutrient to index * 1.25 + 0.1 }.toMap()
        assertEquals(micros, MicrosJson.decode(MicrosJson.encode(micros)))
    }

    @Test
    fun `null, empty and blank text decode to nothing`() {
        assertTrue(MicrosJson.decode(null).isEmpty())
        assertTrue(MicrosJson.decode("").isEmpty())
        assertTrue(MicrosJson.decode("   ").isEmpty())
        assertTrue(MicrosJson.decode("{}").isEmpty())
    }

    @Test
    fun `whole numbers decode as amounts`() {
        assertEquals(mapOf(Nutrient.IRON to 8.0), MicrosJson.decode("""{"IRON": 8}"""))
    }

    @Test
    fun `nutrients unknown to this build are skipped and the rest kept`() {
        val decoded = MicrosJson.decode("""{"IRON": 8.5, "VITAMIN_K": 120.0, "iron": 3.0}""")
        assertEquals(mapOf(Nutrient.IRON to 8.5), decoded)
    }

    @Test
    fun `malformed json decodes to nothing instead of crashing`() {
        listOf(
            "not json",
            "{\"IRON\": ",
            "[1, 2, 3]",
            "{\"IRON\": \"lots\"}",
            "{\"IRON\": null}",
            "{\"IRON\": {\"amount\": 1}}",
            "42",
        ).forEach { assertTrue(it, MicrosJson.decode(it).isEmpty()) }
    }
}
