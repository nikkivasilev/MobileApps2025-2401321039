package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Gemini's vitamin and mineral estimate for a barcode whose Open Food Facts listing lacks them,
 * kept so scanning the same product again doesn't spend another request of the free quota.
 */
@Entity(tableName = "product_micros")
data class ProductMicrosEntity(
    @PrimaryKey val barcode: String,
    /** Per 100 g, as [com.macrosnap.app.data.MicrosJson]. */
    val micros: String?,
    val estimatedAt: Long,
)

@Dao
interface ProductMicrosDao {

    @Query("SELECT * FROM product_micros WHERE barcode = :barcode")
    suspend fun get(barcode: String): ProductMicrosEntity?

    @Upsert
    suspend fun upsert(entry: ProductMicrosEntity)
}
