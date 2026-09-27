package com.macrosnap.app.data

import com.macrosnap.app.data.local.FoodDao
import com.macrosnap.app.data.local.FoodEntryEntity
import com.macrosnap.app.data.settings.SettingsStore
import com.macrosnap.app.model.EntrySource
import com.macrosnap.app.model.FoodEntry
import com.macrosnap.app.model.FoodHistory
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.PastFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FoodRepository(
    private val foodDao: FoodDao,
    private val photoStore: PhotoStore,
    private val settingsStore: SettingsStore,
) {

    /** Every food logged before, one per name, most useful first; see [FoodHistory.build]. */
    fun observePastFoods(): Flow<List<PastFood>> =
        combine(foodDao.observeAll(), settingsStore.settings.map { it.hiddenFoods }.distinctUntilChanged()) { rows, hidden ->
            FoodHistory.build(rows.map { it.toModel() }, hidden)
        }

    /** Looks a past food up by key, including hidden ones: the caller already chose it. */
    suspend fun pastFood(key: String): PastFood? =
        FoodHistory.build(foodDao.observeAll().first().map { it.toModel() }).firstOrNull { it.key == key }

    suspend fun hidePastFood(key: String) = settingsStore.hideFood(key)

    fun observeDay(date: LocalDate): Flow<List<FoodEntry>> =
        foodDao.observeDay(date.toEpochDay()).map { rows -> rows.map { it.toModel() } }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<FoodEntry>> =
        foodDao.observeRange(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.toModel() } }

    suspend fun addAll(
        items: List<FoodItem>,
        date: LocalDate,
        mealType: MealType,
        source: EntrySource,
        photoPath: String?,
    ) {
        val now = System.currentTimeMillis()
        foodDao.insertAll(
            items.map { item ->
                FoodEntryEntity(
                    epochDay = date.toEpochDay(),
                    createdAt = now,
                    mealType = mealType.name,
                    name = item.name,
                    grams = item.grams,
                    kcalPer100 = item.per100g.kcal,
                    proteinPer100 = item.per100g.protein,
                    carbsPer100 = item.per100g.carbs,
                    fatPer100 = item.per100g.fat,
                    source = source.name,
                    photoPath = photoPath,
                    micros = MicrosJson.encode(item.micros100g),
                )
            },
        )
    }

    suspend fun update(entry: FoodEntry) = foodDao.update(entry.toEntity())

    suspend fun deleteOrphanPhotos() {
        photoStore.deleteUnreferenced(foodDao.photoPaths().toSet(), olderThanMillis = ORPHAN_PHOTO_GRACE_MILLIS)
    }

    suspend fun delete(entry: FoodEntry) {
        foodDao.deleteById(entry.id)
        // One photo can back several entries (every item on the plate), so it goes with the last one.
        val photoPath = entry.photoPath ?: return
        if (foodDao.countByPhoto(photoPath) == 0) photoStore.delete(photoPath)
    }
}

private const val ORPHAN_PHOTO_GRACE_MILLIS = 2L * 24 * 60 * 60 * 1000

private fun FoodEntryEntity.toModel() = FoodEntry(
    id = id,
    date = LocalDate.ofEpochDay(epochDay),
    createdAt = createdAt,
    mealType = MealType.entries.firstOrNull { it.name == mealType } ?: MealType.SNACK,
    item = FoodItem(
        name = name,
        grams = grams,
        per100g = Macros(kcalPer100, proteinPer100, carbsPer100, fatPer100),
        micros100g = MicrosJson.decode(micros),
    ),
    source = EntrySource.entries.firstOrNull { it.name == source } ?: EntrySource.MANUAL,
    photoPath = photoPath,
)

private fun FoodEntry.toEntity() = FoodEntryEntity(
    id = id,
    epochDay = date.toEpochDay(),
    createdAt = createdAt,
    mealType = mealType.name,
    name = item.name,
    grams = item.grams,
    kcalPer100 = item.per100g.kcal,
    proteinPer100 = item.per100g.protein,
    carbsPer100 = item.per100g.carbs,
    fatPer100 = item.per100g.fat,
    source = source.name,
    photoPath = photoPath,
    micros = MicrosJson.encode(item.micros100g),
)
