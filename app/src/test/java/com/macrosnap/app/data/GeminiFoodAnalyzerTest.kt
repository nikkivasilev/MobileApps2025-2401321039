package com.macrosnap.app.data

import com.macrosnap.app.data.remote.GeminiApi
import com.macrosnap.app.data.remote.GeminiCandidate
import com.macrosnap.app.data.remote.GeminiContent
import com.macrosnap.app.data.remote.GeminiPart
import com.macrosnap.app.data.remote.GenerateContentRequest
import com.macrosnap.app.data.remote.GenerateContentResponse
import com.macrosnap.app.data.remote.ListModelsResponse
import com.macrosnap.app.data.remote.PromptFeedback
import com.macrosnap.app.data.settings.AppSettings
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.Goals
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.SupplementAnalysis
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class GeminiFoodAnalyzerTest {

    private data class Call(
        val model: String,
        val apiKey: String,
        val request: GenerateContentRequest,
    )

    /** Answers per model; a model with no entry answers with [default]. */
    private class FakeGeminiApi(
        private val default: () -> GenerateContentResponse = { error("no answer configured") },
        private val perModel: Map<String, () -> GenerateContentResponse> = emptyMap(),
    ) : GeminiApi {
        val calls = mutableListOf<Call>()

        override suspend fun generateContent(
            model: String,
            apiKey: String,
            request: GenerateContentRequest,
        ): GenerateContentResponse {
            calls += Call(model, apiKey, request)
            return (perModel[model] ?: default)()
        }

        override suspend fun listModels(apiKey: String, pageSize: Int): ListModelsResponse = error("unused")
    }

    private fun settings(apiKey: String = "test-key", models: List<String> = MODELS) = AppSettings(
        apiKey = apiKey,
        models = models,
        goals = Goals.DEFAULT,
        profile = null,
        nutrientOverrides = emptyMap(),
        waterGoalMl = AppSettings.DEFAULT_WATER_GOAL_ML,
        sleepGoalHours = AppSettings.DEFAULT_SLEEP_GOAL_HOURS,
        restSeconds = AppSettings.DEFAULT_REST_SECONDS,
        addWorkoutBurn = false,
    )

    private fun analyzer(api: FakeGeminiApi, settings: AppSettings = settings()) = GeminiFoodAnalyzer(
        api = api,
        settings = flowOf(settings),
        imageEncoder = ImageEncoder(Dispatchers.Unconfined),
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
        uiLanguage = { "English" },
    )

    private fun parts(vararg parts: GeminiPart) = GenerateContentResponse(
        candidates = listOf(GeminiCandidate(content = GeminiContent(parts = parts.toList(), role = "model"))),
    )

    private fun answer(text: String) = parts(GeminiPart(text = text))

    private fun httpError(code: Int, body: String = "{}"): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody()))

    private fun Call.prompt(): String = request.contents.single().parts.mapNotNull { it.text }.single()

    private fun <T> Outcome<T>.success(): T = (this as? Outcome.Success)?.value ?: error("Expected success, got $this")

    private fun Outcome<*>.failure(): DataError =
        (this as? Outcome.Failure)?.error ?: error("Expected failure, got $this")

    // Text / describe

    @Test
    fun `describe answer becomes detected items with micros and confidence`() = runTest {
        val api = FakeGeminiApi(
            default = {
                answer(
                    """
                    {"is_food": true, "notes": "Assumed large eggs.", "items": [
                      {"name": "  boiled eggs ", "grams": 120, "kcal_per_100g": 155, "protein_per_100g": 12.6,
                       "carbs_per_100g": 1.1, "fat_per_100g": 10.6, "confidence": "HIGH",
                       "nutrients_per_100g": {"vitamin_d_ug": 2.0, "vitamin_b12_ug": 1.1, "iron_mg": -1,
                                              "made_up_mg": 5, "caffeine_mg": 0}},
                      {"name": "сирене", "grams": 50, "kcal_per_100g": 260, "confidence": "low"},
                      {"name": "toast", "grams": 30, "kcal_per_100g": 265, "protein_per_100g": 9,
                       "carbs_per_100g": 49, "fat_per_100g": 3.2, "confidence": "somewhat"}
                    ]}
                    """.trimIndent(),
                )
            },
        )

        val result: AnalysisResult = analyzer(api).analyzeText("  2 eggs, 50 гр сирене, toast ").success()

        assertEquals("Assumed large eggs.", result.notes)
        assertEquals(listOf("Boiled eggs", "Сирене", "Toast"), result.items.map { it.item.name })
        assertEquals(listOf(Confidence.HIGH, Confidence.LOW, Confidence.MEDIUM), result.items.map { it.confidence })

        val eggs = result.items[0].item
        assertEquals(120.0, eggs.grams, 0.0)
        assertEquals(155.0, eggs.per100g.kcal, 0.0)
        assertEquals(12.6, eggs.per100g.protein, 0.0)
        assertEquals(1.1, eggs.per100g.carbs, 0.0)
        assertEquals(10.6, eggs.per100g.fat, 0.0)
        // Negative and unknown nutrients are dropped; zero is a real value, not "unknown".
        assertEquals(
            mapOf(Nutrient.VITAMIN_D to 2.0, Nutrient.VITAMIN_B12 to 1.1, Nutrient.CAFFEINE to 0.0),
            eggs.micros100g,
        )

        // Macros the model left out default to zero, and so do the micros it didn't send.
        val cheese = result.items[1].item
        assertEquals(0.0, cheese.per100g.protein + cheese.per100g.carbs + cheese.per100g.fat, 0.0)
        assertTrue(cheese.micros100g.isEmpty())

        val call = api.calls.single()
        assertEquals(MODELS.first(), call.model)
        assertEquals("test-key", call.apiKey)
        assertEquals("user", call.request.contents.single().role)
        assertTrue(call.prompt().endsWith("The user wrote: \"2 eggs, 50 гр сирене, toast\""))
        assertEquals("application/json", call.request.generationConfig.responseMimeType)
        val schemaKeys = call.request.generationConfig.responseSchema.getValue("properties").toString()
        assertTrue("is_food" in schemaKeys && "items" in schemaKeys)
    }

    @Test
    fun `impossible estimates are clamped`() = runTest {
        val api = FakeGeminiApi(
            default = {
                answer(
                    """
                    {"is_food": true, "items": [
                      {"name": "lard", "grams": 9000, "kcal_per_100g": 1500, "protein_per_100g": -3,
                       "carbs_per_100g": 140, "fat_per_100g": 120, "confidence": "medium"},
                      {"name": "crumb", "grams": 0, "kcal_per_100g": -20, "protein_per_100g": 101,
                       "carbs_per_100g": -1, "fat_per_100g": -0.5, "confidence": "medium"}
                    ]}
                    """.trimIndent(),
                )
            },
        )

        val (lard, crumb) = analyzer(api).analyzeText("lard and a crumb").success().items.map { it.item }

        assertEquals(5000.0, lard.grams, 0.0)
        assertEquals(900.0, lard.per100g.kcal, 0.0)
        assertEquals(0.0, lard.per100g.protein, 0.0)
        assertEquals(100.0, lard.per100g.carbs, 0.0)
        assertEquals(100.0, lard.per100g.fat, 0.0)

        assertEquals(1.0, crumb.grams, 0.0)
        assertEquals(0.0, crumb.per100g.kcal, 0.0)
        assertEquals(100.0, crumb.per100g.protein, 0.0)
        assertEquals(0.0, crumb.per100g.carbs, 0.0)
        assertEquals(0.0, crumb.per100g.fat, 0.0)
    }

    @Test
    fun `blank notes are dropped`() = runTest {
        val api = FakeGeminiApi(
            default = {
                answer("""{"is_food": true, "notes": "  ", "items": [$APPLE_ITEM]}""")
            },
        )

        assertNull(analyzer(api).analyzeText("an apple").success().notes)
    }

    @Test
    fun `an answer that isn't food is NotFood`() = runTest {
        val notFood = FakeGeminiApi(default = { answer("""{"is_food": false, "items": []}""") })
        val noItems = FakeGeminiApi(default = { answer("""{"is_food": true, "items": []}""") })
        // Items listed despite is_food = false are still not trusted.
        val contradictory = FakeGeminiApi(
            default = {
                answer("""{"is_food": false, "items": [{"name": "rock", "grams": 100, "kcal_per_100g": 0}]}""")
            },
        )

        assertEquals(DataError.NotFood, analyzer(notFood).analyzeText("a chair").failure())
        assertEquals(DataError.NotFood, analyzer(noItems).analyzeText("nothing").failure())
        assertEquals(DataError.NotFood, analyzer(contradictory).analyzeText("a rock").failure())
    }

    @Test
    fun `blank description is NotFood without asking the model`() = runTest {
        val api = FakeGeminiApi()

        assertEquals(DataError.NotFood, analyzer(api).analyzeText(" \n ").failure())
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun `unreadable answer is Unexpected`() = runTest {
        val garbage = FakeGeminiApi(default = { answer("Sure! Here are your macros: lots.") })
        // Valid JSON, but an item without its required weight.
        val missingField = FakeGeminiApi(
            default = { answer("""{"is_food": true, "items": [{"name": "apple", "kcal_per_100g": 52}]}""") },
        )
        val expected = DataError.Unexpected("The model's answer could not be read")

        assertEquals(expected, analyzer(garbage).analyzeText("apple").failure())
        assertEquals(expected, analyzer(missingField).analyzeText("apple").failure())
        // A readable reply that can't be parsed won't improve on another model.
        assertEquals(1, garbage.calls.size)
    }

    @Test
    fun `json code fence around the answer is stripped`() = runTest {
        val api = FakeGeminiApi(
            default = {
                answer(
                    "\n```json\n" +
                        """{"is_food": true, "items": [{"name": "rice", "grams": 200, "kcal_per_100g": 130}]}""" +
                        "\n```\n",
                )
            },
        )

        assertEquals("Rice", analyzer(api).analyzeText("rice").success().items.single().item.name)
    }

    @Test
    fun `thought parts are ignored and text parts are joined`() = runTest {
        val api = FakeGeminiApi(
            default = {
                parts(
                    GeminiPart(text = "Let me think about the rice {not json", thought = true),
                    GeminiPart(text = """{"is_food": true, "items": [{"name": "rice", """),
                    GeminiPart(text = """"grams": 200, "kcal_per_100g": 130}]}""", thought = false),
                )
            },
        )

        val item = analyzer(api).analyzeText("rice").success().items.single().item
        assertEquals("Rice", item.name)
        assertEquals(200.0, item.grams, 0.0)
    }

    @Test
    fun `empty answer reports why the model gave none`() = runTest {
        val blocked = FakeGeminiApi(
            default = { GenerateContentResponse(promptFeedback = PromptFeedback(blockReason = "SAFETY")) },
        )
        val finished = FakeGeminiApi(
            default = {
                GenerateContentResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiContent(parts = listOf(GeminiPart(text = "thinking", thought = true))),
                            finishReason = "MAX_TOKENS",
                        ),
                    ),
                )
            },
        )
        val silent = FakeGeminiApi(default = { parts(GeminiPart(text = "  ")) })

        assertEquals(
            DataError.Unexpected("The model returned no answer (SAFETY)"),
            analyzer(blocked).analyzeText("x").failure(),
        )
        assertEquals(
            DataError.Unexpected("The model returned no answer (MAX_TOKENS)"),
            analyzer(finished).analyzeText("x").failure(),
        )
        assertEquals(
            DataError.Unexpected("The model returned no answer (unknown reason)"),
            analyzer(silent).analyzeText("x").failure(),
        )
        assertEquals(1, blocked.calls.size)
    }

    // Model fallback and errors

    @Test
    fun `falls through to the next model on rate limit, missing model and server errors`() = runTest {
        val api = FakeGeminiApi(
            default = { answer(APPLE) },
            perModel = mapOf(
                "flash" to { throw httpError(429, "GenerateRequestsPerMinutePerProjectPerModel-FreeTier") },
                "lite" to { throw httpError(404) },
                "lite-2" to { throw httpError(503) },
            ),
        )
        val settings = settings(models = listOf("flash", "lite", "lite-2", "lite-3"))

        val result = analyzer(api, settings).analyzeText("apple").success()

        assertEquals("Apple", result.items.single().item.name)
        assertEquals(listOf("flash", "lite", "lite-2", "lite-3"), api.calls.map { it.model })
        assertTrue(api.calls.all { it.apiKey == "test-key" })
    }

    @Test
    fun `stops at the first model on a bad API key or a network failure`() = runTest {
        val unauthorized = FakeGeminiApi(default = { throw httpError(403) })
        // Gemini reports a bad key as 400.
        val badRequest = FakeGeminiApi(default = { throw httpError(400, """{"message": "API key not valid"}""") })
        val offline = FakeGeminiApi(default = { throw IOException("no route to host") })

        assertEquals(DataError.InvalidApiKey, analyzer(unauthorized).analyzeText("apple").failure())
        assertEquals(DataError.InvalidApiKey, analyzer(badRequest).analyzeText("apple").failure())
        assertEquals(DataError.Network, analyzer(offline).analyzeText("apple").failure())
        assertEquals(listOf(MODELS.first()), unauthorized.calls.map { it.model })
        assertEquals(1, badRequest.calls.size)
        assertEquals(1, offline.calls.size)
    }

    @Test
    fun `any other failure is Unexpected with its message`() = runTest {
        val api = FakeGeminiApi(default = { throw IllegalStateException("boom") })

        assertEquals(DataError.Unexpected("boom"), analyzer(api).analyzeText("apple").failure())
        assertEquals(1, api.calls.size)
    }

    @Test
    fun `cancellation is not turned into an error`() = runTest {
        val api = FakeGeminiApi(default = { throw CancellationException("left the screen") })

        try {
            analyzer(api).analyzeText("apple")
            fail("Cancellation should propagate")
        } catch (expected: CancellationException) {
            assertEquals("left the screen", expected.message)
        }
        assertEquals(1, api.calls.size)
    }

    @Test
    fun `every model out for the day reads as a daily limit`() = runTest {
        val api = FakeGeminiApi(default = { throw httpError(429, DAILY_QUOTA) })

        assertEquals(DataError.RateLimited(daily = true), analyzer(api).analyzeText("apple").failure())
        assertEquals(MODELS, api.calls.map { it.model })
    }

    @Test
    fun `one model limited only for the minute reads as a minute limit`() = runTest {
        val api = FakeGeminiApi(
            default = { throw httpError(429, DAILY_QUOTA) },
            perModel = mapOf(MODELS[1] to { throw httpError(429, "GenerateRequestsPerMinutePerProjectPerModel") }),
        )

        // The last model was out for the day, but waiting a minute would let the second one answer.
        assertEquals(DataError.RateLimited(daily = false), analyzer(api).analyzeText("apple").failure())
        assertEquals(MODELS, api.calls.map { it.model })
    }

    @Test
    fun `when the models run out the last model's error is reported`() = runTest {
        val api = FakeGeminiApi(
            default = { throw httpError(429, "PerMinute") },
            perModel = mapOf(MODELS.last() to { throw httpError(500) }),
        )

        assertEquals(DataError.Server, analyzer(api).analyzeText("apple").failure())
    }

    @Test
    fun `no configured models is ModelUnavailable`() = runTest {
        val api = FakeGeminiApi()

        val result = analyzer(api, settings(models = emptyList())).analyzeText("x")

        assertEquals(DataError.ModelUnavailable, result.failure())
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun `missing API key is NoApiKey on every path without a request`() = runTest {
        val api = FakeGeminiApi()
        val analyzer = analyzer(api, settings(apiKey = " "))

        assertEquals(DataError.NoApiKey, analyzer.analyzeText("apple").failure())
        assertEquals(DataError.NoApiKey, analyzer.analyzeSupplement("vitamin D").failure())
        assertEquals(DataError.NoApiKey, analyzer.estimateProductMicros("Cola", null, null).failure())
        assertTrue(api.calls.isEmpty())
    }

    // Supplements

    @Test
    fun `supplement answer becomes a trimmed analysis with nutrients per dose`() = runTest {
        val api = FakeGeminiApi(
            default = {
                answer(
                    """
                    {"is_supplement": true, "name": " Vitamin D3 ", "dose_label": " 2000 IU ",
                     "nutrients_per_dose": {"vitamin_d_ug": 50, "omega3_g": -1, "unknown": 3},
                     "notes": "Typical softgel."}
                    """.trimIndent(),
                )
            },
        )

        val result: SupplementAnalysis = analyzer(api).analyzeSupplement(" vitamin D3 2000 IU ").success()

        assertEquals(
            SupplementAnalysis("Vitamin D3", "2000 IU", mapOf(Nutrient.VITAMIN_D to 50.0), "Typical softgel."),
            result,
        )
        val call = api.calls.single()
        assertTrue(call.prompt().endsWith("The user wrote: \"vitamin D3 2000 IU\""))
        val schemaKeys = call.request.generationConfig.responseSchema.getValue("properties").toString()
        assertTrue("nutrients_per_dose" in schemaKeys)
    }

    @Test
    fun `supplement without dose, nutrients or notes gets empty values`() = runTest {
        val api = FakeGeminiApi(default = { answer("""{"is_supplement": true, "name": "Creatine", "notes": ""}""") })

        assertEquals(
            SupplementAnalysis("Creatine", "", emptyMap(), null),
            analyzer(api).analyzeSupplement("creatine").success(),
        )
    }

    @Test
    fun `a supplement answer without a supplement is NotFood`() = runTest {
        val notSupplement = FakeGeminiApi(default = { answer("""{"is_supplement": false, "name": "Pizza"}""") })
        val noName = FakeGeminiApi(default = { answer("""{"is_supplement": true, "name": " "}""") })
        val idle = FakeGeminiApi()

        assertEquals(DataError.NotFood, analyzer(notSupplement).analyzeSupplement("pizza").failure())
        assertEquals(DataError.NotFood, analyzer(noName).analyzeSupplement("???").failure())
        assertEquals(DataError.NotFood, analyzer(idle).analyzeSupplement("  ").failure())
        assertTrue(idle.calls.isEmpty())
    }

    @Test
    fun `unreadable supplement answer is Unexpected`() = runTest {
        val api = FakeGeminiApi(default = { answer("""{"name": ["not", "a", "string"]}""") })

        assertEquals(
            DataError.Unexpected("The model's answer could not be read"),
            analyzer(api).analyzeSupplement("fish oil").failure(),
        )
    }

    // Product micros

    @Test
    fun `product micros are parsed and the prompt names the product`() = runTest {
        val api = FakeGeminiApi(
            default = {
                answer("""{"nutrients_per_100g": {"vitamin_b6_mg": 0.4, "caffeine_mg": 32, "zinc_mg": -2}}""")
            },
        )

        val micros: Micros = analyzer(api).estimateProductMicros(" Red Bull Zero ", " Red Bull ", " 250 ml ").success()

        assertEquals(mapOf(Nutrient.VITAMIN_B6 to 0.4, Nutrient.CAFFEINE to 32.0), micros)
        val prompt = api.calls.single().prompt()
        assertTrue(prompt.endsWith("Product name: \"Red Bull Zero\"\nBrand: Red Bull\nPackage size: 250 ml"))
        val schema = api.calls.single().request.generationConfig.responseSchema
        val required = schema.getValue("required").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("nutrients_per_100g"), required)
        assertEquals("OBJECT", schema.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `product prompt leaves out a blank brand and size`() = runTest {
        val api = FakeGeminiApi(default = { answer("{}") })

        val micros = analyzer(api).estimateProductMicros("Ayran", "  ", null).success()

        // No nutrients at all is an empty estimate, not an error.
        assertTrue(micros.isEmpty())
        val prompt = api.calls.single().prompt()
        assertTrue(prompt.endsWith("Product name: \"Ayran\""))
        assertFalse("Brand:" in prompt)
        assertFalse("Package size:" in prompt)
    }

    @Test
    fun `unreadable product micros answer is Unexpected`() = runTest {
        val api = FakeGeminiApi(default = { answer("""{"nutrients_per_100g": {"iron_mg": "lots"}}""") })

        assertEquals(
            DataError.Unexpected("The model's answer could not be read"),
            analyzer(api).estimateProductMicros("Cereal", null, null).failure(),
        )
    }

    private companion object {
        val MODELS = listOf("model-a", "model-b", "model-c")
        const val DAILY_QUOTA = "Quota exceeded: GenerateRequestsPerDayPerProjectPerModel-FreeTier"
        const val APPLE_ITEM = """{"name": "apple", "grams": 150, "kcal_per_100g": 52}"""
        const val APPLE = """{"is_food": true, "items": [$APPLE_ITEM]}"""
    }
}
