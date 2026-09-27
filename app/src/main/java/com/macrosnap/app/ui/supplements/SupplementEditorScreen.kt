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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    SupplementEditorEffect.Close -> onClose()
                    is SupplementEditorEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    SupplementEditorContent(uiState = uiState, actions = viewModel, snackbarHostState = snackbarHostState, onClose = onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplementEditorContent(uiState: SupplementEditorUiState, snackbarHostState: SnackbarHostState, actions: SupplementEditorActions, onClose: () -> Unit) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isExisting) "Edit supplement" else "New supplement") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isExisting) {
                        IconButton(onClick = actions::onDeleteClick) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete supplement")
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
                    Button(onClick = actions::onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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
                label = "Describe it and let the AI fill this in",
                placeholder = "e.g. vitamin D3 2000 IU, fish oil 1000 mg",
                hint = "Gemini names it and fills in the content of one dose.",
            )
            uiState.notes?.let { notes ->
                Text("Note: $notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = actions::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.doseLabel,
                    onValueChange = actions::onDoseLabelChange,
                    label = { Text("One dose is") },
                    placeholder = { Text("5 g, 1 capsule…") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                )
                OutlinedTextField(
                    value = uiState.timesPerDayInput,
                    onValueChange = actions::onTimesPerDayChange,
                    label = { Text("Times a day") },
                    singleLine = true,
                    isError = uiState.timesPerDay == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Currently taking", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Turn off to keep it in the list without showing it on Today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = uiState.isActive, onCheckedChange = actions::onActiveChange)
            }

            Text("Content of one dose", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Only fill what it contains. Vitamin D: 1000 IU = 25 µg. Magnesium, zinc and calcium " +
                    "are the elemental amount, which labels usually state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NutrientFields(values = uiState.nutrientInputs, onValueChange = actions::onNutrientChange)
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = actions::onDeleteDismiss,
            title = { Text("Delete this supplement?") },
            text = { Text("Its history of taken doses is deleted too. To stop it without losing that, turn off \"Currently taking\" instead.") },
            confirmButton = {
                TextButton(onClick = actions::onDeleteConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = actions::onDeleteDismiss) { Text("Cancel") } },
        )
    }
}
