package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM food_entries WHERE epochDay = :epochDay ORDER BY createdAt, id")
    fun observeDay(epochDay: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay")
    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<FoodEntryEntity>>

    @Insert
    suspend fun insertAll(entries: List<FoodEntryEntity>)

    @Update
    suspend fun update(entry: FoodEntryEntity)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM food_entries WHERE photoPath = :photoPath")
    suspend fun countByPhoto(photoPath: String): Int
}
