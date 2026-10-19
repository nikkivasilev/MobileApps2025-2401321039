package com.macrosnap.app.domain

import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Profile
import com.macrosnap.app.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCalculatorTest {

    private fun profile(
        sex: Sex = Sex.MALE,
        ageYears: Int = 30,
        heightCm: Double = 180.0,
        weightKg: Double = 80.0,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        goal: GoalType = GoalType.MAINTAIN,
    ) = Profile(sex, ageYears, heightCm, weightKg, activity, goal)

    @Test
    fun `maintenance for a moderately active man`() {
        // BMR = 800 + 1125 - 150 + 5 = 1780; x1.55 = 2759.
        assertEquals(Goals(kcal = 2760, protein = 144, carbs = 373, fat = 77), GoalCalculator.calculate(profile()))
    }

    @Test
    fun `cut for a sedentary woman`() {
        // BMR = 600 + 1031.25 - 125 - 161 = 1345.25; x1.2 x0.8 = 1291.44.
        val goals = GoalCalculator.calculate(
            profile(
                sex = Sex.FEMALE,
                ageYears = 25,
                heightCm = 165.0,
                weightKg = 60.0,
                activity = ActivityLevel.SEDENTARY,
                goal = GoalType.LOSE,
            ),
        )
        assertEquals(Goals(kcal = 1290, protein = 120, carbs = 122, fat = 36), goals)
    }

    @Test
    fun `women get 166 kcal less BMR than men of the same build`() {
        val male = GoalCalculator.calculate(profile(activity = ActivityLevel.SEDENTARY))
        val female = GoalCalculator.calculate(profile(sex = Sex.FEMALE, activity = ActivityLevel.SEDENTARY))
        // 166 x 1.2 = 199.2, rounded to tens on both sides.
        assertEquals(200, male.kcal - female.kcal)
        assertEquals(male.protein, female.protein)
    }

    @Test
    fun `gaining adds ten percent over maintenance`() {
        val maintain = GoalCalculator.calculate(profile())
        val gain = GoalCalculator.calculate(profile(goal = GoalType.GAIN))
        // 2759 x 1.1 = 3034.9.
        assertEquals(3030, gain.kcal)
        assertEquals(maintain.protein, gain.protein)
        assertTrue(gain.carbs > maintain.carbs)
    }

    @Test
    fun `calories never drop below 1200`() {
        // BMR = 350 + 875 - 400 - 161 = 664; x1.2 x0.8 = 637.
        val goals = GoalCalculator.calculate(
            profile(
                sex = Sex.FEMALE,
                ageYears = 80,
                heightCm = 140.0,
                weightKg = 35.0,
                activity = ActivityLevel.SEDENTARY,
                goal = GoalType.LOSE,
            ),
        )
        assertEquals(Goals(kcal = 1200, protein = 70, carbs = 155, fat = 33), goals)
    }

    @Test
    fun `carbs bottom out at zero when protein and fat take all the calories`() {
        // BMR = 1500 + 625 - 500 - 161 = 1464; x0.96 = 1405.44 kcal, but protein alone is 300 g = 1200 kcal.
        val goals = GoalCalculator.calculate(
            profile(
                sex = Sex.FEMALE,
                ageYears = 100,
                heightCm = 100.0,
                weightKg = 150.0,
                activity = ActivityLevel.SEDENTARY,
                goal = GoalType.LOSE,
            ),
        )
        assertEquals(Goals(kcal = 1410, protein = 300, carbs = 0, fat = 39), goals)
    }

    @Test
    fun `fat is a quarter of calories and macros add back up to the target`() {
        listOf(
            profile(),
            profile(goal = GoalType.GAIN, activity = ActivityLevel.VERY_ACTIVE),
            profile(sex = Sex.FEMALE, weightKg = 55.0, goal = GoalType.LOSE, activity = ActivityLevel.LIGHT),
        ).forEach {
            val goals = GoalCalculator.calculate(it)
            assertEquals(goals.kcal * 0.25 / 9, goals.fat.toDouble(), 1.0)
            val fromMacros = goals.protein * 4 + goals.carbs * 4 + goals.fat * 9
            assertEquals(goals.kcal.toDouble(), fromMacros.toDouble(), 15.0)
            assertEquals(0, goals.kcal % 10)
        }
    }

    @Test
    fun `more activity means more calories`() {
        val kcal = ActivityLevel.entries.map { GoalCalculator.calculate(profile(activity = it)).kcal }
        assertEquals(kcal.sorted(), kcal)
        assertEquals(kcal.size, kcal.toSet().size)
    }
}
