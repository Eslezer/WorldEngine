package com.example.worldengine.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.worldengine.domain.model.Character
import com.example.worldengine.ui.components.LabeledDropdown

/**
 * Add/edit dialog for a single timeline event. Date and name are required (the Save button stays
 * disabled until both are filled). The "Order" field is the numeric sort key — lower values appear
 * earlier — which keeps chronology working for custom/fantasy calendars the date label can't express.
 */
@Composable
fun EventEditorDialog(
    draft: EventDraft,
    characters: List<Character>,
    onNameChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onSortKeyChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCharacterSelect: (Long?) -> Unit,
    onLocationChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == 0L) "New event" else "Edit event") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.dateLabel,
                    onValueChange = onDateChange,
                    label = { Text("Date *") },
                    placeholder = { Text("e.g. Third Age 3019, or 12 March") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Event name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.sortKey,
                    onValueChange = onSortKeyChange,
                    label = { Text("Order (lower = earlier)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // "None" is represented by a null character option.
                val options = listOf<Character?>(null) + characters
                val selected = characters.firstOrNull { it.id == draft.characterId }
                LabeledDropdown<Character?>(
                    label = "Attached character",
                    options = options,
                    selected = selected,
                    optionLabel = { it?.name ?: "— None —" },
                    onSelected = { onCharacterSelect(it?.id) },
                )

                OutlinedTextField(
                    value = draft.location,
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.duration,
                    onValueChange = onDurationChange,
                    label = { Text("Time period / duration") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
