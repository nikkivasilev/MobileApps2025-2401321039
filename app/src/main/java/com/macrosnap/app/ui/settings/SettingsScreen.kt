package com.macrosnap.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Sex
import com.macrosnap.app.ui.SupplementEditorRoute
import com.macrosnap.app.ui.components.NutrientFields
import com.macrosnap.app.ui.userMessage

private const val API_KEY_URL = "https://aistudio.google.com/apikey"

@Composable
fun SettingsScreen(
    onOpenSupplementEditor: (SupplementEditorRoute) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    SettingsContent(
        uiState = uiState,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onOpenSupplementEditor = onOpenSupplementEditor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
    snackbarHostState: SnackbarHostState,
    onOpenSupplementEditor: (SupplementEditorRoute) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SupplementsCard(uiState, onOpenSupplementEditor)
            GoalsCard(uiState, actions)
            WellnessCard(uiState, actions)
            NutrientTargetsCard(uiState, actions)
            CalculatorCard(uiState, actions)
            ConnectionCard(uiState, actions)
        }
    }
}

@Composable
private fun SupplementsCard(uiState: SettingsUiState, onOpenSupplementEditor: (SupplementEditorRoute) -> Unit) {
    SettingsCard("Supplements") {
        if (uiState.supplements.isEmpty()) {
            Text(
                text = "Add what you take (creatine, vitamin D, fish oil…) and Today shows a checklist. " +
                    "Ticked doses count toward your vitamins & minerals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.supplements.forEach { supplement ->
            ListItem(
                headlineContent = { Text(supplement.name) },
                supportingContent = {
                    Text(
                        buildString {
                            append(supplement.doseLabel)
                            if (supplement.timesPerDay > 1) append(" · ${supplement.timesPerDay}× a day")
                            if (!supplement.isActive) append(" · paused")
                        },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onOpenSupplementEditor(SupplementEditorRoute(supplement.id)) },
            )
        }
        OutlinedButton(onClick = { onOpenSupplementEditor(SupplementEditorRoute()) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Add supplement", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun WellnessCard(uiState: SettingsUiState, actions: SettingsActions) {
    SettingsCard("Water, sleep & gym") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberInput("Water goal (ml)", uiState.waterGoalInput, KeyboardType.Number, Modifier.weight(1f), actions::onWaterGoalChange)
            NumberInput("Sleep goal (h)", uiState.sleepGoalInput, KeyboardType.Decimal, Modifier.weight(1f), actions::onSleepGoalChange)
        }
        NumberInput("Rest timer between sets (seconds)", uiState.restSecondsInput, KeyboardType.Number, Modifier.fillMaxWidth(), actions::onRestSecondsChange)
        Button(onClick = actions::onSaveWellness, enabled = uiState.isWellnessDirty) { Text("Save") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Add workout calories to my daily goal", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Estimates are rough (MET × body weight × time). Off keeps the goal fixed, since the calculator already assumes your activity level.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = uiState.saved?.addWorkoutBurn == true, onCheckedChange = actions::onAddWorkoutBurnChange)
        }
    }
}

@Composable
private fun NutrientTargetsCard(uiState: SettingsUiState, actions: SettingsActions) {
    SettingsCard("Vitamin & mineral targets") {
        Text(
            text = "Pre-filled with adult recommended daily intakes for your sex (from the goal calculator). " +
                "Caffeine is a daily maximum. Tip: 1000 IU of vitamin D is 25 µg.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NutrientFields(values = uiState.nutrientInputs, onValueChange = actions::onNutrientTargetChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions::onSaveNutrientTargets, enabled = uiState.isNutrientTargetsDirty) { Text("Save targets") }
            TextButton(onClick = actions::onResetNutrientTargets) { Text("Reset to defaults") }
        }
    }
}

@Composable
private fun ConnectionCard(uiState: SettingsUiState, actions: SettingsActions) {
    val context = LocalContext.current
    var showKey by rememberSaveable { mutableStateOf(false) }

    SettingsCard("AI recognition") {
        Text(
            text = "MacroSnap uses Google's Gemini through its free tier. Create a key in Google AI Studio " +
                "(a Google account is all it takes, no card), then paste it here. It stays on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(API_KEY_URL)))
                } catch (e: ActivityNotFoundException) {
                    // No browser installed; the URL is still visible on the button.
                }
            },
        ) { Text("Get a free key: aistudio.google.com/apikey") }

        OutlinedTextField(
            value = uiState.apiKeyInput,
            onValueChange = actions::onApiKeyChange,
            label = { Text("Gemini API key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.modelsInput,
            onValueChange = actions::onModelsChange,
            label = { Text("Models, tried in order") },
            supportingText = { Text("When one model's free daily quota runs out, the next one takes over.") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = actions::onSaveConnection,
                enabled = uiState.keyCheck != KeyCheckState.Checking &&
                    (uiState.isConnectionDirty || uiState.apiKeyInput.isNotBlank()),
            ) { Text(if (uiState.isConnectionDirty) "Save and test" else "Test key") }
            TextButton(onClick = actions::onResetModels) { Text("Reset models") }
        }

        when (val check = uiState.keyCheck) {
            KeyCheckState.Idle -> Unit
            KeyCheckState.Checking -> Text("Checking the key…", style = MaterialTheme.typography.bodyMedium)
            is KeyCheckState.Valid -> Text(
                text = if (check.unavailableModels.isEmpty()) {
                    "Key works. You're ready to snap meals."
                } else {
                    "Key works, but these models aren't available to it and will be skipped: " +
                        check.unavailableModels.joinToString(", ")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            is KeyCheckState.Failed -> Text(
                text = check.error.userMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun GoalsCard(uiState: SettingsUiState, actions: SettingsActions) {
    SettingsCard("Daily goals") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalInput("Calories", uiState.kcalInput, Modifier.weight(1f)) { actions.onGoalInputChange(GoalField.KCAL, it) }
            GoalInput("Protein g", uiState.proteinInput, Modifier.weight(1f)) { actions.onGoalInputChange(GoalField.PROTEIN, it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalInput("Carbs g", uiState.carbsInput, Modifier.weight(1f)) { actions.onGoalInputChange(GoalField.CARBS, it) }
            GoalInput("Fat g", uiState.fatInput, Modifier.weight(1f)) { actions.onGoalInputChange(GoalField.FAT, it) }
        }
        Button(onClick = actions::onSaveGoals, enabled = uiState.isGoalsDirty) { Text("Save goals") }
    }
}

@Composable
private fun CalculatorCard(uiState: SettingsUiState, actions: SettingsActions) {
    SettingsCard("Goal calculator") {
        Text(
            text = "Estimates your daily needs with the Mifflin-St Jeor formula. Treat it as a starting point " +
                "and adjust after two weeks of tracking your weight.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Segmented(Sex.entries, uiState.sex, { it.label }, actions::onSexSelect)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberInput("Age", uiState.ageInput, KeyboardType.Number, Modifier.weight(1f), actions::onAgeChange)
            NumberInput("Height cm", uiState.heightInput, KeyboardType.Decimal, Modifier.weight(1f), actions::onHeightChange)
            NumberInput("Weight kg", uiState.weightInput, KeyboardType.Decimal, Modifier.weight(1f), actions::onWeightChange)
        }
        Text("Activity", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityLevel.entries.forEach { level ->
                FilterChip(
                    selected = uiState.activity == level,
                    onClick = { actions.onActivitySelect(level) },
                    label = { Text(level.label) },
                )
            }
        }
        Text("Goal", style = MaterialTheme.typography.labelLarge)
        Segmented(GoalType.entries, uiState.goalType, { it.label }, actions::onGoalTypeSelect)

        val suggested = uiState.suggestedGoals
        Text(
            text = if (suggested != null) {
                "Suggested: ${suggested.kcal} kcal · P ${suggested.protein} · C ${suggested.carbs} · F ${suggested.fat} g"
            } else {
                "Fill in age, height and weight to see a suggestion."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = actions::onApplySuggestedGoals, enabled = suggested != null) { Text("Use these goals") }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun GoalInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    NumberInput(label, value, KeyboardType.Number, modifier, onValueChange)
}

@Composable
private fun NumberInput(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label(option)) }
        }
    }
}
