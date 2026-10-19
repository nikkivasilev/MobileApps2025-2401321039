package com.macrosnap.app.ui.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macrosnap.app.R
import com.macrosnap.app.ui.asString
import com.macrosnap.app.ui.components.DescribeField
import com.macrosnap.app.ui.components.NutrientFields

@Composable
fun SupplementEditorScreen(
    onClose: () -> Unit,
    viewModel: SupplementEditorViewModel = viewModel(factory = SupplementEditorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    SupplementEditorEffect.Close -> onClose()
                    is SupplementEditorEffect.ShowMessage -> snackbarHostState.showSnackbar(
                        effect.message.asString(context),
                    )
                }
            }
        }
    }

    SupplementEditorContent(
        uiState = uiState,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplementEditorContent(
    uiState: SupplementEditorUiState,
    snackbarHostState: SnackbarHostState,
    actions: SupplementEditorActions,
    onClose: () -> Unit,
) {
    val defaultDoseLabel = stringResource(R.string.supplements_default_dose)
    val activeLabel = stringResource(R.string.supplements_active)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isExisting) R.string.supplements_edit_title else R.string.supplements_new,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (uiState.isExisting) {
                        IconButton(onClick = actions::onDeleteClick) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.supplements_delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { actions.onSave(defaultDoseLabel) },
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DescribeField(
                value = uiState.describeInput,
                onValueChange = actions::onDescribeChange,
                onSubmit = actions::onDescribeSubmit,
                status = uiState.describeStatus,
                label = stringResource(R.string.supplements_describe_label),
                placeholder = stringResource(R.string.supplements_describe_placeholder),
                hint = stringResource(R.string.supplements_describe_hint),
            )
            uiState.notes?.let { notes ->
                Text(
                    stringResource(R.string.supplements_note, notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = actions::onNameChange,
                label = { Text(stringResource(R.string.supplements_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.doseLabel,
                    onValueChange = actions::onDoseLabelChange,
                    label = { Text(stringResource(R.string.supplements_dose)) },
                    placeholder = { Text(stringResource(R.string.supplements_dose_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                )
                OutlinedTextField(
                    value = uiState.timesPerDayInput,
                    onValueChange = actions::onTimesPerDayChange,
                    label = { Text(stringResource(R.string.supplements_times_per_day)) },
                    singleLine = true,
                    isError = uiState.timesPerDay == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(activeLabel, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.supplements_active_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = uiState.isActive, onCheckedChange = actions::onActiveChange)
            }

            Text(
                stringResource(R.string.supplements_dose_content),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.supplements_dose_content_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NutrientFields(values = uiState.nutrientInputs, onValueChange = actions::onNutrientChange)
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = actions::onDeleteDismiss,
            title = { Text(stringResource(R.string.supplements_delete_title)) },
            text = { Text(stringResource(R.string.supplements_delete_body, activeLabel)) },
            confirmButton = {
                TextButton(onClick = actions::onDeleteConfirm) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = actions::onDeleteDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
