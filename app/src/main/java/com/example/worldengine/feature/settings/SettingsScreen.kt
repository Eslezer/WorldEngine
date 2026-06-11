package com.example.worldengine.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("NovelAI API Key", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Paste your NovelAI persistent token (starts with \"pst-\"). It is stored " +
                        "encrypted on this device only and is never committed to source control.",
                    style = MaterialTheme.typography.bodySmall,
                )

                val statusText = if (state.hasSavedKey) "A key is currently saved." else "No key saved yet."
                Text(statusText, style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = state.apiKeyInput,
                    onValueChange = viewModel::onApiKeyChange,
                    label = { Text(if (state.hasSavedKey) "Replace key" else "API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = viewModel::saveApiKey,
                        enabled = state.apiKeyInput.isNotBlank(),
                    ) { Text("Save") }

                    if (state.hasSavedKey) {
                        OutlinedButton(onClick = viewModel::clearApiKey) { Text("Clear") }
                    }
                }

                if (state.savedConfirmation) {
                    Text(
                        "Key saved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
