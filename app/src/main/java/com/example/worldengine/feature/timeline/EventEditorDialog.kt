package com.example.worldengine.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CustomCalendar
import com.example.worldengine.ui.components.LabeledDropdown

/**
 * Add/edit dialog for a single timeline event. The name is always required. The date is entered
 * either freeform (a label + numeric order) or, when a [CustomCalendar] is picked, as per-unit
 * fields — which also unlock marking the event as a period (start + end) for things like a war.
 */
@Composable
fun EventEditorDialog(
    draft: EventDraft,
    characters: List<Character>,
    calendars: List<CustomCalendar>,
    onNameChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onSortKeyChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCharacterSelect: (Long?) -> Unit,
    onLocationChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onCalendarSelect: (String?) -> Unit,
    onStartComponentChange: (Int, String) -> Unit,
    onEndComponentChange: (Int, String) -> Unit,
    onPeriodChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val calendar = calendars.firstOrNull { it.id == draft.calendarId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == 0L) "New event" else "Edit event") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Event name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Date-system picker only appears once the user has defined at least one calendar.
                if (calendars.isNotEmpty()) {
                    val options = listOf<CustomCalendar?>(null) + calendars
                    LabeledDropdown<CustomCalendar?>(
                        label = "Date system",
                        options = options,
                        selected = calendar,
                        optionLabel = { it?.name ?: "Freeform date" },
                        onSelected = { onCalendarSelect(it?.id) },
                    )
                }

                if (calendar != null) {
                    Text("Start", style = MaterialTheme.typography.labelLarge)
                    ComponentFields(
                        calendar = calendar,
                        values = draft.startComponents,
                        onChange = onStartComponentChange,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Spans a period (start → end)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = draft.isPeriod, onCheckedChange = onPeriodChange)
                    }

                    if (draft.isPeriod) {
                        Text("End", style = MaterialTheme.typography.labelLarge)
                        ComponentFields(
                            calendar = calendar,
                            values = draft.endComponents,
                            onChange = onEndComponentChange,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = draft.dateLabel,
                        onValueChange = onDateChange,
                        label = { Text("Date *") },
                        placeholder = { Text("e.g. Third Age 3019, or 12 March") },
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
                }

                // "None" is represented by a null character option.
                val charOptions = listOf<Character?>(null) + characters
                val selectedChar = characters.firstOrNull { it.id == draft.characterId }
                LabeledDropdown<Character?>(
                    label = "Attached character",
                    options = charOptions,
                    selected = selectedChar,
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
                    label = { Text("Time period / duration (notes)") },
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
        confirmButton = { TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** One numeric field per calendar unit (e.g. Year / Month / Day). */
@Composable
private fun ComponentFields(
    calendar: CustomCalendar,
    values: List<String>,
    onChange: (Int, String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        calendar.units.forEachIndexed { index, unit ->
            OutlinedTextField(
                value = values.getOrNull(index).orEmpty(),
                onValueChange = { onChange(index, it) },
                label = { Text(unit.name) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(110.dp),
            )
        }
    }
}
