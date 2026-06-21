package com.example.worldengine.feature.lore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.ui.components.LabeledDropdown
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * World-scoped codex: reusable entries that later features can link to (places for maps, factions
 * for character hierarchies, terms for story-reader lookup, and so on).
 */
@Composable
fun LoreSection(
    worldId: Long,
    viewModel: LoreViewModel = koinViewModel { parametersOf(worldId) },
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val entries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val entryDraft by viewModel.entryDraft.collectAsStateWithLifecycle()
    val categoryDraft by viewModel.categoryDraft.collectAsStateWithLifecycle()

    val categoriesById = categories.associateBy { it.id }
    val selectedCategory = selectedCategoryId?.let { categoriesById[it] }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LoreToolbar(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                onSelectCategory = viewModel::selectCategory,
                onSearchChange = viewModel::onSearchChange,
                onCreateCategory = viewModel::startCreateCategory,
                onEditCategory = { selectedCategory?.let(viewModel::startEditCategory) },
            )

            if (entries.isEmpty()) {
                EmptyLoreState(hasFilter = selectedCategoryId != null || searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        LoreEntryCard(
                            entry = entry,
                            category = categoriesById[entry.categoryId],
                            onOpen = { viewModel.startEditEntry(entry) },
                            onDelete = { viewModel.deleteEntry(entry) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::startCreateEntry,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New lore entry")
        }
    }

    entryDraft?.let {
        LoreEntryEditorDialog(
            draft = it,
            categories = categories,
            onCategoryChange = viewModel::onEntryCategoryChange,
            onTitleChange = viewModel::onEntryTitleChange,
            onSummaryChange = viewModel::onEntrySummaryChange,
            onBodyChange = viewModel::onEntryBodyChange,
            onAliasesChange = viewModel::onEntryAliasesChange,
            onTagsChange = viewModel::onEntryTagsChange,
            onSave = viewModel::saveEntry,
            onDismiss = viewModel::dismissEntryDraft,
        )
    }

    categoryDraft?.let {
        LoreCategoryEditorDialog(
            draft = it,
            onNameChange = viewModel::onCategoryNameChange,
            onDescriptionChange = viewModel::onCategoryDescriptionChange,
            onColorChange = viewModel::onCategoryColorChange,
            onIconChange = viewModel::onCategoryIconChange,
            onSave = viewModel::saveCategory,
            onDelete = {
                selectedCategory?.let(viewModel::deleteCategory)
                viewModel.dismissCategoryDraft()
            },
            onDismiss = viewModel::dismissCategoryDraft,
        )
    }
}

@Composable
private fun LoreToolbar(
    categories: List<LoreCategory>,
    selectedCategoryId: String?,
    searchQuery: String,
    selectedCategory: LoreCategory?,
    onSelectCategory: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onCreateCategory: () -> Unit,
    onEditCategory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Search lore") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") },
            )
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onSelectCategory(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        CategoryDot(category.colorHex)
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCreateCategory) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Category")
            }
            if (selectedCategory != null) {
                OutlinedButton(onClick = onEditCategory) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit ${selectedCategory.name}")
                }
            }
        }
    }
}

@Composable
private fun EmptyLoreState(hasFilter: Boolean) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (hasFilter) "No matching lore" else "No lore entries yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (hasFilter) {
                "Try another category or search term."
            } else {
                "Add a codex entry with the + button. Use categories like Places, Factions, Terms " +
                    "or your own custom buckets."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoreEntryCard(
    entry: LoreEntry,
    category: LoreCategory?,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    category?.let {
                        CategoryDot(it.colorHex)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            it.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val preview = entry.summary.ifBlank { entry.body }
                if (preview.isNotBlank()) {
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val tokens = (entry.aliases + entry.tags.map { "#$it" }).take(6)
                if (tokens.isNotEmpty()) {
                    Text(
                        tokens.joinToString("  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete lore entry")
            }
        }
    }
}

@Composable
private fun LoreEntryEditorDialog(
    draft: LoreEntryDraft,
    categories: List<LoreCategory>,
    onCategoryChange: (String?) -> Unit,
    onTitleChange: (String) -> Unit,
    onSummaryChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onAliasesChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf<LoreCategory?>(null) + categories
    val selected = categories.firstOrNull { it.id == draft.categoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == 0L) "New lore entry" else "Edit lore entry") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledDropdown(
                    label = "Category",
                    options = options,
                    selected = selected,
                    optionLabel = { it?.name ?: "Uncategorized" },
                    onSelected = { onCategoryChange(it?.id) },
                )
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.summary,
                    onValueChange = onSummaryChange,
                    label = { Text("Lookup summary") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.body,
                    onValueChange = onBodyChange,
                    label = { Text("Lore notes") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.aliases,
                    onValueChange = onAliasesChange,
                    label = { Text("Aliases (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.tags,
                    onValueChange = onTagsChange,
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LoreCategoryEditorDialog(
    draft: LoreCategoryDraft,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id.isBlank()) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryDot(draft.colorHex)
                    OutlinedTextField(
                        value = draft.colorHex,
                        onValueChange = onColorChange,
                        label = { Text("Color hex") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = draft.icon,
                    onValueChange = onIconChange,
                    label = { Text("Icon label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") } },
        dismissButton = {
            Row {
                if (draft.id.isNotBlank() && !draft.isDefault) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun CategoryDot(colorHex: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(parseColor(colorHex), CircleShape),
    )
}

private fun parseColor(colorHex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(Color(0xFF6750A4))
