package com.example.worldengine.feature.relationships

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CharacterLoreLink
import com.example.worldengine.domain.model.CharacterRelationship
import com.example.worldengine.domain.model.CustomRelationshipType
import com.example.worldengine.domain.model.HierarchyKind
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.model.RelationshipCategory
import com.example.worldengine.domain.model.RelationshipStructure
import com.example.worldengine.domain.model.RelationshipTone
import com.example.worldengine.ui.components.LabeledDropdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Relationships section inside a world. Shows the relationship graph in one of three layouts —
 * **Web** (ring), **Tree** (layered top-down by one-way edges) and **Pyramid** (the same, narrowing
 * to an apex) — or, when a character is focused, an ego view centred on them (parents/superiors
 * above, children/subordinates below, mutual links to the sides). Characters with no relationships
 * are omitted from the graph. Nodes show the character's portrait when they have one. Self-contained:
 * owns its [RelationshipViewModel], FAB and dialog.
 */
@Composable
fun RelationshipsSection(
    worldId: Long,
    viewModel: RelationshipViewModel = koinViewModel { parametersOf(worldId) },
) {
    val relationships by viewModel.relationships.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val customTypes by viewModel.customTypes.collectAsStateWithLifecycle()
    val typeOptions by viewModel.typeOptions.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val loreCategories by viewModel.loreCategories.collectAsStateWithLifecycle()
    val loreEntries by viewModel.loreEntries.collectAsStateWithLifecycle()
    val characterLoreLinks by viewModel.characterLoreLinks.collectAsStateWithLifecycle()
    val filterCategoryId by viewModel.filterCategoryId.collectAsStateWithLifecycle()
    val filterEntryId by viewModel.filterEntryId.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val selectedCharacterId by viewModel.selectedCharacterId.collectAsStateWithLifecycle()
    val centeredCharacterId by viewModel.centeredCharacterId.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val lastDeleted by viewModel.lastDeleted.collectAsStateWithLifecycle()

    val characterNames = remember(characters) { characters.associate { it.id to it.name } }
    val typesById = remember(customTypes) { customTypes.associateBy { it.id } }
    val visibleCharacters = remember(characters, characterLoreLinks, loreEntries, filterCategoryId, filterEntryId) {
        characters.filterByLore(characterLoreLinks, loreEntries, filterCategoryId, filterEntryId)
    }
    val visibleIds = remember(visibleCharacters) { visibleCharacters.mapTo(HashSet()) { it.id } }
    val visibleRelationships = remember(relationships, visibleIds) {
        relationships.filter { it.fromCharacterId in visibleIds && it.toCharacterId in visibleIds }
    }
    val graphRelationships = remember(visibleRelationships, viewMode, typesById) {
        visibleRelationships.forGraphMode(viewMode, typesById)
    }
    // Only characters that take part in at least one relationship belong in the graph.
    val connected = remember(visibleCharacters, graphRelationships) {
        visibleCharacters.filter { c ->
            graphRelationships.any { it.fromCharacterId == c.id || it.toCharacterId == c.id }
        }
    }
    val portraits = rememberPortraits(connected)
    val enoughCharacters = characters.size >= 2

    Box(modifier = Modifier.fillMaxSize()) {
        if (!enoughCharacters) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add at least two characters", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Relationships connect characters together. Create two or more characters in this " +
                        "world first, then link them here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ViewModeChips(selected = viewMode, onSelect = viewModel::setViewMode) }
                item {
                    CharacterCenterSearch(
                        characters = visibleCharacters,
                        centeredCharacterId = centeredCharacterId,
                        onCenter = viewModel::centerCharacter,
                    )
                }
                item {
                    LoreRelationshipFilter(
                        categories = loreCategories,
                        entries = loreEntries,
                        links = characterLoreLinks,
                        selectedCategoryId = filterCategoryId,
                        selectedEntryId = filterEntryId,
                        onCategorySelect = viewModel::setFilterCategory,
                        onEntrySelect = viewModel::setFilterEntry,
                    )
                }
                item {
                    if (graphRelationships.isEmpty()) {
                        Text(
                            if (filterCategoryId == null) {
                                emptyGraphMessage(viewMode)
                            } else {
                                "No ${viewMode.graphLabel()} relationships match this lore filter yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        RelationshipGraph(
                            characters = connected,
                            relationships = graphRelationships,
                            typesById = typesById,
                            mode = viewMode,
                            focusId = selectedCharacterId ?: centeredCharacterId,
                            centerId = centeredCharacterId,
                            portraits = portraits,
                            onCharacterClick = viewModel::selectCharacter,
                        )
                    }
                }
                items(visibleRelationships, key = { it.id }) { relationship ->
                    RelationshipCard(
                        relationship = relationship,
                        characterNames = characterNames,
                        typesById = typesById,
                        onEdit = { viewModel.startEdit(relationship) },
                        onDelete = { viewModel.askDelete(relationship) },
                    )
                }
            }
        }

        if (enoughCharacters) {
            FloatingActionButton(
                onClick = viewModel::startCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add relationship")
            }
        }
    }

    draft?.let {
        RelationshipEditorDialog(
            draft = it,
            characters = characters,
            typeOptions = typeOptions,
            onFromSelect = viewModel::onFromSelect,
            onToSelect = viewModel::onToSelect,
            onTypeOptionSelect = viewModel::onTypeOptionSelect,
            onLabelChange = viewModel::onLabelChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissDraft,
        )
    }

    selectedCharacterId?.let { characterId ->
        val selected = characters.firstOrNull { it.id == characterId }
        if (selected != null) {
            CharacterRelationshipsDialog(
                character = selected,
                characters = characters,
                relationships = relationships,
                typesById = typesById,
                lastDeleted = lastDeleted,
                onEdit = viewModel::startEdit,
                onAskDelete = viewModel::askDelete,
                onAdd = { other -> viewModel.startCreateBetween(selected.id, other.id) },
                onUndoDelete = viewModel::restoreLastDeleted,
                onDismiss = { viewModel.selectCharacter(null) },
            )
        }
    }

    pendingDelete?.let { relationship ->
        DeleteRelationshipDialog(
            relationship = relationship,
            characterNames = characterNames,
            typesById = typesById,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun CharacterRelationshipsDialog(
    character: Character,
    characters: List<Character>,
    relationships: List<CharacterRelationship>,
    typesById: Map<String, CustomRelationshipType>,
    lastDeleted: CharacterRelationship?,
    onEdit: (CharacterRelationship) -> Unit,
    onAskDelete: (CharacterRelationship) -> Unit,
    onAdd: (Character) -> Unit,
    onUndoDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val others = remember(characters, character.id) {
        characters.filter { it.id != character.id }.sortedBy { it.name.lowercase() }
    }
    val relationshipsByOther = remember(relationships, character.id) {
        relationships
            .filter { it.fromCharacterId == character.id || it.toCharacterId == character.id }
            .flatMap { rel ->
                val otherId = if (rel.fromCharacterId == character.id) rel.toCharacterId else rel.fromCharacterId
                listOf(otherId to rel)
            }
            .groupBy({ it.first }, { it.second })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${character.name}'s relationships") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                others.forEach { other ->
                    CharacterRelationshipOverviewRow(
                        characterId = character.id,
                        other = other,
                        relationships = relationshipsByOther[other.id].orEmpty(),
                        typesById = typesById,
                        onEdit = onEdit,
                        onAskDelete = onAskDelete,
                        onAdd = { onAdd(other) },
                    )
                }
                if (lastDeleted != null) {
                    TextButton(onClick = onUndoDelete) {
                        Text("Restore last deleted relationship")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun CharacterRelationshipOverviewRow(
    characterId: Long,
    other: Character,
    relationships: List<CharacterRelationship>,
    typesById: Map<String, CustomRelationshipType>,
    onEdit: (CharacterRelationship) -> Unit,
    onAskDelete: (CharacterRelationship) -> Unit,
    onAdd: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(other.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add relationship with ${other.name}")
                }
            }
            if (relationships.isEmpty()) {
                Text("None", style = MaterialTheme.typography.bodyMedium)
            } else {
                relationships.forEach { relationship ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            relationshipOverviewText(characterId, relationship, typesById),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onEdit(relationship) }) { Text("Edit") }
                        IconButton(onClick = { onAskDelete(relationship) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete relationship")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteRelationshipDialog(
    relationship: CharacterRelationship,
    characterNames: Map<Long, String>,
    typesById: Map<String, CustomRelationshipType>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val from = characterNames[relationship.fromCharacterId] ?: "?"
    val to = characterNames[relationship.toCharacterId] ?: "?"
    val typeName = relationship.customTypeId?.let { typesById[it]?.name } ?: relationship.type.label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete relationship?") },
        text = { Text("$from / $to · $typeName") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun relationshipOverviewText(
    selectedCharacterId: Long,
    relationship: CharacterRelationship,
    typesById: Map<String, CustomRelationshipType>,
): String {
    val typeName = relationship.customTypeId?.let { typesById[it]?.name } ?: relationship.type.label
    val structure = relationship.structure(typesById)
    val oriented = when {
        structure == RelationshipStructure.PEER -> typeName
        relationship.fromCharacterId == selectedCharacterId -> typeName
        structure == RelationshipStructure.OVER -> "${typeName} (under)"
        else -> "${typeName} (over)"
    }
    return if (relationship.label.isBlank()) oriented else "$oriented · ${relationship.label}"
}

@Composable
private fun ViewModeChips(selected: RelationshipViewMode, onSelect: (RelationshipViewMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RelationshipViewMode.entries.forEach { mode ->
            FilterChip(selected = mode == selected, onClick = { onSelect(mode) }, label = { Text(mode.label) })
        }
    }
}

@Composable
private fun CharacterCenterSearch(
    characters: List<Character>,
    centeredCharacterId: Long?,
    onCenter: (Long?) -> Unit,
) {
    var query by remember(centeredCharacterId, characters) {
        mutableStateOf(characters.firstOrNull { it.id == centeredCharacterId }?.name.orEmpty())
    }
    val suggestions = remember(query, characters) {
        val q = query.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            characters
                .filter { it.name.contains(q, ignoreCase = true) }
                .take(5)
        }
    }

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isBlank()) onCenter(null)
                },
                label = { Text("Center on character") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { character ->
                    FilterChip(
                        selected = character.id == centeredCharacterId,
                        onClick = {
                            query = character.name
                            onCenter(character.id)
                        },
                        label = { Text(character.name) },
                    )
                }
                if (centeredCharacterId != null) {
                    TextButton(
                        onClick = {
                            query = ""
                            onCenter(null)
                        },
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoreRelationshipFilter(
    categories: List<LoreCategory>,
    entries: List<LoreEntry>,
    links: List<CharacterLoreLink>,
    selectedCategoryId: String?,
    selectedEntryId: Long?,
    onCategorySelect: (String?) -> Unit,
    onEntrySelect: (Long?) -> Unit,
) {
    val categoryOptions = remember(categories) { listOf<LoreCategory?>(null) + categories }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val entriesForCategory = remember(entries, selectedCategoryId) {
        if (selectedCategoryId == null) emptyList() else entries.filter { it.categoryId == selectedCategoryId }
    }
    val entryOptions = remember(entriesForCategory) { listOf<LoreEntry?>(null) + entriesForCategory }
    val selectedEntry = entriesForCategory.firstOrNull { it.id == selectedEntryId }
    val linkCounts = remember(links) { links.groupingBy { it.loreEntryId }.eachCount() }

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lore filter", style = MaterialTheme.typography.titleMedium)
            LabeledDropdown(
                label = "Category",
                options = categoryOptions,
                selected = selectedCategory,
                optionLabel = { it?.name ?: "All categories" },
                onSelected = { onCategorySelect(it?.id) },
            )
            if (selectedCategory != null) {
                if (entriesForCategory.isEmpty()) {
                    Text(
                        "No ${selectedCategory.name.lowercase()} entries yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LabeledDropdown(
                        label = selectedCategory.name,
                        options = entryOptions,
                        selected = selectedEntry,
                        optionLabel = { entry ->
                            if (entry == null) {
                                "All ${selectedCategory.name}"
                            } else {
                                "${entry.title} (${linkCounts[entry.id] ?: 0})"
                            }
                        },
                        onSelected = { onEntrySelect(it?.id) },
                    )
                }
            }
        }
    }
}

private fun List<Character>.filterByLore(
    links: List<CharacterLoreLink>,
    entries: List<LoreEntry>,
    selectedCategoryId: String?,
    selectedEntryId: Long?,
): List<Character> {
    if (selectedCategoryId == null && selectedEntryId == null) return this
    val entryIds = if (selectedEntryId != null) {
        setOf(selectedEntryId)
    } else {
        entries.filter { it.categoryId == selectedCategoryId }.mapTo(HashSet()) { it.id }
    }
    val characterIds = links
        .filter { it.loreEntryId in entryIds }
        .mapTo(HashSet()) { it.characterId }
    return filter { it.id in characterIds }
}

private fun List<CharacterRelationship>.forGraphMode(
    mode: RelationshipViewMode,
    typesById: Map<String, CustomRelationshipType>,
): List<CharacterRelationship> = when (mode) {
    RelationshipViewMode.Web -> this
    RelationshipViewMode.Familial -> filter { it.category(typesById) == RelationshipCategory.FAMILIAL }
    RelationshipViewMode.Factional -> filter { it.category(typesById) == RelationshipCategory.FACTIONAL }
    RelationshipViewMode.Social -> filter { it.category(typesById) == RelationshipCategory.SOCIAL }
}

private fun CharacterRelationship.category(typesById: Map<String, CustomRelationshipType>): RelationshipCategory =
    customTypeId?.let { typesById[it]?.category } ?: type.category

private fun CharacterRelationship.structure(typesById: Map<String, CustomRelationshipType>): RelationshipStructure =
    customTypeId?.let { typesById[it]?.structure } ?: type.structure

private fun CharacterRelationship.hierarchy(typesById: Map<String, CustomRelationshipType>): HierarchyKind =
    customTypeId?.let { typesById[it]?.hierarchy } ?: type.hierarchy

private fun RelationshipViewMode.graphLabel(): String = when (this) {
    RelationshipViewMode.Web -> "web"
    RelationshipViewMode.Familial -> "familial"
    RelationshipViewMode.Factional -> "factional"
    RelationshipViewMode.Social -> "social"
}

private fun emptyGraphMessage(mode: RelationshipViewMode): String = when (mode) {
    RelationshipViewMode.Web -> "No relationships yet. Use the + button to connect two characters."
    RelationshipViewMode.Familial -> "No familial relationships yet. Add Partner, Family or Parent of links to build this view."
    RelationshipViewMode.Factional -> "No factional relationships yet. Add Superior of or faction custom links to build this view."
    RelationshipViewMode.Social -> "No social relationships yet. Add Friend, Rival, Enemy or social custom links to build this view."
}

@Composable
private fun RelationshipGraph(
    characters: List<Character>,
    relationships: List<CharacterRelationship>,
    typesById: Map<String, CustomRelationshipType>,
    mode: RelationshipViewMode,
    focusId: Long?,
    centerId: Long?,
    portraits: Map<Long, ImageBitmap>,
    onCharacterClick: (Long) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val nodeColor = MaterialTheme.colorScheme.primaryContainer
    val nodeBorder = MaterialTheme.colorScheme.primary
    val focusBorder = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)

    val levels = remember(characters, relationships, mode, typesById) {
        when (mode) {
            RelationshipViewMode.Familial -> computeLevels(characters, relationships, HierarchyKind.FAMILY, typesById)
            RelationshipViewMode.Factional -> computeLevels(characters, relationships, HierarchyKind.ORG, typesById)
            RelationshipViewMode.Web, RelationshipViewMode.Social -> emptyMap()
        }
    }
    val maxLevel = levels.values.maxOrNull() ?: 0
    val edgeOffset = remember(relationships) { computeEdgeOffsets(relationships) }
    var nodePositions by remember { mutableStateOf<Map<Long, Offset>>(emptyMap()) }

    val canvasHeight = when (mode) {
        RelationshipViewMode.Web, RelationshipViewMode.Social -> 300.dp
        else -> (140 + maxLevel * 90).dp
    }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = "Relationship ${mode.label.lowercase()}" + if (focusId != null) " (focused)" else ""
            Text(title, style = MaterialTheme.typography.titleMedium)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .pointerInput(nodePositions) {
                        detectTapGestures { tap ->
                            val hit = nodePositions.minByOrNull { (_, pos) ->
                                hypot((tap.x - pos.x).toDouble(), (tap.y - pos.y).toDouble())
                            }?.takeIf { (_, pos) ->
                                hypot((tap.x - pos.x).toDouble(), (tap.y - pos.y).toDouble()) <= 36.0
                            }
                            hit?.let { (id, _) -> onCharacterClick(id) }
                        }
                    },
            ) {
                if (characters.isEmpty()) return@Canvas
                val nodeRadius = 18.dp.toPx()
                val positions: Map<Long, Offset> = when (mode) {
                    RelationshipViewMode.Web, RelationshipViewMode.Social ->
                        circularPositions(characters, size.width, size.height, centerId)
                    RelationshipViewMode.Familial ->
                        layeredPositions(characters, levels, maxLevel, size.width, size.height, pyramid = false, centerId = centerId)
                    RelationshipViewMode.Factional ->
                        layeredPositions(characters, levels, maxLevel, size.width, size.height, pyramid = true, centerId = centerId)
                }
                nodePositions = positions

                val spacing = 14.dp.toPx()
                val headSize = 9.dp.toPx()
                relationships.forEach { rel ->
                    val fromNode = positions[rel.fromCharacterId] ?: return@forEach
                    val toNode = positions[rel.toCharacterId] ?: return@forEach
                    // Lane direction uses a consistent pair orientation (low id → high id) so that
                    // opposite-direction edges between the same pair land in different lanes.
                    val lowPos = positions[minOf(rel.fromCharacterId, rel.toCharacterId)] ?: fromNode
                    val highPos = positions[maxOf(rel.fromCharacterId, rel.toCharacterId)] ?: toNode
                    val custom = rel.customTypeId?.let { typesById[it] }
                    val color = custom?.color ?: rel.type.color
                    val tone = custom?.tone ?: rel.type.tone
                    val structure = rel.structure(typesById)
                    val drawFrom = if (structure == RelationshipStructure.UNDER) toNode else fromNode
                    val drawTo = if (structure == RelationshipStructure.UNDER) fromNode else toNode
                    drawRelationshipEdge(
                        from = drawFrom,
                        to = drawTo,
                        lowPos = lowPos,
                        highPos = highPos,
                        offsetPx = (edgeOffset[rel.id] ?: 0f) * spacing,
                        color = color,
                        tone = tone,
                        structure = structure,
                        nodeRadius = nodeRadius,
                        headSize = headSize,
                    )
                }

                characters.forEach { character ->
                    val pos = positions[character.id] ?: return@forEach
                    val portrait = portraits[character.id]
                    if (portrait != null) {
                        drawCircularImage(portrait, pos, nodeRadius)
                    } else {
                        drawCircle(color = nodeColor, radius = nodeRadius, center = pos)
                    }
                    val border = if (character.id == focusId) focusBorder else nodeBorder
                    val borderWidth = if (character.id == focusId) 4.dp.toPx() else 2.dp.toPx()
                    drawCircle(color = border, radius = nodeRadius, center = pos, style = Stroke(width = borderWidth))
                    val layout = textMeasurer.measure(character.name, style = labelStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(pos.x - layout.size.width / 2f, pos.y + nodeRadius + 2.dp.toPx()),
                    )
                }
            }
            RelationshipLegend()
        }
    }
}

/** Explains the line styles (tone) and arrowheads (direction) used in the graph. */
@Composable
private fun RelationshipLegend() {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Legend", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendLine(RelationshipTone.POSITIVE, "Friendly", lineColor)
            LegendLine(RelationshipTone.NEUTRAL, "Neutral", lineColor)
            LegendLine(RelationshipTone.HOSTILE, "Hostile", lineColor)
        }
        Text(
            "Plain lines are peers (=). Arrows point from over to under. Familial and factional " +
                "views use rows; social uses a web.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LegendLine(tone: RelationshipTone, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(width = 32.dp, height = 10.dp)) {
            val y = size.height / 2f
            drawRelationshipLine(Offset(0f, y), Offset(size.width, y), color, tone, 3.dp.toPx())
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Decodes each character's portrait file (downscaled, center-crop-friendly) into an ImageBitmap. */
@Composable
private fun rememberPortraits(characters: List<Character>): Map<Long, ImageBitmap> {
    val paths = characters.mapNotNull { c -> c.portraitPath?.let { c.id to it } }
    return produceState(emptyMap<Long, ImageBitmap>(), paths) {
        value = withContext(Dispatchers.IO) {
            paths.mapNotNull { (id, path) ->
                runCatching { decodeDownscaled(path, 192)?.asImageBitmap() }.getOrNull()?.let { id to it }
            }.toMap()
        }
    }.value
}

private fun decodeDownscaled(path: String, target: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val minSide = min(bounds.outWidth, bounds.outHeight)
    if (minSide <= 0) return null
    var sample = 1
    while (minSide / (sample * 2) >= target) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

/** Even ring layout, optionally pulling one character into the centre. */
private fun circularPositions(characters: List<Character>, w: Float, h: Float, centerId: Long?): Map<Long, Offset> {
    val n = characters.size
    val center = Offset(w / 2f, h / 2f)
    val centered = characters.firstOrNull { it.id == centerId }
    if (centered != null) {
        val ring = characters.filter { it.id != centerId }
        if (ring.isEmpty()) return mapOf(centered.id to center)
        val radius = min(w, h) / 2f * 0.72f
        return buildMap {
            put(centered.id, center)
            ring.forEachIndexed { i, c ->
                val angle = (2.0 * Math.PI * i / ring.size) - Math.PI / 2.0
                put(c.id, Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat()))
            }
        }
    }
    val radius = min(w, h) / 2f * 0.74f
    return characters.mapIndexed { i, c ->
        val angle = (2.0 * Math.PI * i / n) - Math.PI / 2.0
        c.id to Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }.toMap()
}

/** Layered top-down layout used by Tree and Pyramid (rows narrow toward the top when [pyramid]). */
private fun layeredPositions(
    characters: List<Character>,
    levels: Map<Long, Int>,
    maxLevel: Int,
    w: Float,
    h: Float,
    pyramid: Boolean,
    centerId: Long?,
): Map<Long, Offset> {
    val byLevel = characters.groupBy { levels[it.id] ?: 0 }
    val rowHeight = h / (maxLevel + 1)
    val out = HashMap<Long, Offset>(characters.size)
    byLevel.forEach { (level, rowChars) ->
        val y = rowHeight * (level + 0.5f)
        val rowWidth = if (pyramid) w * (level + 1f) / (maxLevel + 1f) else w
        val startX = (w - rowWidth) / 2f
        val ordered = rowChars.centerOrdered(centerId)
        ordered.forEachIndexed { i, c ->
            out[c.id] = Offset(startX + rowWidth * (i + 0.5f) / ordered.size, y)
        }
    }
    return out
}

private fun List<Character>.centerOrdered(centerId: Long?): List<Character> {
    val centered = firstOrNull { it.id == centerId } ?: return sortedBy { it.name.lowercase() }
    val others = filter { it.id != centerId }.sortedBy { it.name.lowercase() }
    val leftCount = others.size / 2
    return others.take(leftCount) + centered + others.drop(leftCount)
}

/**
 * Assigns each character a depth from the one-way edges of a single [HierarchyKind] (from = parent →
 * to = child): roots with no incoming such edge sit at level 0, children one below their deepest
 * parent. Tree ranks by [HierarchyKind.FAMILY] (Parent of), Pyramid by [HierarchyKind.ORG] (Superior
 * of); other links never stack. Cycles are broken so the recursion always terminates.
 */
private fun computeLevels(
    characters: List<Character>,
    relationships: List<CharacterRelationship>,
    kind: HierarchyKind,
    typesById: Map<String, CustomRelationshipType>,
): Map<Long, Int> {
    val ids = characters.mapTo(HashSet()) { it.id }
    val hierarchyEdges = relationships.mapNotNull { rel ->
        rel.normalizedHierarchyEdge(typesById)
            ?.takeIf { (parent, child) -> parent in ids && child in ids && rel.hierarchy(typesById) == kind }
    }
    val parents = hierarchyEdges.groupBy({ it.second }, { it.first })

    val level = HashMap<Long, Int>()
    fun depth(id: Long, visiting: Set<Long>): Int {
        level[id]?.let { return it }
        val ps = parents[id]
        if (ps.isNullOrEmpty() || id in visiting) {
            if (id !in visiting) level[id] = 0
            return 0
        }
        val d = ps.maxOf { depth(it, visiting + id) } + 1
        level[id] = d
        return d
    }
    val idsInHierarchy = hierarchyEdges.flatMapTo(HashSet()) { listOf(it.first, it.second) }
    idsInHierarchy.forEach { depth(it, emptySet()) }

    val peerEdges = relationships
        .filter { it.structure(typesById) == RelationshipStructure.PEER }
        .filter { rel ->
            rel.category(typesById).toGraphHierarchy() == kind &&
                rel.fromCharacterId in ids &&
                rel.toCharacterId in ids
        }
    repeat(characters.size.coerceAtLeast(1)) {
        peerEdges.forEach { rel ->
            val fromLevel = level[rel.fromCharacterId]
            val toLevel = level[rel.toCharacterId]
            when {
                fromLevel != null && toLevel == null -> level[rel.toCharacterId] = fromLevel
                toLevel != null && fromLevel == null -> level[rel.fromCharacterId] = toLevel
                fromLevel != null && toLevel != null && fromLevel != toLevel -> {
                    val merged = maxOf(fromLevel, toLevel)
                    level[rel.fromCharacterId] = merged
                    level[rel.toCharacterId] = merged
                }
            }
        }
    }
    characters.forEach { level.putIfAbsent(it.id, 0) }
    return level
}

private fun RelationshipCategory.toGraphHierarchy(): HierarchyKind = when (this) {
    RelationshipCategory.FAMILIAL -> HierarchyKind.FAMILY
    RelationshipCategory.FACTIONAL -> HierarchyKind.ORG
    RelationshipCategory.SOCIAL -> HierarchyKind.NONE
}

private fun CharacterRelationship.normalizedHierarchyEdge(
    typesById: Map<String, CustomRelationshipType>,
): Pair<Long, Long>? = when (structure(typesById)) {
    RelationshipStructure.OVER -> fromCharacterId to toCharacterId
    RelationshipStructure.UNDER -> toCharacterId to fromCharacterId
    RelationshipStructure.PEER -> null
}

/**
 * Assigns each edge a signed lane index within its character pair (…-1, 0, +1…), centred on 0, so
 * parallel relationships between the same two characters draw side by side instead of overlapping.
 */
private fun computeEdgeOffsets(relationships: List<CharacterRelationship>): Map<Long, Float> {
    val groups = relationships.groupBy {
        val a = it.fromCharacterId
        val b = it.toCharacterId
        if (a <= b) a to b else b to a
    }
    val out = HashMap<Long, Float>(relationships.size)
    groups.values.forEach { group ->
        val k = group.size
        group.forEachIndexed { i, rel -> out[rel.id] = i - (k - 1) / 2f }
    }
    return out
}

/** Unit perpendicular to the line low → high (a consistent direction for both edges of a pair). */
private fun perpUnit(lowPos: Offset, highPos: Offset): Offset {
    val dx = highPos.x - lowPos.x
    val dy = highPos.y - lowPos.y
    val len = kotlin.math.hypot(dx, dy)
    if (len == 0f) return Offset(0f, 0f)
    return Offset(-dy / len, dx / len)
}

/**
 * Draws one relationship edge. Edges sharing a character pair are shifted into separate parallel
 * lanes (offset perpendicular along their whole length, using a consistent low → high orientation so
 * opposite-direction edges still split apart). Peer edges are plain lines; over/under edges point
 * from the higher/source role to the lower/target role.
 */
private fun DrawScope.drawRelationshipEdge(
    from: Offset,
    to: Offset,
    lowPos: Offset,
    highPos: Offset,
    offsetPx: Float,
    color: Color,
    tone: RelationshipTone,
    structure: RelationshipStructure,
    nodeRadius: Float,
    headSize: Float,
) {
    val start: Offset
    val end: Offset
    if (offsetPx == 0f) {
        start = from
        end = to
    } else {
        val perp = perpUnit(lowPos, highPos)
        val shift = Offset(perp.x * offsetPx, perp.y * offsetPx)
        start = Offset(from.x + shift.x, from.y + shift.y)
        end = Offset(to.x + shift.x, to.y + shift.y)
    }
    drawRelationshipLine(start, end, color, tone, 3.dp.toPx())
    if (structure != RelationshipStructure.PEER) {
        drawArrowHead(start, end, nodeRadius, color, headSize)
    }
}

/** Tone → stroke dash pattern: solid (positive), long dashes (neutral), round dots (hostile). */
private fun DrawScope.toneEffect(tone: RelationshipTone): PathEffect? = when (tone) {
    RelationshipTone.POSITIVE -> null
    RelationshipTone.NEUTRAL -> PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 8.dp.toPx()))
    RelationshipTone.HOSTILE -> PathEffect.dashPathEffect(floatArrayOf(0.5.dp.toPx(), 7.dp.toPx()))
}

/** Draws a portrait clipped into the node circle, centre-cropped to a square. */
private fun DrawScope.drawCircularImage(image: ImageBitmap, center: Offset, radius: Float) {
    val side = min(image.width, image.height)
    val srcOffset = IntOffset((image.width - side) / 2, (image.height - side) / 2)
    val srcSize = IntSize(side, side)
    val d = (radius * 2f).toInt()
    val dstOffset = IntOffset((center.x - radius).toInt(), (center.y - radius).toInt())
    val circle = Path().apply { addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)) }
    clipPath(circle) {
        drawImage(image, srcOffset, srcSize, dstOffset, IntSize(d, d))
    }
}

