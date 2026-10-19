package com.macrosnap.app.data

import com.macrosnap.app.R
import com.macrosnap.app.data.local.ProductMicrosDao
import com.macrosnap.app.data.local.ProductMicrosEntity
import com.macrosnap.app.data.remote.OffProduct
import com.macrosnap.app.data.remote.OpenFoodFactsApi
import com.macrosnap.app.data.remote.toDataError
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.ui.UiText
import com.macrosnap.app.ui.uiText
import com.macrosnap.app.ui.whole
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A barcode's label values, plus what about them looked off and which nutrients the listing lacks. */
data class ScannedProduct(
    val barcode: String,
    val item: FoodItem,
    val productName: String?,
    val brand: String?,
    val quantity: String?,
    /** Plain-language problems with the listing, for the user to check against the label. */
    val warnings: List<UiText>,
    val missingMicros: Set<Nutrient>,
)

class ProductRepository(
    private val api: OpenFoodFactsApi,
    private val microsCache: ProductMicrosDao,
    private val foodAnalyzer: FoodAnalyzer,
) {

    @Suppress("TooGenericExceptionCaught") // Any failure is reported to the user as a DataError.
    suspend fun lookup(barcode: String): Outcome<ScannedProduct> {
        val response = try {
            api.product(barcode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = e.toDataError()
            // Here a 404 means "no such barcode", not a missing model.
            return Outcome.Failure(if (error is DataError.ModelUnavailable) DataError.ProductNotFound else error)
        }
        val product = response.product?.takeIf { response.status == 1 }?.toScannedProduct(barcode)
            ?: return Outcome.Failure(DataError.ProductNotFound)
        return Outcome.Success(product)
    }

    /**
     * Estimates the nutrients [ScannedProduct.missingMicros] from the product's name, remembering the
     * answer per barcode. Returns only the missing ones, so label values always win.
     */
    suspend fun estimateMissingMicros(product: ScannedProduct): Outcome<Micros> {
        val name = product.productName ?: return Outcome.Failure(
            DataError.Unexpected("The listing has no product name to go on."),
        )
        val cached = microsCache.get(product.barcode)
        val estimate = if (cached != null) {
            MicrosJson.decode(cached.micros)
        } else {
            when (val outcome = foodAnalyzer.estimateProductMicros(name, product.brand, product.quantity)) {
                is Outcome.Failure -> return outcome
                is Outcome.Success -> outcome.value.also { micros ->
                    microsCache.upsert(
                        ProductMicrosEntity(product.barcode, MicrosJson.encode(micros), System.currentTimeMillis()),
                    )
                }
            }
        }
        return Outcome.Success(estimate.filterKeys { it in product.missingMicros })
    }

    private fun OffProduct.toScannedProduct(barcode: String): ScannedProduct? {
        val nutriments = nutriments ?: return null
        val warnings = mutableListOf<UiText>()

        val protein = nutriments["proteins_100g"].asDouble()
        val carbs = nutriments["carbohydrates_100g"].asDouble()
        val fat = nutriments["fat_100g"].asDouble()
        val kcal = checkedKcal(nutriments, protein, carbs, fat, warnings) ?: return null

        val grams = servingQuantity.asDouble()?.takeIf { it > 0 }.let { serving ->
            when {
                serving == null -> 100.0
                serving > MAX_PLAUSIBLE_SERVING -> {
                    warnings += uiText(R.string.review_warning_serving, serving.whole())
                    100.0
                }
                else -> serving
            }
        }
        // A listing whose per-serving energy disagrees with per-100 g × serving has one of them wrong.
        val kcalPerServing = nutriments["energy-kcal_serving"].asDouble()
        if (kcalPerServing != null && grams != 100.0) {
            val implied = kcal * grams / 100
            if (abs(
                    implied - kcalPerServing,
                ) > maxOf(SERVING_TOLERANCE_KCAL, kcalPerServing * MAX_RELATIVE_DISAGREEMENT)
            ) {
                warnings += uiText(R.string.review_warning_serving_kcal, kcalPerServing.whole(), implied.whole())
            }
        }

        val trimmedName = productName?.trim()?.takeIf { it.isNotEmpty() }
        val brand = brands?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
        val micros = nutriments.toMicros()
        return ScannedProduct(
            barcode = barcode,
            item = FoodItem(
                name = listOfNotNull(trimmedName, brand).joinToString(" - ").ifEmpty { "Scanned product" },
                grams = grams,
                per100g = Macros(kcal = kcal, protein = protein ?: 0.0, carbs = carbs ?: 0.0, fat = fat ?: 0.0),
                micros100g = micros,
            ),
            productName = trimmedName,
            brand = brand,
            quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
            warnings = warnings,
            missingMicros = Nutrient.entries.toSet() - micros.keys,
        )
    }

    /**
     * Picks kcal per 100 g from a crowd-sourced listing, repairing the common kJ-typed-as-kcal slip
     * and flagging values the macros can't account for. Null when there's no energy at all.
     */
    private fun checkedKcal(
        nutriments: JsonObject,
        protein: Double?,
        carbs: Double?,
        fat: Double?,
        warnings: MutableList<UiText>,
    ): Double? {
        // Open Food Facts normalises the generic energy field to kJ.
        val kj = (nutriments["energy-kj_100g"] ?: nutriments["energy_100g"]).asDouble()?.takeIf { it > 0 }
        val listed = nutriments["energy-kcal_100g"].asDouble()
        // Protein and carbs give ~4 kcal/g and fat ~9; fibre, sweeteners and alcohol explain small gaps, not big ones.
        val fromMacros = if (protein != null && carbs != null && fat != null) {
            protein * KCAL_PER_G_PROTEIN + carbs * KCAL_PER_G_CARBS + fat * KCAL_PER_G_FAT
        } else {
            null
        }
        val warningsBefore = warnings.size

        var kcal = listedKcal(listed, kj, fromMacros, warnings) ?: return null
        if (kcal > MAX_KCAL_PER_100G) {
            warnings += uiText(R.string.review_warning_impossible_kcal, kcal.whole())
            kcal = fromMacros?.takeIf { it <= MAX_KCAL_PER_100G } ?: MAX_KCAL_PER_100G
        }
        if (fromMacros != null &&
            warnings.size == warningsBefore &&
            abs(kcal - fromMacros) > maxOf(MACRO_TOLERANCE_KCAL, fromMacros * MAX_RELATIVE_DISAGREEMENT)
        ) {
            warnings += uiText(R.string.review_warning_macros_mismatch, kcal.whole(), fromMacros.whole())
        }
        return kcal
    }

    /** The listing's energy in kcal, repairing kJ typed into the kcal field. */
    private fun listedKcal(listed: Double?, kj: Double?, fromMacros: Double?, warnings: MutableList<UiText>): Double? =
        when {
            listed != null && kj != null && kj > MIN_KJ_TO_COMPARE && abs(listed - kj) <= kj * KJ_AS_KCAL_TOLERANCE -> {
                warnings += uiText(R.string.review_warning_kj, kj.whole(), (kj / KJ_PER_KCAL).whole())
                kj / KJ_PER_KCAL
            }
            listed != null &&
                kj != null &&
                abs(
                    listed - kj / KJ_PER_KCAL,
                ) > maxOf(ENERGY_TOLERANCE_KCAL, kj / KJ_PER_KCAL * MAX_RELATIVE_DISAGREEMENT) ->
                resolveEnergyConflict(listed, kj, fromMacros, warnings)
            listed != null -> listed
            kj != null -> kj / KJ_PER_KCAL
            else -> null
        }

    /** Two energy values that can't both be right: trust the one the macros back up. */
    private fun resolveEnergyConflict(
        listed: Double,
        kj: Double,
        fromMacros: Double?,
        warnings: MutableList<UiText>,
    ): Double {
        val fromKj = kj / KJ_PER_KCAL
        val pick = if (fromMacros != null && abs(fromKj - fromMacros) < abs(listed - fromMacros)) fromKj else listed
        warnings += uiText(
            if (fromMacros != null) {
                R.string.review_warning_energy_conflict_macros
            } else {
                R.string.review_warning_energy_conflict
            },
            listed.whole(),
            kj.whole(),
            fromKj.whole(),
            pick.whole(),
        )
        return pick
    }

    // Open Food Facts stores every `_100g` nutrient in grams; scale into the unit the app uses.
    private fun JsonObject.toMicros(): Micros = Nutrient.entries.mapNotNull { nutrient ->
        val key = nutrient.offKey ?: return@mapNotNull null
        this["${key}_100g"].asDouble()?.takeIf { it >= 0 }?.let { nutrient to it * nutrient.offScale }
    }.toMap()

    private fun JsonElement?.asDouble(): Double? = (this as? JsonPrimitive)?.content?.toDoubleOrNull()?.takeIf {
        it.isFinite()
    }

    private companion object {
        const val KJ_PER_KCAL = 4.184

        /** Pure fat; nothing edible is denser. */
        const val MAX_KCAL_PER_100G = 900.0
        const val MACRO_TOLERANCE_KCAL = 40.0
        const val SERVING_TOLERANCE_KCAL = 20.0
        const val ENERGY_TOLERANCE_KCAL = 20.0

        /** A kcal value within this share of the kJ value is the kJ number typed into the wrong field. */
        const val KJ_AS_KCAL_TOLERANCE = 0.1
        const val MIN_KJ_TO_COMPARE = 20.0

        /** Two energy values further apart than this share of the expected one can't both be right. */
        const val MAX_RELATIVE_DISAGREEMENT = 0.5

        const val KCAL_PER_G_PROTEIN = 4.0
        const val KCAL_PER_G_CARBS = 4.0
        const val KCAL_PER_G_FAT = 9.0

        /** Grams; a listed serving above this is almost always a unit or typing slip. */
        const val MAX_PLAUSIBLE_SERVING = 1500.0
    }
}
