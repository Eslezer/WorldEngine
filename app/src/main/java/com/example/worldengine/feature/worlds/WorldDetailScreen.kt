package com.example.worldengine.feature.worlds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.worldengine.domain.model.Character
import com.example.worldengine.feature.relationships.RelationshipsSection
import com.example.worldengine.feature.timeline.TimelineSection
import com.example.worldengine.ui.components.PlaceholderScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

private enum class WorldSection(val label: String, val implemented: Boolean) {
    Characters("Characters", true),
    Timeline("Timeline", false),
    Maps("Maps", false),
    Relationships("Relationships", false),
    Lore("Lore", false),
}

@Composable
fun WorldDetailScreen(
    worldId: Long,
    onAddCharacter: () -> Unit,
    onOpenCharacter: (Long) -> Unit,
    viewModel: WorldDetailViewModel = koinViewModel { parametersOf(worldId) },
) {
    val world by viewModel.world.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(WorldSection.Characters) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    world?.name ?: "World",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                world?.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }

            SectionChips(selected = section, onSelect = { section = it })

            when (section) {
                WorldSection.Characters -> CharactersSection(
                    characters = characters,
                    onOpenCharacter = onOpenCharacter,
                    onDeleteCharacter = viewModel::deleteCharacter,
                )
                WorldSection.Timeline -> TimelineSection(worldId = worldId)
                WorldSection.Relationships -> RelationshipsSection(worldId = worldId)
                else -> PlaceholderScreen("${section.label} — inside ${world?.name ?: "this world"}")
            }
        }

        if (section == WorldSection.Characters) {
            FloatingActionButton(
                onClick = onAddCharacter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "New character")
            }
        }
    }
}

@Composable
private fun SectionChips(selected: WorldSection, onSelect: (WorldSection) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorldSection.entries.forEach { entry ->
            FilterChip(
                selected = entry == selected,
                onClick = { onSelect(entry) },
                label = { Text(entry.label) },
            )
        }
    }
}

@Composable
private fun CharactersSection(
    characters: List<Character>,
    onOpenCharacter: (Long) -> Unit,
    onDeleteCharacter: (Character) -> Unit,
) {
    if (characters.isEmpty()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No characters yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add a character with the + button. You can generate a portrait for them with NovelAI.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(characters, key = { it.id }) { character ->
            CharacterCard(
                character = character,
                onOpen = { onOpenCharacter(character.id) },
                onDelete = { onDeleteCharacter(character) },
            )
        }
    }
}

@Composable
private fun CharacterCard(character: Character, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val portrait = character.portraitPath
            if (portrait != null) {
                AsyncImage(
                    model = File(portrait),
                    contentDescription = "${character.name} portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    character.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (character.role.isNotBlank()) {
                    Text(character.role, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete character")
            }
        }
    }
}
