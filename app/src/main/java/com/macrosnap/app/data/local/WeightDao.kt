package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Upsert
    suspend fun upsert(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE epochDay = :epochDay")
    suspend fun deleteByDay(epochDay: Long)
}
