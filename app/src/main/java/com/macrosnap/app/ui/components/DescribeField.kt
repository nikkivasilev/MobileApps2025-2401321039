package com.macrosnap.app.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.macrosnap.app.model.PastFood
import com.macrosnap.app.ui.summary
import com.macrosnap.app.ui.whole
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.macrosnap.app.model.DataError
import com.macrosnap.app.ui.userMessage

sealed interface DescribeStatus {
    data object Idle : DescribeStatus
    data object Loading : DescribeStatus
    data class Failed(val error: DataError) : DescribeStatus
}

/**
 * Free-text entry ("50 g olive oil, 2 eggs") that the AI turns into items. [suggestions] are foods
 * logged before that match what's being typed; picking one adds it without asking the AI.
 */
@Composable
fun DescribeField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    status: DescribeStatus,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "e.g. 200 g chicken breast, 1 tbsp olive oil",
    hint: String = "Gemini identifies each food and fills in the macros.",
    suggestions: List<PastFood> = emptyList(),
    onSuggestionClick: (PastFood) -> Unit = {},
) {
    val isLoading = status is DescribeStatus.Loading
    val canSubmit = value.isNotBlank() && !isLoading
    Column(modifier.fillMaxWidth()) {
        // Above the field: while typing, the keyboard covers whatever sits below it.
        if (suggestions.isNotEmpty() && !isLoading) {
            Text(
                text = "From your foods (no AI needed)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            suggestions.forEach { food -> PastFoodRow(food, onClick = { onSuggestionClick(food) }) }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            supportingText = {
                when (status) {
                    is DescribeStatus.Failed -> Text(status.error.userMessage(), color = MaterialTheme.colorScheme.error)
                    DescribeStatus.Loading -> Text("Working out what that is…")
                    DescribeStatus.Idle -> Text(hint)
                }
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onSubmit, enabled = canSubmit) { Text("Add") }
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSubmit) onSubmit() }),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One remembered food: name, usual amount, calories for that amount and how often it's been logged. */
@Composable
fun PastFoodRow(food: PastFood, onClick: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(food.item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${food.item.grams.whole()} g · ${food.item.total.kcal.whole()} kcal · ${food.item.total.summary()}")
        },
        trailingContent = {
            Text("${food.timesLogged}×", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}
