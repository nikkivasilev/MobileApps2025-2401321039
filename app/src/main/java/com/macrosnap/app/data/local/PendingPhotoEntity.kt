package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A photo waiting to be analyzed again; see [com.macrosnap.app.model.PendingPhoto]. */
@Entity(tableName = "pending_photos")
data class PendingPhotoEntity(
    @PrimaryKey val photoPath: String,
    val epochDay: Long,
    val mealType: String,
    val hint: String,
    val savedAt: Long,
)

@Dao
interface PendingPhotoDao {

    @Query("SELECT * FROM pending_photos ORDER BY savedAt")
    fun observeAll(): Flow<List<PendingPhotoEntity>>

    @Query("SELECT * FROM pending_photos WHERE photoPath = :photoPath")
    suspend fun get(photoPath: String): PendingPhotoEntity?

    @Upsert
    suspend fun upsert(entry: PendingPhotoEntity)

    @Query("DELETE FROM pending_photos WHERE photoPath = :photoPath")
    suspend fun delete(photoPath: String)

    @Query("SELECT photoPath FROM pending_photos")
    suspend fun photoPaths(): List<String>
}
