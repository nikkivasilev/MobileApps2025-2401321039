package com.macrosnap.app.data

import com.macrosnap.app.data.local.ProductMicrosDao
import com.macrosnap.app.data.local.ProductMicrosEntity
import com.macrosnap.app.data.remote.OffProduct
import com.macrosnap.app.data.remote.OffProductResponse
import com.macrosnap.app.data.remote.OpenFoodFactsApi
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.DataError
import com.macrosnap.app.model.Micros
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.SupplementAnalysis
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRepositoryTest {

    private class FakeApi(private val product: OffProduct?) : OpenFoodFactsApi {
        override suspend fun product(barcode: String, fields: String) =
            OffProductResponse(status = if (product != null) 1 else 0, product = product)
    }

    private class FakeCache : ProductMicrosDao {
        val rows = mutableMapOf<String, ProductMicrosEntity>()
        override suspend fun get(barcode: String) = rows[barcode]
        override suspend fun upsert(entry: ProductMicrosEntity) {
            rows[entry.barcode] = entry
        }
    }

    private class FakeAnalyzer(private val estimate: Outcome<Micros>) : FoodAnalyzer {
        var calls = 0
        override suspend fun analyzePhoto(photoPath: String, hint: String?): Outcome<AnalysisResult> = error("unused")
        override suspend fun analyzeText(description: String): Outcome<AnalysisResult> = error("unused")
        override suspend fun analyzeSupplement(description: String): Outcome<SupplementAnalysis> = error("unused")
        override suspend fun estimateProductMicros(productName: String, brand: String?, quantity: String?): Outcome<Micros> {
            calls++
            return estimate
        }
    }

    private fun nutriments(vararg pairs: Pair<String, Number>): JsonObject =
        JsonObject(pairs.associate { (key, value) -> key to JsonPrimitive(value) })

    private fun product(nutriments: JsonObject, serving: Number? = 250, name: String = "Red Bull Zero") = OffProduct(
        productName = name,
        brands = "Red Bull",
        nutriments = nutriments,
        servingQuantity = serving?.let(::JsonPrimitive),
        quantity = "250 ml",
    )

    private fun repository(product: OffProduct?, analyzer: FakeAnalyzer = FakeAnalyzer(Outcome.Success(emptyMap())), cache: FakeCache = FakeCache()) =
        ProductRepository(FakeApi(product), cache, analyzer)

    private suspend fun ProductRepository.scan(): ScannedProduct = (lookup("90415425") as Outcome.Success).value

    @Test
    fun `clean listing keeps label values and maps B3, B5 and caffeine`() = runTest {
        // Open Food Facts record 90415425, trimmed.
        val scanned = repository(
            product(
                nutriments(
                    "energy-kcal_100g" to 2, "energy-kj_100g" to 9, "proteins_100g" to 0, "carbohydrates_100g" to 0,
                    "fat_100g" to 0, "caffeine_100g" to 0.032, "vitamin-pp_100g" to 0.008, "pantothenic-acid_100g" to 0.002,
                ),
            ),
        ).scan()

        assertEquals(2.0, scanned.item.per100g.kcal, 0.001)
        assertEquals(250.0, scanned.item.grams, 0.001)
        assertTrue(scanned.warnings.isEmpty())
        assertEquals(32.0, scanned.item.micros100g.getValue(Nutrient.CAFFEINE), 0.001)
        assertEquals(8.0, scanned.item.micros100g.getValue(Nutrient.VITAMIN_B3), 0.001)
        assertEquals(2.0, scanned.item.micros100g.getValue(Nutrient.VITAMIN_B5), 0.001)
        assertTrue(Nutrient.VITAMIN_B12 in scanned.missingMicros)
        assertTrue(Nutrient.CAFFEINE !in scanned.missingMicros)
    }

    @Test
    fun `kcal field holding kJ is converted`() = runTest {
        val scanned = repository(product(nutriments("energy-kcal_100g" to 192, "energy-kj_100g" to 192, "carbohydrates_100g" to 11, "proteins_100g" to 0, "fat_100g" to 0))).scan()

        assertEquals(192 / 4.184, scanned.item.per100g.kcal, 0.01)
        assertEquals(1, scanned.warnings.size)
    }

    @Test
    fun `kcal that disagrees with kJ falls back to the value the macros support`() = runTest {
        // 330 "kcal" per 100 ml × 250 ml would log 825 kcal for a sugar-free can.
        val scanned = repository(product(nutriments("energy-kcal_100g" to 330, "energy-kj_100g" to 13, "proteins_100g" to 0, "carbohydrates_100g" to 0, "fat_100g" to 0))).scan()

        assertEquals(13 / 4.184, scanned.item.per100g.kcal, 0.01)
        assertTrue(scanned.item.total.kcal < 10)
        assertEquals(1, scanned.warnings.size)
    }

    @Test
    fun `kcal the macros cannot explain is flagged but kept`() = runTest {
        val scanned = repository(product(nutriments("energy-kcal_100g" to 330, "proteins_100g" to 0, "carbohydrates_100g" to 0, "fat_100g" to 0))).scan()

        assertEquals(330.0, scanned.item.per100g.kcal, 0.001)
        assertEquals(1, scanned.warnings.size)
    }

    @Test
    fun `implausible serving falls back to 100 g`() = runTest {
        val scanned = repository(product(nutriments("energy-kcal_100g" to 3), serving = 25_000)).scan()

        assertEquals(100.0, scanned.item.grams, 0.001)
        assertEquals(1, scanned.warnings.size)
    }

    @Test
    fun `listing without energy is not a usable product`() = runTest {
        val outcome = repository(product(nutriments("proteins_100g" to 1))).lookup("1")
        assertEquals(Outcome.Failure(DataError.ProductNotFound), outcome)
    }

    @Test
    fun `estimate fills only missing nutrients and is cached per barcode`() = runTest {
        val analyzer = FakeAnalyzer(
            Outcome.Success(mapOf(Nutrient.CAFFEINE to 999.0, Nutrient.VITAMIN_B12 to 0.8, Nutrient.VITAMIN_B6 to 0.8)),
        )
        val cache = FakeCache()
        val repo = repository(product(nutriments("energy-kcal_100g" to 3, "caffeine_100g" to 0.032)), analyzer, cache)
        val scanned = repo.scan()

        val first = (repo.estimateMissingMicros(scanned) as Outcome.Success).value
        val second = (repo.estimateMissingMicros(scanned) as Outcome.Success).value

        assertEquals(mapOf(Nutrient.VITAMIN_B12 to 0.8, Nutrient.VITAMIN_B6 to 0.8), first)
        assertEquals(first, second)
        assertEquals(1, analyzer.calls)
        assertTrue("90415425" in cache.rows)
    }

    @Test
    fun `failed estimate is not cached`() = runTest {
        val analyzer = FakeAnalyzer(Outcome.Failure(DataError.RateLimited(daily = false)))
        val cache = FakeCache()
        val repo = repository(product(nutriments("energy-kcal_100g" to 3)), analyzer, cache)

        val outcome = repo.estimateMissingMicros(repo.scan())

        assertTrue(outcome is Outcome.Failure)
        assertTrue(cache.rows.isEmpty())
    }

    @Test
    fun `numbers sent as strings are still read`() = runTest {
        val raw = Json.parseToJsonElement("""{"energy-kcal_100g":"45","proteins_100g":"0","carbohydrates_100g":"11","fat_100g":"0"}""").jsonObject
        val scanned = repository(product(raw)).scan()

        assertEquals(45.0, scanned.item.per100g.kcal, 0.001)
        assertTrue(scanned.warnings.isEmpty())
    }
}
