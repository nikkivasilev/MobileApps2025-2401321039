package com.macrosnap.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest,
    ): GenerateContentResponse

    @GET("v1beta/models")
    suspend fun listModels(
        @Header("x-goog-api-key") apiKey: String,
        @Query("pageSize") pageSize: Int = 1000,
    ): ListModelsResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }
}

@Serializable
data class GenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig,
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
    val role: String? = null,
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val thought: Boolean? = null,
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String,
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String,
    val responseSchema: JsonObject,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

@Serializable
data class PromptFeedback(val blockReason: String? = null)

@Serializable
data class ListModelsResponse(val models: List<GeminiModel> = emptyList())

@Serializable
data class GeminiModel(
    val name: String,
    val supportedGenerationMethods: List<String> = emptyList(),
)
