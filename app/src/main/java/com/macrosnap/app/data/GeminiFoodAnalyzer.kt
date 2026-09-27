package com.macrosnap.app.data

import com.macrosnap.app.data.remote.GeminiApi
import com.macrosnap.app.data.remote.GeminiContent
import com.macrosnap.app.data.remote.GeminiPart
import com.macrosnap.app.data.remote.GenerateContentRequest
import com.macrosnap.app.data.remote.GenerateContentResponse
import com.macrosnap.app.data.remote.GenerationConfig
import com.macrosnap.app.data.remote.InlineData
import com.macrosnap.app.data.remote.toDataError
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.Confidence
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.DetectedItem
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.SupplementAnalysis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException

class GeminiFoodAnalyzer(
    private val api: GeminiApi,
    private val settingsStore: SettingsStore,
    private val imageEncoder: ImageEncoder,
    private val json: Json,
) : FoodAnalyzer {

    override suspend fun analyzePhoto(photoPath: String, hint: String?): Outcome<AnalysisResult> {
        // Shrink before anything can bail out: the photo may still be logged with manually
        // entered items, and a full-resolution camera file is far too big to keep per meal.
        val imageBase64 = try {
            imageEncoder.shrinkAndEncode(photoPath)
        } catch (e: IOException) {
            return Outcome.Failure(DataError.BadImage)
        }
        val prompt = buildString {
            append(PHOTO_PROMPT).append("\n\n").append(MICROS_PROMPT)
            if (!hint.isNullOrBlank()) append("\n\nExtra context from the person who ate this: ").append(hint.trim())
        }
        val parts = listOf(
            GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)),
            GeminiPart(text = prompt),
        )
        return generate(parts, FOOD_SCHEMA).flatMap(::parseFood)
    }

    override suspend fun analyzeText(description: String): Outcome<AnalysisResult> {
        if (description.isBlank()) return Outcome.Failure(DataError.NotFood)
        val prompt = TEXT_PROMPT + "\n\n" + MICROS_PROMPT + "\n\nThe user wrote: \"" + description.trim() + "\""
        return generate(listOf(GeminiPart(text = prompt)), FOOD_SCHEMA).flatMap(::parseFood)
    }

    override suspend fun analyzeSupplement(description: String): Outcome<SupplementAnalysis> {
        if (description.isBlank()) return Outcome.Failure(DataError.NotFood)
        val prompt = SUPPLEMENT_PROMPT + "\n\nThe user wrote: \"" + description.trim() + "\""
        return generate(listOf(GeminiPart(text = prompt)), SUPPLEMENT_SCHEMA).flatMap(::parseSupplement)
    }

    /** Runs one structured-output request, falling through the configured models on quota errors. */
    private suspend fun generate(parts: List<GeminiPart>, schema: JsonObject): Outcome<String> {
        val settings = settingsStore.settings.first()
        if (settings.apiKey.isBlank()) return Outcome.Failure(DataError.NoApiKey)

        val request = GenerateContentRequest(
            contents = listOf(GeminiContent(role = "user", parts = parts)),
            generationConfig = GenerationConfig(responseMimeType = "application/json", responseSchema = schema),
        )
        var lastError: DataError = DataError.ModelUnavailable
        var hitMinuteLimit = false
        for (model in settings.models) {
            val outcome = try {
                extractText(api.generateContent(model, settings.apiKey, request))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Outcome.Failure(e.toDataError())
            }
            when (outcome) {
                is Outcome.Success -> return outcome
                is Outcome.Failure -> {
                    lastError = outcome.error
                    if ((outcome.error as? DataError.RateLimited)?.daily == false) hitMinuteLimit = true
                    if (!outcome.error.isWorthTryingNextModel) return outcome
                }
            }
        }
        // If any model was only rate-limited for the minute, waiting helps, so say that rather than "used up today".
        if (lastError is DataError.RateLimited && hitMinuteLimit) lastError = DataError.RateLimited(daily = false)
        return Outcome.Failure(lastError)
    }

    private fun extractText(response: GenerateContentResponse): Outcome<String> {
        val text = response.candidates.firstOrNull()?.content?.parts.orEmpty()
            .filter { it.thought != true }
            .mapNotNull { it.text }
            .joinToString("")
            .trim()
            .removeSurrounding("```json", "```")
            .trim()
        if (text.isEmpty()) {
            val reason = response.promptFeedback?.blockReason ?: response.candidates.firstOrNull()?.finishReason
            return Outcome.Failure(DataError.Unexpected("The model returned no answer (${reason ?: "unknown reason"})"))
        }
        return Outcome.Success(text)
    }

    private fun parseFood(text: String): Outcome<AnalysisResult> {
        val dto = try {
            json.decodeFromString<AnalysisDto>(text)
        } catch (e: SerializationException) {
            return Outcome.Failure(DataError.Unexpected("The model's answer could not be read"))
        }
        if (!dto.isFood || dto.items.isEmpty()) return Outcome.Failure(DataError.NotFood)
        return Outcome.Success(
            AnalysisResult(
                items = dto.items.map { it.toDetectedItem() },
                notes = dto.notes?.takeIf { it.isNotBlank() },
            ),
        )
    }

    private fun parseSupplement(text: String): Outcome<SupplementAnalysis> {
        val dto = try {
            json.decodeFromString<SupplementDto>(text)
        } catch (e: SerializationException) {
            return Outcome.Failure(DataError.Unexpected("The model's answer could not be read"))
        }
        if (!dto.isSupplement || dto.name.isBlank()) return Outcome.Failure(DataError.NotFood)
        return Outcome.Success(
            SupplementAnalysis(
                name = dto.name.trim(),
                doseLabel = dto.doseLabel?.trim().orEmpty(),
                micros = dto.nutrientsPerDose.toMicros(),
                notes = dto.notes?.takeIf { it.isNotBlank() },
            ),
        )
    }

    private inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
        is Outcome.Success -> transform(value)
        is Outcome.Failure -> this
    }

    private val DataError.isWorthTryingNextModel: Boolean
        get() = this is DataError.RateLimited || this is DataError.ModelUnavailable || this is DataError.Server

    private companion object {
        val PHOTO_PROMPT = """
            You are a sports nutritionist estimating macros from a photo of a meal.

            Identify every distinct food and drink in the photo as a separate item. For each item:
            - name: a short, specific name including the cooking method when visible
              (e.g. "grilled chicken breast", "white rice, cooked", "olive oil dressing"). Write names in
              the language the person used in their extra context below; if there is none, use English.
            - grams: your best estimate of the weight of the portion shown, as served. Use plate and
              cutlery size, packaging and typical serving sizes as scale references.
            - per-100 g values: typical calories (kcal), protein, carbohydrates and fat per 100 g of that
              food as prepared, including cooking fat or sauce that is visibly part of it.
            - confidence: how sure you are about both the identification and the portion size.

            List sauces, oils, dressings and drinks separately when they are visible. Do not invent
            foods that are not visible. If the photo shows a nutrition label or packaged product,
            use the label values. If there is no food or drink in the photo, set is_food to false
            and return an empty items list. Use notes for one short sentence about the biggest
            uncertainty in your estimate.
        """.trimIndent()

        val TEXT_PROMPT = """
            You are a sports nutritionist. The user lists foods or recipe ingredients in plain words,
            usually with quantities, for example "50 g olive oil, 2 eggs, a cup of cooked rice".

            The text may be in any language, often English or Bulgarian. Bulgarian shorthand: "г"/"гр" = grams,
            "мл" = ml, "с.л." = tablespoon, "ч.л." = teaspoon, "бр." = pieces, "чаша" = cup, "филия" = slice,
            "шепа" = handful, "пакет" = packet. Decimal commas ("0,5") are decimals.

            Turn each distinct food into one item:
            - name: a short, specific name in the same language the user wrote in (Bulgarian stays
              Bulgarian). Keep the user's wording where it is already specific.
            - grams: the weight of the stated quantity. Convert units (tbsp, tsp, cups, ml, slices,
              pieces, eggs, cans) to grams using typical sizes. If no quantity is given, use a typical
              single serving.
            - per-100 g values: typical calories (kcal), protein, carbohydrates and fat per 100 g of
              that food as described (raw unless the user says it is cooked).
            - confidence: how sure you are about the identification and the weight.

            Do not add foods the user did not mention. If the text does not describe any food or
            drink, set is_food to false and return an empty items list. Use notes for one short
            sentence about any size you had to assume.
        """.trimIndent()

        val MICROS_PROMPT = """
            Also fill nutrients_per_100g for each item with typical values per 100 g from food
            composition tables: vitamin D (µg), vitamin B12 (µg), vitamin B6 (mg), vitamin C (mg), vitamin A (µg RAE),
            iron (mg), magnesium (mg), zinc (mg), calcium (mg), potassium (mg), omega-3 fatty acids
            (g, EPA+DHA+ALA), creatine (g; only meat and fish contain it, roughly 0.3-0.5 g per 100 g
            raw) and caffeine (mg). Use 0 for nutrients the food essentially lacks. Round sensibly.
        """.trimIndent()

        val SUPPLEMENT_PROMPT = """
            You are a sports nutritionist. The user describes a dietary supplement and, usually, its
            dose, for example "vitamin D3 2000 IU", "creatine monohydrate 5 g", "fish oil 1000 mg
            softgel", "magnesium glycinate 400 mg", "a multivitamin".

            The text may be in English or Bulgarian; answer in the language the user wrote in.

            Return:
            - name: a short product-style name (e.g. "Vitamin D3", "Creatine monohydrate", "Fish oil").
            - dose_label: how one dose is described, e.g. "2000 IU", "5 g", "1 softgel", "1 tablet".
            - nutrients_per_dose: the nutrient content of ONE dose, in these units: vitamin D µg
              (1 µg = 40 IU), vitamin B12 µg, vitamin B6 mg, vitamin C mg, vitamin A µg RAE (1 IU = 0.3 µg retinol),
              iron mg, magnesium mg (elemental magnesium, not the compound weight), zinc mg (elemental),
              calcium mg (elemental), potassium mg, omega-3 g (EPA + DHA), creatine g, caffeine mg.
              Fill only what the supplement actually contains; use 0 otherwise. For a generic
              multivitamin with no brand, use typical one-a-day amounts.
            - is_supplement: false if the text does not describe a supplement.
            - notes: one short sentence about any assumption (e.g. brand-typical dose you assumed).
        """.trimIndent()

        val NUTRIENTS_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                Nutrient.entries.forEach { putJsonObject(it.schemaKey) { put("type", "NUMBER") } }
            }
        }

        val FOOD_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("is_food") { put("type", "BOOLEAN") }
                putJsonObject("notes") { put("type", "STRING") }
                putJsonObject("items") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("name") { put("type", "STRING") }
                            putJsonObject("grams") { put("type", "NUMBER") }
                            putJsonObject("kcal_per_100g") { put("type", "NUMBER") }
                            putJsonObject("protein_per_100g") { put("type", "NUMBER") }
                            putJsonObject("carbs_per_100g") { put("type", "NUMBER") }
                            putJsonObject("fat_per_100g") { put("type", "NUMBER") }
                            putJsonObject("confidence") {
                                put("type", "STRING")
                                putJsonArray("enum") { add("low"); add("medium"); add("high") }
                            }
                            put("nutrients_per_100g", NUTRIENTS_SCHEMA)
                        }
                        putJsonArray("required") {
                            add("name"); add("grams"); add("kcal_per_100g"); add("protein_per_100g")
                            add("carbs_per_100g"); add("fat_per_100g"); add("confidence"); add("nutrients_per_100g")
                        }
                    }
                }
            }
            putJsonArray("required") { add("is_food"); add("items") }
        }

        val SUPPLEMENT_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("is_supplement") { put("type", "BOOLEAN") }
                putJsonObject("name") { put("type", "STRING") }
                putJsonObject("dose_label") { put("type", "STRING") }
                put("nutrients_per_dose", NUTRIENTS_SCHEMA)
                putJsonObject("notes") { put("type", "STRING") }
            }
            putJsonArray("required") { add("is_supplement"); add("name"); add("dose_label"); add("nutrients_per_dose") }
        }
    }
}

