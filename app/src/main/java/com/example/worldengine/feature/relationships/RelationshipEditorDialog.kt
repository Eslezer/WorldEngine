package com.example.worldengine.feature.relationships

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.worldengine.domain.model.Character
import com.example.worldengine.ui.components.LabeledDropdown

/**
 * Add/edit dialog for a directed relationship between two characters. The type picker offers built-in
 * templates and any custom types. Save is disabled until both ends are chosen and distinct.
 */
@Composable
fun RelationshipEditorDialog(
    draft: RelationshipDraft,
    characters: List<Character>,
    typeOptions: List<RelationshipTypeOption>,
    onFromSelect: (Long) -> Unit,
    onToSelect: (Long) -> Unit,
    onTypeOptionSelect: (RelationshipTypeOption) -> Unit,
    onLabelChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fromChar = characters.firstOrNull { it.id == draft.fromCharacterId } ?: characters.first()
    val toChar = characters.firstOrNull { it.id == draft.toCharacterId } ?: characters.first()
    val selectedType = typeOptions.firstOrNull {
        it.customTypeId == draft.customTypeId && it.base == draft.type
    } ?: typeOptions.firstOrNull { it.customTypeId == null && it.base == draft.type }
        ?: typeOptions.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == 0L) "New relationship" else "Edit relationship") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledDropdown(
                    label = "From",
                    options = characters,
                    selected = fromChar,
                    optionLabel = { it.name },
                    onSelected = { onFromSelect(it.id) },
                )
                LabeledDropdown(
                    label = "Relationship",
                    options = typeOptions,
                    selected = selectedType,
                    optionLabel = { it.label + if (it.base.mutual) " · mutual" else " · one-way" },
                    onSelected = onTypeOptionSelect,
                )
                LabeledDropdown(
                    label = "To",
                    options = characters,
                    selected = toChar,
                    optionLabel = { it.name },
                    onSelected = { onToSelect(it.id) },
                )
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = onLabelChange,
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. mentor, estranged") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (draft.fromCharacterId == draft.toCharacterId) {
                    Text("Pick two different characters.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
