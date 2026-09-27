package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {

    @Query("SELECT * FROM supplements ORDER BY createdAt, id")
    fun observeAll(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE id = :id")
    suspend fun get(id: Long): SupplementEntity?

    @Upsert
    suspend fun upsert(supplement: SupplementEntity): Long

    @Query("DELETE FROM supplements WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM supplement_logs WHERE epochDay = :epochDay")
    fun observeLogs(epochDay: Long): Flow<List<SupplementLogEntity>>

    @Insert
    suspend fun insertLog(log: SupplementLogEntity)

    @Query(
        "DELETE FROM supplement_logs WHERE id = (SELECT id FROM supplement_logs " +
            "WHERE supplementId = :supplementId AND epochDay = :epochDay ORDER BY takenAt DESC, id DESC LIMIT 1)",
    )
    suspend fun deleteLatestLog(supplementId: Long, epochDay: Long)
}
