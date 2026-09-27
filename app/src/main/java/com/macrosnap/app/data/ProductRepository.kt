package com.macrosnap.app.data

import com.macrosnap.app.data.remote.OffProduct
import com.macrosnap.app.data.remote.OpenFoodFactsApi
import com.macrosnap.app.data.remote.toDataError
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ProductRepository(private val api: OpenFoodFactsApi) {

    suspend fun lookup(barcode: String): Outcome<FoodItem> {
        val response = try {
            api.product(barcode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = e.toDataError()
            // Here a 404 means "no such barcode", not a missing model.
            return Outcome.Failure(if (error is DataError.ModelUnavailable) DataError.ProductNotFound else error)
        }
        val item = response.product?.takeIf { response.status == 1 }?.toFoodItem()
            ?: return Outcome.Failure(DataError.ProductNotFound)
        return Outcome.Success(item)
    }

    private fun OffProduct.toFoodItem(): FoodItem? {
        val nutriments = nutriments ?: return null
        val kcal = nutriments["energy-kcal_100g"].asDouble()
            ?: nutriments["energy_100g"].asDouble()?.let { it / KJ_PER_KCAL }
            ?: return null
        val name = listOfNotNull(
            productName?.takeIf { it.isNotBlank() },
            brands?.substringBefore(',')?.trim()?.takeIf { it.isNotBlank() },
        ).joinToString(" - ").ifEmpty { "Scanned product" }

        return FoodItem(
            name = name,
            grams = servingQuantity.asDouble()?.takeIf { it > 0 } ?: 100.0,
            per100g = Macros(
                kcal = kcal,
                protein = nutriments["proteins_100g"].asDouble() ?: 0.0,
                carbs = nutriments["carbohydrates_100g"].asDouble() ?: 0.0,
                fat = nutriments["fat_100g"].asDouble() ?: 0.0,
            ),
            micros100g = nutriments.toMicros(),
        )
    }

    // Open Food Facts stores every `_100g` nutrient in grams; scale into the unit the app uses.
    private fun JsonObject.toMicros(): Micros = Nutrient.entries.mapNotNull { nutrient ->
        val key = nutrient.offKey ?: return@mapNotNull null
        this["${key}_100g"].asDouble()?.takeIf { it >= 0 }?.let { nutrient to it * nutrient.offScale }
    }.toMap()

    private fun JsonElement?.asDouble(): Double? = (this as? JsonPrimitive)?.content?.toDoubleOrNull()

    private companion object {
        const val KJ_PER_KCAL = 4.184
    }
}
