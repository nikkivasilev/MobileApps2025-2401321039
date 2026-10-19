package com.macrosnap.app.data

import com.macrosnap.app.model.Exercise
import com.macrosnap.app.model.ExerciseType
import com.macrosnap.app.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLibraryTest {

    private val seeds = ExerciseLibrary.SEEDS

    @Test
    fun `seed names are unique ignoring case`() {
        val names = seeds.map { it.name.trim().lowercase() }
        assertEquals(names.filter { name -> names.count { it == name } > 1 }.distinct(), emptyList<String>())
    }

    @Test
    fun `every seed has its own display string`() {
        assertEquals(seeds.size, seeds.map { it.nameRes }.toSet().size)
    }

    @Test
    fun `names are trimmed and not blank`() {
        seeds.forEach {
            assertTrue(it.name.isNotBlank())
            assertEquals(it.name.trim(), it.name)
        }
    }

    @Test
    fun `cardio exercises and the cardio group go together`() {
        seeds.forEach {
            assertEquals(it.name, it.type == ExerciseType.CARDIO, it.group == MuscleGroup.CARDIO)
        }
    }

    @Test
    fun `every muscle group except other has something to pick`() {
        val covered = seeds.map { it.group }.toSet()
        assertEquals(MuscleGroup.entries.toSet() - MuscleGroup.OTHER, covered)
    }

    @Test
    fun `defaults mirror the seeds as built-in exercises`() {
        val defaults = ExerciseLibrary.DEFAULTS
        assertEquals(seeds.map { it.name }, defaults.map { it.name })
        assertEquals(seeds.map { it.group.name }, defaults.map { it.muscleGroup })
        assertEquals(seeds.map { it.type.name }, defaults.map { it.type })
        defaults.forEach {
            assertFalse(it.name, it.isCustom)
            assertEquals(it.name, 0L, it.id)
            assertEquals(it.name, Exercise.DEFAULT_REPS, it.defaultReps)
        }
    }

    @Test
    fun `cardio starts with one set and strength with the default count`() {
        ExerciseLibrary.DEFAULTS.forEach {
            val expected = if (it.type == ExerciseType.CARDIO.name) 1 else Exercise.DEFAULT_SETS
            assertEquals(it.name, expected, it.defaultSets)
            assertTrue(it.name, it.defaultSets in Exercise.SETS_RANGE)
        }
    }

    @Test
    fun `stored enum names parse back`() {
        ExerciseLibrary.DEFAULTS.forEach {
            MuscleGroup.valueOf(it.muscleGroup)
            ExerciseType.valueOf(it.type)
        }
    }
}
