package com.macrosnap.app.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.model.Recipe
import com.macrosnap.app.ui.RecipeEditorRoute
import com.macrosnap.app.ui.ReviewRoute
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.components.CardHeader
import com.macrosnap.app.ui.components.MacroDots
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.whole
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onOpenEditor: (RecipeEditorRoute) -> Unit,
    onOpenReview: (ReviewRoute) -> Unit,
    viewModel: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory),
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_recipes)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onOpenEditor(RecipeEditorRoute()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.recipes_new)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (recipes.isEmpty()) {
                item(key = "empty") {
                    AppCard {
                        CardHeader(title = stringResource(R.string.recipes_empty_title), icon = AppIcons.Restaurant, tint = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = stringResource(R.string.recipes_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            items(recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onClick = { onOpenEditor(RecipeEditorRoute(recipe.id)) },
                    onLog = { onOpenReview(ReviewRoute(LocalDate.now().toEpochDay(), recipeId = recipe.id)) },
                )
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit, onLog: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 12.dp)) {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(
                    R.plurals.recipes_card_summary,
                    recipe.ingredients.size,
                    recipe.ingredients.size,
                    recipe.per100g.kcal.whole(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.recipes_card_serving, recipe.servingGrams.whole(), recipe.perServing.kcal.whole()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    MacroDots(recipe.perServing, Modifier.padding(top = 2.dp))
                }
                FilledTonalButton(onClick = onLog) { Text(stringResource(R.string.recipes_log_today)) }
            }
        }
    }
}
