package com.macrosnap.app

import android.content.Context
import androidx.room.Room
import com.macrosnap.app.data.ApiKeyChecker
import com.macrosnap.app.data.FoodAnalyzer
import com.macrosnap.app.data.FoodRepository
import com.macrosnap.app.data.GeminiFoodAnalyzer
import com.macrosnap.app.data.ImageEncoder
import com.macrosnap.app.data.PhotoStore
import com.macrosnap.app.data.ProductRepository
import com.macrosnap.app.data.RecipeRepository
import com.macrosnap.app.data.SupplementRepository
import com.macrosnap.app.data.WellnessRepository
import com.macrosnap.app.data.WorkoutRepository
import com.macrosnap.app.data.WeightRepository
import com.macrosnap.app.data.local.AppDatabase
import com.macrosnap.app.data.remote.GeminiApi
import com.macrosnap.app.data.remote.OpenFoodFactsApi
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.ui.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Hand-rolled DI: one single-module personal app doesn't earn a DI framework. */
class AppContainer(context: Context) {

    private val io = Dispatchers.IO

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Vision requests on a free tier can sit in a queue for a while before answering.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Open Food Facts asks every client to identify itself.
            chain.proceed(chain.request().newBuilder().header("User-Agent", "MacroSnap/1.0 (Android)").build())
        }
        .build()

    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "macrosnap.db")
        .addMigrations(*AppDatabase.MIGRATIONS)
        .build()

    private val geminiApi: GeminiApi = retrofit(GeminiApi.BASE_URL).create(GeminiApi::class.java)

    val settingsStore = SettingsStore(context)
    val photoStore = PhotoStore(context, io)
    val foodRepository = FoodRepository(database.foodDao(), database.pendingPhotoDao(), photoStore, settingsStore)
    val weightRepository = WeightRepository(database.weightDao())
    val recipeRepository = RecipeRepository(database.recipeDao())
    val supplementRepository = SupplementRepository(database.supplementDao())
    val wellnessRepository = WellnessRepository(database.wellnessDao())
    val workoutRepository = WorkoutRepository(database.workoutDao())
    val foodAnalyzer: FoodAnalyzer = GeminiFoodAnalyzer(geminiApi, settingsStore, ImageEncoder(io), json) {
        AppLanguage.effectiveLocale().getDisplayLanguage(Locale.ENGLISH)
    }
    val productRepository = ProductRepository(
        api = retrofit(OpenFoodFactsApi.BASE_URL).create(OpenFoodFactsApi::class.java),
        microsCache = database.productMicrosDao(),
        foodAnalyzer = foodAnalyzer,
    )
    val apiKeyChecker = ApiKeyChecker(geminiApi)

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
