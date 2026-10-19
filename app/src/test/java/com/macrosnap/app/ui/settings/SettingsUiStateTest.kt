package com.macrosnap.app.ui.settings

import com.macrosnap.app.data.settings.AppSettings
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Profile
import com.macrosnap.app.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {

    private val saved = AppSettings(
        apiKey = "key-1",
        models = listOf("flash", "lite"),
        goals = Goals(kcal = 2200, protein = 140, carbs = 250, fat = 70),
        profile = null,
        nutrientOverrides = emptyMap(),
        waterGoalMl = 2500,
        sleepGoalHours = 8.0,
        restSeconds = 90,
        addWorkoutBurn = false,
    )

    private val allNutrients: Map<Nutrient, String> = Nutrient.defaultTargets(null).mapValues { it.value.toString() }

    private fun goalsState(kcal: String = "2200", protein: String = "140", carbs: String = "250", fat: String = "70") =
        SettingsUiState(kcalInput = kcal, proteinInput = protein, carbsInput = carbs, fatInput = fat, saved = saved)

    private fun profileState(age: String = "30", height: String = "180", weight: String = "80") = SettingsUiState(
        sex = Sex.MALE,
        ageInput = age,
        heightInput = height,
        weightInput = weight,
        activity = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN,
    )

    private fun wellnessState(water: String = "2500", sleep: String = "8", rest: String = "90") =
        SettingsUiState(waterGoalInput = water, sleepGoalInput = sleep, restSecondsInput = rest, saved = saved)

    @Test
    fun `model list is split on commas with blanks dropped`() {
        assertEquals(listOf("flash", "lite"), SettingsUiState(modelsInput = " flash , ,lite,").models)
        assertEquals(emptyList<String>(), SettingsUiState(modelsInput = " , ").models)
    }

    @Test
    fun `connection is dirty only when the key or models differ from what is saved`() {
        assertFalse(SettingsUiState(apiKeyInput = "other").isConnectionDirty)
        fun connection(key: String, models: String) =
            SettingsUiState(apiKeyInput = key, modelsInput = models, saved = saved).isConnectionDirty
        assertFalse(connection(" key-1 ", "flash, lite"))
        assertTrue(connection("key-2", "flash, lite"))
        assertTrue(connection("key-1", "lite, flash"))
    }

    @Test
    fun `goals parse when every field is in range`() {
        assertEquals(Goals(2000, 150, 200, 60), goalsState(" 2000 ", "150", "200", "60").enteredGoals)
        assertEquals(Goals(500, 0, 0, 0), goalsState("500", "0", "0", "0").enteredGoals)
    }

    @Test
    fun `any blank or out of range goal rejects them all`() {
        assertNull(goalsState(kcal = "499").enteredGoals)
        assertNull(goalsState(kcal = "10001").enteredGoals)
        assertNull(goalsState(protein = "1001").enteredGoals)
        assertNull(goalsState(carbs = "2001").enteredGoals)
        assertNull(goalsState(fat = "-1").enteredGoals)
        assertNull(goalsState(fat = "").enteredGoals)
        assertNull(goalsState(kcal = "2200.5").enteredGoals)
    }

    @Test
    fun `goals are dirty only when valid and different from saved`() {
        assertFalse(goalsState().isGoalsDirty)
        assertTrue(goalsState(kcal = "2300").isGoalsDirty)
        assertFalse(goalsState(kcal = "abc").isGoalsDirty)
        assertTrue(goalsState().copy(saved = null).isGoalsDirty)
    }

    @Test
    fun `profile accepts a decimal comma and suggests Mifflin-St Jeor goals`() {
        val state = profileState(height = "180,0", weight = " 80 ")

        assertEquals(
            Profile(Sex.MALE, 30, 180.0, 80.0, ActivityLevel.MODERATE, GoalType.MAINTAIN),
            state.enteredProfile,
        )
        // BMR 1780 x 1.55 = 2759 kcal; protein 1.8 g/kg; fat a quarter of kcal; carbs the rest.
        assertEquals(Goals(kcal = 2760, protein = 144, carbs = 373, fat = 77), state.suggestedGoals)
    }

    @Test
    fun `profile outside plausible ranges is rejected`() {
        assertNull(profileState(age = "12").enteredProfile)
        assertNull(profileState(age = "101").enteredProfile)
        assertNull(profileState(height = "99").enteredProfile)
        assertNull(profileState(height = "251").enteredProfile)
        assertNull(profileState(weight = "29.9").enteredProfile)
        assertNull(profileState(weight = "301").enteredProfile)
        assertNull(profileState(weight = "").suggestedGoals)
    }

    @Test
    fun `nutrient targets need every field filled`() {
        val complete = SettingsUiState(nutrientInputs = allNutrients, saved = saved)
        assertEquals(Nutrient.defaultTargets(null), complete.enteredNutrientTargets)

        val missing = complete.copy(nutrientInputs = allNutrients - Nutrient.IRON)
        assertNull(missing.enteredNutrientTargets)
        assertFalse(missing.isNutrientTargetsDirty)

        val garbled = complete.copy(nutrientInputs = allNutrients + (Nutrient.ZINC to "lots"))
        assertNull(garbled.enteredNutrientTargets)
    }

    @Test
    fun `nutrient targets are dirty when they differ from the saved targets`() {
        val unchanged = SettingsUiState(nutrientInputs = allNutrients, saved = saved)
        assertFalse(unchanged.isNutrientTargetsDirty)

        val changed = unchanged.copy(nutrientInputs = allNutrients + (Nutrient.VITAMIN_D to "50"))
        assertTrue(changed.isNutrientTargetsDirty)
        assertEquals(50.0, changed.enteredNutrientTargets!![Nutrient.VITAMIN_D]!!, 0.0)
    }

    @Test
    fun `wellness goals parse within their ranges`() {
        val state = wellnessState(water = " 3000 ", sleep = "7,5", rest = "120")
        assertEquals(3000, state.waterGoal)
        assertEquals(7.5, state.sleepGoal!!, 0.0)
        assertEquals(120, state.restSeconds)

        assertNull(wellnessState(water = "499").waterGoal)
        assertNull(wellnessState(water = "10001").waterGoal)
        assertNull(wellnessState(sleep = "3.9").sleepGoal)
        assertNull(wellnessState(sleep = "14.1").sleepGoal)
        assertNull(wellnessState(rest = "14").restSeconds)
        assertNull(wellnessState(rest = "601").restSeconds)
    }

    @Test
    fun `wellness is dirty when all parse and any one changed`() {
        assertFalse(wellnessState().isWellnessDirty)
        assertTrue(wellnessState(water = "3000").isWellnessDirty)
        assertTrue(wellnessState(sleep = "7").isWellnessDirty)
        assertTrue(wellnessState(rest = "60").isWellnessDirty)
        assertFalse(wellnessState(water = "3000", rest = "5").isWellnessDirty)
        assertFalse(wellnessState(sleep = "").isWellnessDirty)
    }
}
