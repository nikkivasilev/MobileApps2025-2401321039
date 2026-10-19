package com.macrosnap.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macrosnap.app.R
import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.FoodItem
import com.macrosnap.app.model.Macros
import com.macrosnap.app.model.MealType
import com.macrosnap.app.model.Nutrient
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.scaled
import com.macrosnap.app.ui.compact
import com.macrosnap.app.ui.summary
import com.macrosnap.app.ui.toAmountOrNull
import com.macrosnap.app.ui.whole
import kotlinx.coroutines.launch

private enum class ValueBasis(@StringRes val labelRes: Int) {
    PER_100G(R.string.review_basis_per_100g),
    PORTION(R.string.review_basis_portion),
}

/**
 * Add/edit form for one food item. Labels give values per 100 g while people remember
 * "that shake is 150 kcal", so both ways of entering nutrition are offered.
 *
 * @param initialMealType pass null to hide the meal picker (the caller sets the meal elsewhere).
 * @param onDescribe when given, a describe-in-words field lets the AI fill the form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    title: String,
    initial: FoodItem?,
    initialMealType: MealType?,
    onSave: (FoodItem, MealType?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onDescribe: (suspend (String) -> Outcome<AnalysisResult>)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var grams by rememberSaveable { mutableStateOf(initial?.grams?.compact().orEmpty()) }
    var basis by rememberSaveable { mutableStateOf(if (initial != null) ValueBasis.PER_100G else ValueBasis.PORTION) }
    var kcal by rememberSaveable { mutableStateOf(initial?.per100g?.kcal?.compact().orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(initial?.per100g?.protein?.compact().orEmpty()) }
    var carbs by rememberSaveable { mutableStateOf(initial?.per100g?.carbs?.compact().orEmpty()) }
    var fat by rememberSaveable { mutableStateOf(initial?.per100g?.fat?.compact().orEmpty()) }
    var mealType by rememberSaveable { mutableStateOf(initialMealType) }
    val micros =
        remember { mutableStateMapOf<Nutrient, String>().apply { putAll(initial?.micros100g.orEmpty().toInputs()) } }
    var showMicros by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    var describeInput by rememberSaveable { mutableStateOf("") }
    var describeStatus by remember { mutableStateOf<DescribeStatus>(DescribeStatus.Idle) }
    var describeNote by rememberSaveable { mutableStateOf<String?>(null) }

    /** How many foods the last description held; only the first fills the form. */
    var describedCount by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun fillFrom(item: FoodItem) {
        name = item.name
        grams = item.grams.compact()
        basis = ValueBasis.PER_100G
        kcal = item.per100g.kcal.compact()
        protein = item.per100g.protein.compact()
        carbs = item.per100g.carbs.compact()
        fat = item.per100g.fat.compact()
        micros.clear()
        micros.putAll(item.micros100g.toInputs())
    }

    fun describe() {
        val describer = onDescribe ?: return
        val text = describeInput.trim()
        if (text.isEmpty() || describeStatus is DescribeStatus.Loading) return
        describeStatus = DescribeStatus.Loading
        scope.launch {
            when (val outcome = describer(text)) {
                is Outcome.Success -> {
                    val items = outcome.value.items
                    fillFrom(items.first().item)
                    describeInput = ""
                    describeStatus = DescribeStatus.Idle
                    describedCount = items.size
                    describeNote = outcome.value.notes?.trim()?.ifEmpty { null }
                }
                is Outcome.Failure -> describeStatus = DescribeStatus.Failed(outcome.error)
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.review_delete_entry_title)) },
            text = { Text(stringResource(R.string.review_delete_entry_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    val gramsValue = grams.toAmountOrNull()?.takeIf { it > 0 }
    val proteinValue = protein.toAmountOrNull() ?: 0.0
    val carbsValue = carbs.toAmountOrNull() ?: 0.0
    val fatValue = fat.toAmountOrNull() ?: 0.0
    val derivedKcal = 4 * proteinValue + 4 * carbsValue + 9 * fatValue
    val kcalValue = kcal.toAmountOrNull() ?: derivedKcal.takeIf { kcal.isBlank() && it > 0 }

    val toPer100g: Double? = gramsValue?.let { if (basis == ValueBasis.PER_100G) 1.0 else 100.0 / it }
    val per100g: Macros? = if (kcalValue == null || toPer100g == null) {
        null
    } else {
        Macros(kcalValue, proteinValue, carbsValue, fatValue) * toPer100g
    }
    val result = if (name.isBlank() || gramsValue == null || toPer100g == null) {
        null
    } else {
        per100g?.let { FoodItem(name.trim(), gramsValue, it, micros.toMicros().scaled(toPer100g)) }
    }

    fun switchBasis(target: ValueBasis) {
        if (target == basis) return
        val g = gramsValue
        if (g != null) {
            val factor = if (target == ValueBasis.PORTION) g / 100.0 else 100.0 / g
            fun String.rescaled() = toAmountOrNull()?.let { (it * factor).compact() } ?: this
            kcal = kcal.rescaled()
            protein = protein.rescaled()
            carbs = carbs.rescaled()
            fat = fat.rescaled()
            micros.keys.toList().forEach { micros[it] = micros.getValue(it).rescaled() }
        }
        basis = target
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Before verticalScroll, so the viewport shrinks above the keyboard and the
                // focused field scrolls into view instead of hiding behind it.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)

            if (onDescribe != null) {
                DescribeField(
                    value = describeInput,
                    onValueChange = {
                        describeInput = it
                        if (describeStatus is DescribeStatus.Failed) describeStatus = DescribeStatus.Idle
                    },
                    onSubmit = ::describe,
                    status = describeStatus,
                    label = stringResource(R.string.review_editor_describe_label),
                    placeholder = stringResource(R.string.review_editor_describe_placeholder),
                    hint = stringResource(R.string.review_editor_describe_hint),
                )
                val firstOnly = if (describedCount > 1) {
                    pluralStringResource(R.plurals.review_editor_first_only, describedCount, describedCount)
                } else {
                    null
                }
                listOfNotNull(firstOnly, describeNote).takeIf { it.isNotEmpty() }?.let { parts ->
                    Text(
                        text = stringResource(R.string.review_note, parts.joinToString(" ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.review_food_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(grams, { grams = it }, stringResource(R.string.review_amount_eaten), Modifier.fillMaxWidth())

            mealType?.let { selected ->
                MealTypeChips(selected = selected, onSelect = { mealType = it })
            }

            Text(stringResource(R.string.review_values_for), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ValueBasis.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = basis == option,
                        onClick = { switchBasis(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, ValueBasis.entries.size),
                    ) { Text(stringResource(option.labelRes)) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = stringResource(R.string.calories),
                    modifier = Modifier.weight(1f),
                    placeholder = derivedKcal.takeIf { it > 0 }?.whole(),
                )
                AmountField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = stringResource(R.string.review_field_protein),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(carbs, { carbs = it }, stringResource(R.string.review_field_carbs), Modifier.weight(1f))
                AmountField(fat, { fat = it }, stringResource(R.string.review_field_fat), Modifier.weight(1f))
            }

            TextButton(onClick = { showMicros = !showMicros }) {
                Text(stringResource(if (showMicros) R.string.review_hide_micros else R.string.review_show_micros))
            }
            if (showMicros) {
                NutrientFields(values = micros, onValueChange = { nutrient, value -> micros[nutrient] = value })
            }

            Text(
                text = result?.total?.let {
                    stringResource(R.string.review_this_portion, it.kcal.whole(), it.summary())
                }
                    ?: stringResource(R.string.review_editor_incomplete),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { result?.let { onSave(it, mealType) } },
                    enabled = result != null && describeStatus !is DescribeStatus.Loading,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
