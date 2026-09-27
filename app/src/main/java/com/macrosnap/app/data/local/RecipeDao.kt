package com.macrosnap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: Long): RecipeWithIngredients?

    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun save(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>): Long {
        // Upsert reports -1 for an update, in which case the row keeps its existing id.
        val id = upsertRecipe(recipe).takeIf { it > 0 } ?: recipe.id
        deleteIngredients(id)
        insertIngredients(ingredients.map { it.copy(recipeId = id) })
        return id
    }
}
