package com.macrosnap.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.macrosnap.app.data.MicrosJson
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Profile
import com.macrosnap.app.model.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        // A failed read shouldn't take the whole app down; fall back to defaults.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    suspend fun setApiKey(apiKey: String) {
        context.settingsDataStore.edit { it[Keys.API_KEY] = apiKey.trim() }
    }

    suspend fun setModels(models: List<String>) {
        context.settingsDataStore.edit { it[Keys.MODELS] = models.joinToString(",") }
    }

    suspend fun setGoals(goals: Goals) {
        context.settingsDataStore.edit {
            it[Keys.GOAL_KCAL] = goals.kcal
            it[Keys.GOAL_PROTEIN] = goals.protein
            it[Keys.GOAL_CARBS] = goals.carbs
            it[Keys.GOAL_FAT] = goals.fat
        }
    }

    suspend fun setProfile(profile: Profile) {
        context.settingsDataStore.edit {
            it[Keys.SEX] = profile.sex.name
            it[Keys.AGE] = profile.ageYears
            it[Keys.HEIGHT_CM] = profile.heightCm
            it[Keys.WEIGHT_KG] = profile.weightKg
            it[Keys.ACTIVITY] = profile.activity.name
            it[Keys.GOAL_TYPE] = profile.goal.name
        }
    }

    suspend fun setWellnessGoals(waterGoalMl: Int, sleepGoalHours: Double, restSeconds: Int) {
        context.settingsDataStore.edit {
            it[Keys.WATER_GOAL_ML] = waterGoalMl
            it[Keys.SLEEP_GOAL_HOURS] = sleepGoalHours
            it[Keys.REST_SECONDS] = restSeconds
        }
    }

    suspend fun setAddWorkoutBurn(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ADD_WORKOUT_BURN] = enabled }
    }

    suspend fun hideFood(key: String) {
        context.settingsDataStore.edit {
            val hidden = decodeHidden(it[Keys.HIDDEN_FOODS]) + (key to System.currentTimeMillis())
            it[Keys.HIDDEN_FOODS] = hiddenJson.encodeToString(hidden)
        }
    }

    suspend fun setNutrientOverrides(overrides: Micros) {
        context.settingsDataStore.edit {
            val encoded = MicrosJson.encode(overrides)
            if (encoded == null) it.remove(Keys.NUTRIENT_OVERRIDES) else it[Keys.NUTRIENT_OVERRIDES] = encoded
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val models = this[Keys.MODELS].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return AppSettings(
            apiKey = this[Keys.API_KEY].orEmpty(),
            models = models.ifEmpty { AppSettings.DEFAULT_MODELS },
            goals = Goals(
                kcal = this[Keys.GOAL_KCAL] ?: Goals.DEFAULT.kcal,
                protein = this[Keys.GOAL_PROTEIN] ?: Goals.DEFAULT.protein,
                carbs = this[Keys.GOAL_CARBS] ?: Goals.DEFAULT.carbs,
                fat = this[Keys.GOAL_FAT] ?: Goals.DEFAULT.fat,
            ),
            profile = toProfile(),
            nutrientOverrides = MicrosJson.decode(this[Keys.NUTRIENT_OVERRIDES]),
            waterGoalMl = this[Keys.WATER_GOAL_ML] ?: AppSettings.DEFAULT_WATER_GOAL_ML,
            sleepGoalHours = this[Keys.SLEEP_GOAL_HOURS] ?: AppSettings.DEFAULT_SLEEP_GOAL_HOURS,
            restSeconds = this[Keys.REST_SECONDS] ?: AppSettings.DEFAULT_REST_SECONDS,
            addWorkoutBurn = this[Keys.ADD_WORKOUT_BURN] ?: false,
            hiddenFoods = decodeHidden(this[Keys.HIDDEN_FOODS]),
        )
    }

    private fun decodeHidden(text: String?): Map<String, Long> {
        if (text.isNullOrBlank()) return emptyMap()
        return try {
            hiddenJson.decodeFromString<Map<String, Long>>(text)
        } catch (e: SerializationException) {
            emptyMap()
        } catch (e: IllegalArgumentException) {
            emptyMap()
        }
    }

    private fun Preferences.toProfile(): Profile? {
        return Profile(
            sex = Sex.entries.firstOrNull { it.name == this[Keys.SEX] } ?: return null,
            ageYears = this[Keys.AGE] ?: return null,
            heightCm = this[Keys.HEIGHT_CM] ?: return null,
            weightKg = this[Keys.WEIGHT_KG] ?: return null,
            activity = ActivityLevel.entries.firstOrNull { it.name == this[Keys.ACTIVITY] } ?: return null,
            goal = GoalType.entries.firstOrNull { it.name == this[Keys.GOAL_TYPE] } ?: return null,
        )
    }

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val MODELS = stringPreferencesKey("gemini_models")
        val GOAL_KCAL = intPreferencesKey("goal_kcal")
        val GOAL_PROTEIN = intPreferencesKey("goal_protein")
        val GOAL_CARBS = intPreferencesKey("goal_carbs")
        val GOAL_FAT = intPreferencesKey("goal_fat")
        val SEX = stringPreferencesKey("profile_sex")
        val AGE = intPreferencesKey("profile_age")
        val HEIGHT_CM = doublePreferencesKey("profile_height_cm")
        val WEIGHT_KG = doublePreferencesKey("profile_weight_kg")
        val ACTIVITY = stringPreferencesKey("profile_activity")
        val GOAL_TYPE = stringPreferencesKey("profile_goal_type")
        val NUTRIENT_OVERRIDES = stringPreferencesKey("nutrient_target_overrides")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val SLEEP_GOAL_HOURS = doublePreferencesKey("sleep_goal_hours")
        val REST_SECONDS = intPreferencesKey("rest_seconds")
        val ADD_WORKOUT_BURN = booleanPreferencesKey("add_workout_burn")
        val HIDDEN_FOODS = stringPreferencesKey("hidden_foods")
    }

    private companion object {
        val hiddenJson = Json { ignoreUnknownKeys = true }
    }
}
