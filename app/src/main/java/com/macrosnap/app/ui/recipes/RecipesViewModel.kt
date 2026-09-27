package com.macrosnap.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrosnap.app.appContainer
import com.macrosnap.app.data.RecipeRepository
import com.macrosnap.app.model.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RecipesViewModel(recipeRepository: RecipeRepository) : ViewModel() {

    val recipes: StateFlow<List<Recipe>> = recipeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { RecipesViewModel(appContainer.recipeRepository) }
        }
    }
}
