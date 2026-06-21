package com.example.worldengine.feature.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.ui.components.LabeledDropdown
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
    val loreCategories by viewModel.loreCategories.collectAsStateWithLifecycle()
    val loreEntries by viewModel.loreEntries.collectAsStateWithLifecycle()

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

        LoreLinksSection(
            categories = loreCategories,
            entries = loreEntries,
            linkedIds = state.linkedLoreEntryIds,
            onToggle = viewModel::onLoreEntryToggled,
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

@Composable
private fun LoreLinksSection(
    categories: List<LoreCategory>,
    entries: List<LoreEntry>,
    linkedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    if (categories.isEmpty()) return

    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
    val entriesInCategory = entries.filter { it.categoryId == selectedCategory.id }
    val linkedNames = entries.filter { it.id in linkedIds }.map { it.title }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Lore links", style = MaterialTheme.typography.titleMedium)
            Text(
                "Connect this character to places, factions, cultures, magic systems or other codex entries.",
                style = MaterialTheme.typography.bodySmall,
            )

            LabeledDropdown(
                label = "Category",
                options = categories,
                selected = selectedCategory,
                optionLabel = { it.name },
                onSelected = { selectedCategoryId = it.id },
            )

            if (entriesInCategory.isEmpty()) {
                Text(
                    "No entries in ${selectedCategory.name} yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entriesInCategory.forEach { entry ->
                        FilterChip(
                            selected = entry.id in linkedIds,
                            onClick = { onToggle(entry.id) },
                            label = { Text(entry.title) },
                        )
                    }
                }
            }

            if (linkedNames.isNotEmpty()) {
                Text(
                    "Linked: ${linkedNames.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
