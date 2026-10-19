package com.macrosnap.app.ui.supplements

import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Supplement
import com.macrosnap.app.ui.components.DescribeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplementEditorUiStateTest {

    private val filled = SupplementEditorUiState(
        id = 0,
        name = "  Vitamin D3 ",
        doseLabel = " 1 softgel ",
        timesPerDayInput = " 2 ",
        nutrientInputs = mapOf(
            Nutrient.VITAMIN_D to "25",
            Nutrient.CALCIUM to "0",
            Nutrient.ZINC to "",
            Nutrient.MAGNESIUM to "12,5",
        ),
        isActive = false,
    )

    @Test
    fun `an id of zero means a new supplement`() {
        assertFalse(filled.isExisting)
        assertTrue(filled.copy(id = 4).isExisting)
    }

    @Test
    fun `times per day must be a whole number from 1 to 10`() {
        assertEquals(1, filled.copy(timesPerDayInput = "1").timesPerDay)
        assertEquals(10, filled.copy(timesPerDayInput = "10").timesPerDay)
        assertNull(filled.copy(timesPerDayInput = "0").timesPerDay)
        assertNull(filled.copy(timesPerDayInput = "11").timesPerDay)
        assertNull(filled.copy(timesPerDayInput = "").timesPerDay)
        assertNull(filled.copy(timesPerDayInput = "1.5").timesPerDay)
    }

    @Test
    fun `supplement is trimmed and keeps only nutrients with an amount`() {
        assertEquals(
            Supplement(
                id = 0,
                name = "Vitamin D3",
                doseLabel = "1 softgel",
                timesPerDay = 2,
                micros = mapOf(Nutrient.VITAMIN_D to 25.0, Nutrient.MAGNESIUM to 12.5),
                isActive = false,
            ),
            filled.supplement,
        )
    }

    @Test
    fun `blank dose label stays empty for the screen to fill in`() {
        assertEquals("", filled.copy(doseLabel = "   ").supplement!!.doseLabel)
    }

    @Test
    fun `supplement needs a name and a valid count`() {
        assertNull(filled.copy(name = "  ").supplement)
        assertNull(filled.copy(timesPerDayInput = "0").supplement)
        assertFalse(filled.copy(name = "").canSave)
    }

    @Test
    fun `cannot save while saving or while the description is being analysed`() {
        assertTrue(filled.canSave)
        assertTrue(filled.copy(describeStatus = DescribeStatus.Failed(DataError.Network)).canSave)
        assertFalse(filled.copy(isSaving = true).canSave)
        assertFalse(filled.copy(describeStatus = DescribeStatus.Loading).canSave)
    }
}
