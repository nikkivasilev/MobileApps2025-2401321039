package com.macrosnap.app.data

import com.macrosnap.app.data.local.RecipeDao
import com.macrosnap.app.data.local.RecipeEntity
import com.macrosnap.app.data.local.RecipeIngredientEntity
import com.macrosnap.app.data.local.RecipeWithIngredients
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecipeRepository(private val recipeDao: RecipeDao) {

    fun observeAll(): Flow<List<Recipe>> = recipeDao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun get(id: Long): Recipe? = recipeDao.get(id)?.toModel()

    suspend fun save(recipe: Recipe): Long {
        val now = System.currentTimeMillis()
        val existing = if (recipe.id != 0L) recipeDao.get(recipe.id)?.recipe else null
        val entity = RecipeEntity(
            id = recipe.id,
            name = recipe.name,
            cookedWeightGrams = recipe.cookedWeightGrams,
            servings = recipe.servings,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val ingredients = recipe.ingredients.mapIndexed { index, item ->
            RecipeIngredientEntity(
                recipeId = recipe.id,
                position = index,
                name = item.name,
                grams = item.grams,
                kcalPer100 = item.per100g.kcal,
                proteinPer100 = item.per100g.protein,
                carbsPer100 = item.per100g.carbs,
                fatPer100 = item.per100g.fat,
                micros = MicrosJson.encode(item.micros100g),
            )
        }
        return recipeDao.save(entity, ingredients)
    }

    suspend fun delete(id: Long) = recipeDao.delete(id)
}

private fun RecipeWithIngredients.toModel() = Recipe(
    id = recipe.id,
    name = recipe.name,
    cookedWeightGrams = recipe.cookedWeightGrams,
    servings = recipe.servings,
    ingredients = ingredients.sortedBy { it.position }.map {
        FoodItem(
            name = it.name,
            grams = it.grams,
            per100g = Macros(it.kcalPer100, it.proteinPer100, it.carbsPer100, it.fatPer100),
            micros100g = MicrosJson.decode(it.micros),
        )
    },
)
