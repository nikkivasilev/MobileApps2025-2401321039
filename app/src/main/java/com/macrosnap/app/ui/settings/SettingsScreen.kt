package com.macrosnap.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.model.ActivityLevel
import com.macrosnap.app.model.GoalType
import com.macrosnap.app.model.Sex
import com.macrosnap.app.ui.AppLanguage
import com.macrosnap.app.ui.SupplementEditorRoute
import com.macrosnap.app.ui.asString
import com.macrosnap.app.ui.components.AppCard
import com.macrosnap.app.ui.components.CardHeader
import com.macrosnap.app.ui.components.NutrientFields
import com.macrosnap.app.ui.components.appChipColors
import com.macrosnap.app.ui.label
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.theme.AppTheme
import com.macrosnap.app.ui.userMessage

private const val API_KEY_URL = "https://aistudio.google.com/apikey"

@Composable
fun SettingsScreen(
    onOpenSupplementEditor: (SupplementEditorRoute) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
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
            LanguageCard()
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
private fun LanguageCard() {
    // AppCompat stores the choice and recreates the activity, so there's nothing to keep in the ViewModel.
    var selected by remember { mutableStateOf(AppLanguage.current()) }

    SettingsCard(stringResource(R.string.language_title), AppIcons.Language, MaterialTheme.colorScheme.onSurface) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    colors = appChipColors(),
                    shape = CircleShape,
                    selected = selected == language,
                    onClick = {
                        if (language != selected) {
                            selected = language
                            AppLanguage.set(language)
                        }
                    },
                    label = { Text(stringResource(language.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun SupplementsCard(uiState: SettingsUiState, onOpenSupplementEditor: (SupplementEditorRoute) -> Unit) {
    SettingsCard(stringResource(R.string.settings_supplements_title), AppIcons.Pill, AppTheme.colors.micros) {
        if (uiState.supplements.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_supplements_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val paused = stringResource(R.string.settings_supplement_paused)
        uiState.supplements.forEach { supplement ->
            val timesPerDay = stringResource(R.string.settings_supplement_times_per_day, supplement.timesPerDay)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onOpenSupplementEditor(SupplementEditorRoute(supplement.id)) }
                    .background(AppTheme.colors.tile)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(supplement.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = buildString {
                            append(supplement.doseLabel)
                            if (supplement.timesPerDay > 1) append(" · $timesPerDay")
                            if (!supplement.isActive) append(" · $paused")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(onClick = { onOpenSupplementEditor(SupplementEditorRoute()) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.settings_add_supplement), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun WellnessCard(uiState: SettingsUiState, actions: SettingsActions) {
    SettingsCard(stringResource(R.string.settings_wellness_title), AppIcons.WaterDrop, AppTheme.colors.water) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberInput(
                stringResource(R.string.settings_water_goal),
                uiState.waterGoalInput,
                KeyboardType.Number,
                Modifier.weight(1f),
                actions::onWaterGoalChange,
            )
            NumberInput(
                stringResource(R.string.settings_sleep_goal),
                uiState.sleepGoalInput,
                KeyboardType.Decimal,
                Modifier.weight(1f),
                actions::onSleepGoalChange,
            )
        }
        NumberInput(
            stringResource(R.string.settings_rest_timer),
            uiState.restSecondsInput,
            KeyboardType.Number,
            Modifier.fillMaxWidth(),
            actions::onRestSecondsChange,
        )
        Button(onClick = actions::onSaveWellness, enabled = uiState.isWellnessDirty) {
            Text(stringResource(R.string.action_save))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_workout_burn_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_workout_burn_body),
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
    SettingsCard(stringResource(R.string.settings_nutrients_title), AppIcons.Leaf, AppTheme.colors.micros) {
        Text(
            text = stringResource(R.string.settings_nutrients_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NutrientFields(values = uiState.nutrientInputs, onValueChange = actions::onNutrientTargetChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions::onSaveNutrientTargets, enabled = uiState.isNutrientTargetsDirty) {
                Text(stringResource(R.string.settings_save_targets))
            }
            TextButton(onClick = actions::onResetNutrientTargets) {
                Text(stringResource(R.string.settings_reset_defaults))
            }
        }
    }
}

@Composable
private fun ConnectionCard(uiState: SettingsUiState, actions: SettingsActions) {
    val context = LocalContext.current
    var showKey by rememberSaveable { mutableStateOf(false) }

    SettingsCard(stringResource(R.string.settings_ai_title), AppIcons.Sparkle, MaterialTheme.colorScheme.onSurface) {
        Text(
            text = stringResource(R.string.settings_ai_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(API_KEY_URL)))
                } catch (ignored: ActivityNotFoundException) {
                    // No browser installed; the URL is still visible on the button.
                }
            },
        ) { Text(stringResource(R.string.settings_get_key)) }

        OutlinedTextField(
            value = uiState.apiKeyInput,
            onValueChange = actions::onApiKeyChange,
            label = { Text(stringResource(R.string.settings_api_key)) },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = {
                    showKey = !showKey
                }) { Text(stringResource(if (showKey) R.string.settings_hide_key else R.string.settings_show_key)) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.modelsInput,
            onValueChange = actions::onModelsChange,
            label = { Text(stringResource(R.string.settings_models)) },
            supportingText = { Text(stringResource(R.string.settings_models_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = actions::onSaveConnection,
                enabled = uiState.keyCheck != KeyCheckState.Checking &&
                    (uiState.isConnectionDirty || uiState.apiKeyInput.isNotBlank()),
            ) {
                Text(
                    stringResource(
                        if (uiState.isConnectionDirty) R.string.settings_save_and_test else R.string.settings_test_key,
                    ),
                )
            }
            TextButton(onClick = actions::onResetModels) { Text(stringResource(R.string.settings_reset_models)) }
        }

        when (val check = uiState.keyCheck) {
            KeyCheckState.Idle -> Unit
            KeyCheckState.Checking -> Text(
                stringResource(R.string.settings_checking_key),
                style = MaterialTheme.typography.bodyMedium,
            )
            is KeyCheckState.Valid -> Text(
                text = if (check.unavailableModels.isEmpty()) {
                    stringResource(R.string.settings_key_valid)
                } else {
                    stringResource(R.string.settings_key_valid_skipped, check.unavailableModels.joinToString(", "))
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
    SettingsCard(stringResource(R.string.settings_goals_title), AppIcons.Flame, AppTheme.colors.calories) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalInput(stringResource(R.string.calories), uiState.kcalInput, Modifier.weight(1f)) {
                actions.onGoalInputChange(GoalField.KCAL, it)
            }
            GoalInput(stringResource(R.string.settings_goal_protein), uiState.proteinInput, Modifier.weight(1f)) {
                actions.onGoalInputChange(GoalField.PROTEIN, it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalInput(stringResource(R.string.settings_goal_carbs), uiState.carbsInput, Modifier.weight(1f)) {
                actions.onGoalInputChange(GoalField.CARBS, it)
            }
            GoalInput(stringResource(R.string.settings_goal_fat), uiState.fatInput, Modifier.weight(1f)) {
                actions.onGoalInputChange(GoalField.FAT, it)
            }
        }
        Button(onClick = actions::onSaveGoals, enabled = uiState.isGoalsDirty) {
            Text(stringResource(R.string.settings_save_goals))
        }
    }
}

@Composable
private fun CalculatorCard(uiState: SettingsUiState, actions: SettingsActions) {
    SettingsCard(
        stringResource(R.string.settings_calculator_title),
        Icons.Filled.Person,
        MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = stringResource(R.string.settings_calculator_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Segmented(Sex.entries, uiState.sex, { it.label }, actions::onSexSelect)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberInput(
                stringResource(R.string.settings_age),
                uiState.ageInput,
                KeyboardType.Number,
                Modifier.weight(1f),
                actions::onAgeChange,
            )
            NumberInput(
                stringResource(R.string.settings_height),
                uiState.heightInput,
                KeyboardType.Decimal,
                Modifier.weight(1f),
                actions::onHeightChange,
            )
            NumberInput(
                stringResource(R.string.settings_weight),
                uiState.weightInput,
                KeyboardType.Decimal,
                Modifier.weight(1f),
                actions::onWeightChange,
            )
        }
        Text(stringResource(R.string.settings_activity), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityLevel.entries.forEach { level ->
                FilterChip(
                    colors = appChipColors(),
                    shape = CircleShape,
                    selected = uiState.activity == level,
                    onClick = { actions.onActivitySelect(level) },
                    label = { Text(level.label) },
                )
            }
        }
        Text(stringResource(R.string.settings_goal), style = MaterialTheme.typography.labelLarge)
        Segmented(GoalType.entries, uiState.goalType, { it.label }, actions::onGoalTypeSelect)

        val suggested = uiState.suggestedGoals
        Text(
            text = if (suggested != null) {
                stringResource(
                    R.string.settings_suggested,
                    suggested.kcal,
                    suggested.protein,
                    suggested.carbs,
                    suggested.fat,
                )
            } else {
                stringResource(R.string.settings_suggested_empty)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = actions::onApplySuggestedGoals, enabled = suggested != null) {
            Text(stringResource(R.string.settings_use_goals))
        }
    }
}

@Composable
private fun SettingsCard(title: String, icon: ImageVector, tint: Color, content: @Composable () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(title = title, icon = icon, tint = tint)
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
private fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
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
