package com.macrosnap.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.model.matching
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.components.IconBadge
import com.macrosnap.app.ui.components.PastFoodRow
import com.macrosnap.app.ui.components.SectionTitle
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme

/** Enough to scroll through without making the sheet heavy; search reaches the rest. */
private const val MAX_LISTED_FOODS = 100

/**
 * The add-food sheet: the ways to add something new as a compact grid, then foods logged before
 * (search, or the most frequent), so a repeat takes one tap and no AI request.
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
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            item(key = "title") {
                Text(
                    text = "Add food",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
            }
            if (pastFoods.isNotEmpty()) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search your foods") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppTheme.colors.card,
                            unfocusedContainerColor = AppTheme.colors.card,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                }
            }

            if (query.isBlank()) {
                item(key = "methods") { MethodGrid(onMethod) }
                if (pastFoods.isNotEmpty()) {
                    item(key = "foods-title") {
                        SectionTitle("Your foods", Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                            Text(
                                text = "Long-press to hide",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    pastFoodItems(pastFoods.take(MAX_LISTED_FOODS), onPastFood) { hiding = it }
                }
            } else {
                if (matches.isEmpty()) {
                    item(key = "no-match") {
                        Text(
                            text = "Nothing called \"${query.trim()}\" in your foods yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
                pastFoodItems(matches.take(MAX_LISTED_FOODS), onPastFood) { hiding = it }
                item(key = "describe-query") {
                    AppCard(
                        onClick = { onDescribe(query.trim()) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(AppIcons.Sparkle, MaterialTheme.colorScheme.onSurface)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("Describe \"${query.trim()}\"", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Gemini works out the macros",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
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

/** Every way to add something new, three to a row; the photo, the main path, is filled in ink. */
@Composable
private fun MethodGrid(onMethod: (AddMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AddMethod.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { method -> MethodTile(method, Modifier.weight(1f)) { onMethod(method) } }
            }
        }
    }
}

@Composable
private fun MethodTile(method: AddMethod, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val primary = method == AddMethod.CAMERA
    val container = if (primary) MaterialTheme.colorScheme.primary else AppTheme.colors.card
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        modifier = modifier.height(96.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(content.copy(alpha = if (primary) 0.16f else 0.07f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(method.icon, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(method.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

private fun LazyListScope.pastFoodItems(foods: List<PastFood>, onClick: (PastFood) -> Unit, onLongClick: (PastFood) -> Unit) {
    itemsIndexed(foods, key = { _, food -> "food-${food.key}" }) { index, food ->
        Surface(
            color = AppTheme.colors.card,
            shape = groupedShape(index, foods.size),
            modifier = Modifier.padding(bottom = if (index == foods.size - 1) 0.dp else 2.dp),
        ) {
            PastFoodRow(
                food = food,
                onClick = { onClick(food) },
                onLongClick = { onLongClick(food) },
            )
        }
    }
}

/** Rows of one list share a card: only the first and last get rounded outer corners. */
private fun groupedShape(index: Int, count: Int): Shape {
    val top = if (index == 0) 24.dp else 4.dp
    val bottom = if (index == count - 1) 24.dp else 4.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
