package com.macrosnap.app.data

import com.macrosnap.app.data.remote.GeminiApi
import com.macrosnap.app.data.remote.toDataError
import com.macrosnap.app.model.Outcome
import kotlinx.coroutines.CancellationException

class ApiKeyChecker(private val api: GeminiApi) {

    /** Succeeds with the configured models the key can NOT use (empty means all good). */
    suspend fun check(apiKey: String, configuredModels: List<String>): Outcome<List<String>> = try {
        val available = api.listModels(apiKey).models
            .filter { "generateContent" in it.supportedGenerationMethods }
            .map { it.name.removePrefix("models/") }
            .toSet()
        Outcome.Success(configuredModels.filterNot { it in available })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Outcome.Failure(e.toDataError())
    }
}
