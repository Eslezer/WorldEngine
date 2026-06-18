package com.example.worldengine.feature.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@Composable
fun CharacterEditorScreen(
    worldId: Long,
    characterId: Long,
    onDone: () -> Unit,
    viewModel: CharacterEditorViewModel = koinViewModel { parametersOf(worldId, characterId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Close the screen once the save has committed.
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.role,
            onValueChange = viewModel::onRoleChange,
            label = { Text("Role / title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        PortraitSection(state)

        Button(
            onClick = viewModel::save,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isNew) "Create character" else "Save changes")
        }
    }
}

/**
 * Read-only portrait display. Portraits are produced in the Image Generation tab and pushed onto a
 * character from there (see [com.example.worldengine.feature.imagelab.ImageLabScreen]); this section
 * only shows the result and points the user to where it's set.
 */
@Composable
private fun PortraitSection(state: CharacterEditorUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Portrait", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
                contentAlignment = Alignment.Center,
            ) {
                if (state.portraitPath != null) {
                    AsyncImage(
                        model = File(state.portraitPath),
                        contentDescription = "Character portrait",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        "No portrait yet. Generate an image in the Image Generation tab, then assign " +
                            "it to this character there.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
