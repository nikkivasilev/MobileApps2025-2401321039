package com.macrosnap.app.data

import com.macrosnap.app.data.local.SupplementDao
import com.macrosnap.app.data.local.SupplementEntity
import com.macrosnap.app.data.local.SupplementLogEntity
import com.macrosnap.app.model.Supplement
import com.macrosnap.app.model.SupplementStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SupplementRepository(private val supplementDao: SupplementDao) {

    fun observeAll(): Flow<List<Supplement>> = supplementDao.observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeDay(date: LocalDate): Flow<List<SupplementStatus>> =
        combine(supplementDao.observeActive(), supplementDao.observeLogs(date.toEpochDay())) { supplements, logs ->
            supplements.map { row -> SupplementStatus(row.toModel(), logs.count { it.supplementId == row.id }) }
        }

    suspend fun get(id: Long): Supplement? = supplementDao.get(id)?.toModel()

    suspend fun save(supplement: Supplement): Long {
        val existing = if (supplement.id != 0L) supplementDao.get(supplement.id) else null
        val rowId = supplementDao.upsert(
            SupplementEntity(
                id = supplement.id,
                name = supplement.name,
                doseLabel = supplement.doseLabel,
                timesPerDay = supplement.timesPerDay,
                micros = MicrosJson.encode(supplement.micros),
                isActive = supplement.isActive,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            ),
        )
        return rowId.takeIf { it > 0 } ?: supplement.id
    }

    suspend fun delete(id: Long) = supplementDao.delete(id)

    suspend fun take(supplementId: Long, date: LocalDate) =
        supplementDao.insertLog(SupplementLogEntity(supplementId = supplementId, epochDay = date.toEpochDay(), takenAt = System.currentTimeMillis()))

    suspend fun untake(supplementId: Long, date: LocalDate) = supplementDao.deleteLatestLog(supplementId, date.toEpochDay())
}

private fun SupplementEntity.toModel() = Supplement(
    id = id,
    name = name,
    doseLabel = doseLabel,
    timesPerDay = timesPerDay,
    micros = MicrosJson.decode(micros),
    isActive = isActive,
)
