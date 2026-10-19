package com.macrosnap.app.data

import com.macrosnap.app.data.local.SleepLogEntity
import com.macrosnap.app.data.local.WaterLogEntity
import com.macrosnap.app.data.local.WellnessDao
import com.macrosnap.app.model.SleepLog
import com.macrosnap.app.model.WaterDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WellnessRepository(private val wellnessDao: WellnessDao) {

    fun observeWater(date: LocalDate): Flow<WaterDay> =
        wellnessDao.observeWater(date.toEpochDay()).map { rows -> WaterDay(date, rows.sumOf { it.ml }, rows.size) }

    fun observeWaterRange(from: LocalDate, to: LocalDate): Flow<List<WaterDay>> =
        wellnessDao.observeWaterRange(from.toEpochDay(), to.toEpochDay()).map { rows ->
            val byDay = rows.groupBy { it.epochDay }
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { date ->
                val day = byDay[date.toEpochDay()].orEmpty()
                WaterDay(date, day.sumOf { it.ml }, day.size)
            }.toList()
        }

    suspend fun addWater(date: LocalDate, ml: Int) = wellnessDao.insertWater(
        WaterLogEntity(epochDay = date.toEpochDay(), ml = ml, loggedAt = System.currentTimeMillis()),
    )

    suspend fun undoWater(date: LocalDate) = wellnessDao.deleteLatestWater(date.toEpochDay())

    fun observeSleep(date: LocalDate): Flow<SleepLog?> = wellnessDao.observeSleep(date.toEpochDay()).map {
        it?.toModel()
    }

    fun observeSleepRange(from: LocalDate, to: LocalDate): Flow<List<SleepLog>> =
        wellnessDao.observeSleepRange(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.toModel() } }

    suspend fun logSleep(log: SleepLog) = wellnessDao.upsertSleep(
        SleepLogEntity(
            epochDay = log.date.toEpochDay(),
            bedtime = log.bedtime.toEpochMillis(),
            wakeTime = log.wakeTime.toEpochMillis(),
            quality = log.quality,
        ),
    )

    suspend fun deleteSleep(date: LocalDate) = wellnessDao.deleteSleep(date.toEpochDay())
}

private fun SleepLogEntity.toModel() = SleepLog(
    date = LocalDate.ofEpochDay(epochDay),
    bedtime = bedtime.toLocalDateTime(),
    wakeTime = wakeTime.toLocalDateTime(),
    quality = quality,
)

private fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

private fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
