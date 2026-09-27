package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WellnessDao {

    @Query("SELECT * FROM water_logs WHERE epochDay = :epochDay ORDER BY loggedAt")
    fun observeWater(epochDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE epochDay BETWEEN :from AND :to")
    fun observeWaterRange(from: Long, to: Long): Flow<List<WaterLogEntity>>

    @Insert
    suspend fun insertWater(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = (SELECT id FROM water_logs WHERE epochDay = :epochDay ORDER BY loggedAt DESC, id DESC LIMIT 1)")
    suspend fun deleteLatestWater(epochDay: Long)

    @Query("SELECT * FROM sleep_logs WHERE epochDay = :epochDay")
    fun observeSleep(epochDay: Long): Flow<SleepLogEntity?>

    @Query("SELECT * FROM sleep_logs WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay")
    fun observeSleepRange(from: Long, to: Long): Flow<List<SleepLogEntity>>

    @Upsert
    suspend fun upsertSleep(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE epochDay = :epochDay")
    suspend fun deleteSleep(epochDay: Long)
}
