package com.macrosnap.app.data

import com.macrosnap.app.data.local.WeightDao
import com.macrosnap.app.data.local.WeightEntryEntity
import com.macrosnap.app.model.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepository(private val weightDao: WeightDao) {

    fun observeAll(): Flow<List<WeightEntry>> = weightDao.observeAll().map { rows ->
        rows.map { WeightEntry(LocalDate.ofEpochDay(it.epochDay), it.weightKg) }
    }

    /** Latest logged weight, for calorie-burn estimates; the profile weight is the fallback. */
    fun observeLatestKg(): Flow<Double?> = weightDao.observeAll().map { rows -> rows.lastOrNull()?.weightKg }

    suspend fun log(date: LocalDate, weightKg: Double) =
        weightDao.upsert(WeightEntryEntity(date.toEpochDay(), weightKg))

    suspend fun delete(date: LocalDate) = weightDao.deleteByDay(date.toEpochDay())
}
