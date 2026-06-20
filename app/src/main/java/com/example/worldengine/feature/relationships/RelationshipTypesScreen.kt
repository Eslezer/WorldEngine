package com.example.worldengine.feature.relationships

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.domain.model.CustomRelationshipType
import com.example.worldengine.domain.model.RelationshipType
import com.example.worldengine.ui.components.LabeledDropdown
import org.koin.androidx.compose.koinViewModel

/** Selectable colour swatches offered in the custom-type editor (null = use the template's colour). */
private val SWATCHES: List<Int> = listOf(
    0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63, 0xFFFF9800,
    0xFFF44336, 0xFF00BCD4, 0xFF795548, 0xFF607D8B, 0xFFFFC107,
).map { Color(it).toArgb() }

/**
 * Global custom-relationship-type manager (reached from Settings). Lets the user create named types
 * (e.g. "Sire / Childe") on top of a built-in template, optionally with a custom colour, for reuse
 * across all worlds.
 */
@Composable
fun RelationshipTypesScreen(viewModel: RelationshipTypesViewModel = koinViewModel()) {
    val types by viewModel.types.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (types.isEmpty()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No custom relationship types yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Create a named type on top of a built-in template (e.g. base \"Parent of\" to make " +
                        "\"Sire / Childe\"). It then appears alongside the built-ins when linking characters.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(types, key = { it.id }) { type ->
                    TypeCard(type = type, onEdit = { viewModel.startEdit(type) }, onDelete = { viewModel.delete(type) })
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::startCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New relationship type")
        }
    }

    draft?.let {
        TypeEditorDialog(
            draft = it,
            onNameChange = viewModel::onNameChange,
            onBaseChange = viewModel::onBaseChange,
            onColorChange = viewModel::onColorChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissDraft,
        )
    }
}

@Composable
private fun TypeCard(type: CustomRelationshipType, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(type.color),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(type.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "based on ${type.base.label} · ${if (type.mutual) "mutual" else "one-way"} · " +
                        type.tone.name.lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete type")
            }
        }
    }
}

@Composable
private fun TypeEditorDialog(
    draft: TypeDraft,
    onNameChange: (String) -> Unit,
    onBaseChange: (RelationshipType) -> Unit,
    onColorChange: (Int?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id.isBlank()) "New type" else "Edit type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Type name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LabeledDropdown(
                    label = "Based on template",
                    options = RelationshipType.entries,
                    selected = draft.base,
                    optionLabel = { it.label + if (it.mutual) " · mutual" else " · one-way" },
                    onSelected = onBaseChange,
                )
                Text("Colour", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // "Default" swatch (template colour) shown as the base type's colour with no selection ring logic.
                    ColorSwatch(
                        color = draft.base.color,
                        selected = draft.colorArgb == null,
                        onClick = { onColorChange(null) },
                    )
                    SWATCHES.forEach { argb ->
                        ColorSwatch(
                            color = Color(argb),
                            selected = draft.colorArgb == argb,
                            onClick = { onColorChange(argb) },
                        )
                    }
                }
                Text(
                    if (draft.colorArgb == null) "Using template colour" else "Custom colour",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}