private fun Map<String, Double>?.toMicros(): Micros {
    if (this == null) return emptyMap()
    return Nutrient.entries.mapNotNull { nutrient ->
        this[nutrient.schemaKey]?.takeIf { it.isFinite() && it >= 0 }?.let { nutrient to it }
    }.toMap()
}

@Serializable
private data class AnalysisDto(
    @SerialName("is_food") val isFood: Boolean = true,
    val items: List<AnalysisItemDto> = emptyList(),
    val notes: String? = null,
)

@Serializable
private data class AnalysisItemDto(
    val name: String,
    val grams: Double,
    @SerialName("kcal_per_100g") val kcalPer100g: Double,
    @SerialName("protein_per_100g") val proteinPer100g: Double = 0.0,
    @SerialName("carbs_per_100g") val carbsPer100g: Double = 0.0,
    @SerialName("fat_per_100g") val fatPer100g: Double = 0.0,
    val confidence: String? = null,
    @SerialName("nutrients_per_100g") val nutrients: Map<String, Double>? = null,
) {
    // Pure fat tops out at 900 kcal/100 g; anything beyond these bounds is a model slip.
    fun toDetectedItem() = DetectedItem(
        item = FoodItem(
            name = name.trim().replaceFirstChar { it.uppercase() },
            grams = grams.coerceIn(1.0, 5000.0),
            per100g = Macros(
                kcal = kcalPer100g.coerceIn(0.0, 900.0),
                protein = proteinPer100g.coerceIn(0.0, 100.0),
                carbs = carbsPer100g.coerceIn(0.0, 100.0),
                fat = fatPer100g.coerceIn(0.0, 100.0),
            ),
            micros100g = nutrients.toMicros(),
        ),
        confidence = when (confidence?.lowercase()) {
            "high" -> Confidence.HIGH
            "low" -> Confidence.LOW
            else -> Confidence.MEDIUM
        },
    )
}

@Serializable
private data class SupplementDto(
    @SerialName("is_supplement") val isSupplement: Boolean = true,
    val name: String = "",
    @SerialName("dose_label") val doseLabel: String? = null,
    @SerialName("nutrients_per_dose") val nutrientsPerDose: Map<String, Double>? = null,
    val notes: String? = null,
)
