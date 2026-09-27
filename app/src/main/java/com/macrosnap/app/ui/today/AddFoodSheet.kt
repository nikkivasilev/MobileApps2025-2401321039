package com.macrosnap.app.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.matching
import com.macrosnap.app.ui.components.PastFoodRow

/** Foods shown above the add methods; the rest follow below them. */
private const val TOP_FOODS = 5

/** Enough to scroll through without making the sheet heavy; search reaches the rest. */
private const val MAX_LISTED_FOODS = 100

/**
 * The add-food sheet: foods logged before come first (search, or the most frequent), so a
 * repeat takes one tap and no AI request; the ways to add something new follow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFoodSheet(
    pastFoods: List<PastFood>,
    onMethod: (AddMethod) -> Unit,
    onPastFood: (PastFood) -> Unit,
    onDescribe: (String) -> Unit,
    onHide: (PastFood) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var hiding by remember { mutableStateOf<PastFood?>(null) }
    val matches = remember(pastFoods, query) { pastFoods.matching(query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = pastFoods.isNotEmpty()),
    ) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (pastFoods.isNotEmpty()) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search your foods") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (query.isBlank()) {
                if (pastFoods.isNotEmpty()) {
                    header("Your foods", "Tap to log again · long-press to hide")
                    pastFoodItems(pastFoods.take(TOP_FOODS), onPastFood) { hiding = it }
                    header("Add something new")
                }
                items(AddMethod.entries, key = { "method-${it.name}" }) { method -> MethodRow(method) { onMethod(method) } }
                if (pastFoods.size > TOP_FOODS) {
                    header("More of your foods")
                    pastFoodItems(pastFoods.drop(TOP_FOODS).take(MAX_LISTED_FOODS), onPastFood) { hiding = it }
                }
            } else {
                if (matches.isEmpty()) {
                    item(key = "no-match") {
                        Text(
                            text = "Nothing called \"${query.trim()}\" in your foods yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
                pastFoodItems(matches.take(MAX_LISTED_FOODS), onPastFood) { hiding = it }
                item(key = "describe-query") {
                    ListItem(
                        headlineContent = { Text("Describe \"${query.trim()}\"") },
                        supportingContent = { Text("Gemini works out the macros") },
                        leadingContent = { Text(AddMethod.DESCRIBE.emoji, style = MaterialTheme.typography.headlineSmall) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onDescribe(query.trim()) },
                    )
                }
            }
        }
    }

    hiding?.let { food ->
        AlertDialog(
            onDismissRequest = { hiding = null },
            title = { Text("Hide ${food.item.name}?") },
            text = { Text("It won't be suggested again unless you log it again. Your logged entries stay as they are.") },
            confirmButton = {
                TextButton(onClick = {
                    hiding = null
                    onHide(food)
                }) { Text("Hide") }
            },
            dismissButton = { TextButton(onClick = { hiding = null }) { Text("Cancel") } },
        )
    }
}

private fun LazyListScope.header(title: String, subtitle: String? = null) {
    item(key = "header-$title") {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
            supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

private fun LazyListScope.pastFoodItems(foods: List<PastFood>, onClick: (PastFood) -> Unit, onLongClick: (PastFood) -> Unit) {
    items(foods, key = { "food-${it.key}" }) { food ->
        PastFoodRow(
            food = food,
            onClick = { onClick(food) },
            onLongClick = { onLongClick(food) },
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun MethodRow(method: AddMethod, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(method.title) },
        supportingContent = { Text(method.subtitle) },
        leadingContent = { Text(method.emoji, style = MaterialTheme.typography.headlineSmall) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
