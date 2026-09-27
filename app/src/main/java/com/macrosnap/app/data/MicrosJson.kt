package com.macrosnap.app.data

import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Micros travel through Room and DataStore as a small JSON object keyed by [Nutrient.name]. */
object MicrosJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(micros: Micros): String? =
        if (micros.isEmpty()) null else json.encodeToString(micros.mapKeys { it.key.name })

    fun decode(text: String?): Micros {
        if (text.isNullOrBlank()) return emptyMap()
        val raw = try {
            json.decodeFromString<Map<String, Double>>(text)
        } catch (e: SerializationException) {
            return emptyMap()
        } catch (e: IllegalArgumentException) {
            return emptyMap()
        }
        // Unknown names are skipped so an older build can still read data written by a newer one.
        return raw.entries.mapNotNull { (name, amount) ->
            Nutrient.entries.firstOrNull { it.name == name }?.let { it to amount }
        }.toMap()
    }
}
