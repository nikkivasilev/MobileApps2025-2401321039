package com.macrosnap.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}")
    suspend fun product(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,brands,nutriments,serving_quantity",
    ): OffProductResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }
}

@Serializable
data class OffProductResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

// Open Food Facts is crowd-sourced: numeric fields arrive as numbers or as strings depending
// on who entered them, so they stay JsonElement and are parsed leniently in the repository.
@Serializable
data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: JsonObject? = null,
    @SerialName("serving_quantity") val servingQuantity: JsonElement? = null,
)