/** Straight tone-styled line, used for the [RelationshipLegend] swatches. */
private fun DrawScope.drawRelationshipLine(
    start: Offset,
    end: Offset,
    color: Color,
    tone: RelationshipTone,
    strokeWidth: Float,
) {
    val cap = if (tone == RelationshipTone.HOSTILE) StrokeCap.Round else StrokeCap.Butt
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = cap, pathEffect = toneEffect(tone))
}

/** Draws a small filled triangle pointing at the [to] node (offset back by the node radius). */
private fun DrawScope.drawArrowHead(from: Offset, to: Offset, nodeRadius: Float, color: Color, sizePx: Float) {
    val angle = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
    val tip = Offset(to.x - nodeRadius * cos(angle).toFloat(), to.y - nodeRadius * sin(angle).toFloat())
    val left = Offset(tip.x - sizePx * cos(angle - 0.4).toFloat(), tip.y - sizePx * sin(angle - 0.4).toFloat())
    val right = Offset(tip.x - sizePx * cos(angle + 0.4).toFloat(), tip.y - sizePx * sin(angle + 0.4).toFloat())
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color = color)
}

@Composable
private fun RelationshipCard(
    relationship: CharacterRelationship,
    characterNames: Map<Long, String>,
    typesById: Map<String, CustomRelationshipType>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val from = characterNames[relationship.fromCharacterId] ?: "?"
    val to = characterNames[relationship.toCharacterId] ?: "?"
    val connector = when (relationship.structure(typesById)) {
        RelationshipStructure.PEER -> "="
        RelationshipStructure.OVER -> ">"
        RelationshipStructure.UNDER -> "<"
    }
    val typeName = relationship.customTypeId?.let { typesById[it]?.name } ?: relationship.type.label

    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$from  $connector  $to",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val subtitle = buildString {
                    append(typeName)
                    append(" · ")
                    append(relationship.category(typesById).label)
                    append(" · ")
                    append(relationship.structure(typesById).label)
                    if (relationship.label.isNotBlank()) append(" · ${relationship.label}")
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete relationship")
            }
        }
    }
}
