package com.example.worldengine.feature.calendars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.domain.model.CustomCalendar
import com.example.worldengine.ui.components.LabeledDropdown
import org.koin.androidx.compose.koinViewModel

/**
 * Global custom-calendar manager (reached from Settings). Lets the user define date systems —
 * a name plus ordered units like Year / Month / Day — that are then selectable when dating timeline
 * events in any world.
 */
@Composable
fun CalendarsScreen(viewModel: CalendarsViewModel = koinViewModel()) {
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (calendars.isEmpty()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No custom calendars yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Create a calendar with the + button — give it a name and ordered units (e.g. " +
                        "Year, Month, Day). You can then use it to date and order timeline events, " +
                        "including events that span a period.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(calendars, key = { it.id }) { calendar ->
                    CalendarCard(
                        calendar = calendar,
                        onEdit = { viewModel.startEdit(calendar) },
                        onDelete = { viewModel.delete(calendar) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::startCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New calendar")
        }
    }

    draft?.let {
        CalendarEditorDialog(
            draft = it,
            onNameChange = viewModel::onNameChange,
            onUnitNameChange = viewModel::onUnitNameChange,
            onUnitCountChange = viewModel::onUnitCountChange,
            onUnitNamesChange = viewModel::onUnitNamesChange,
            onWeekdayNamesChange = viewModel::onWeekdayNamesChange,
            onWeekdayStartChange = viewModel::onWeekdayStartChange,
            onAddUnit = viewModel::addUnit,
            onRemoveUnit = viewModel::removeUnit,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissDraft,
        )
    }
}

@Composable
private fun CalendarCard(calendar: CustomCalendar, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(calendar.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    calendar.units.joinToString(" › ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete calendar")
            }
        }
    }
}

@Composable
private fun CalendarEditorDialog(
    draft: CalendarDraft,
    onNameChange: (String) -> Unit,
    onUnitNameChange: (Int, String) -> Unit,
    onUnitCountChange: (Int, String) -> Unit,
    onUnitNamesChange: (Int, String) -> Unit,
    onWeekdayNamesChange: (String) -> Unit,
    onWeekdayStartChange: (String) -> Unit,
    onAddUnit: () -> Unit,
    onRemoveUnit: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id.isBlank()) "New calendar" else "Edit calendar") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text("Calendar name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Units, largest first. The first unit is unbounded (e.g. Year); each later unit " +
                        "needs how many fit in the one above it (e.g. 12 months per year). Optionally " +
                        "name each value (one per line, in order) — e.g. month names.",
                    style = MaterialTheme.typography.bodySmall,
                )

                draft.units.forEachIndexed { index, unit ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = unit.name,
                                onValueChange = { onUnitNameChange(index, it) },
                                label = { Text(if (index == 0) "Top unit" else "Unit") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (index > 0) {
                                OutlinedTextField(
                                    value = unit.count,
                                    onValueChange = { onUnitCountChange(index, it) },
                                    label = { Text("per ${draft.units[index - 1].name.ifBlank { "parent" }}") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(120.dp),
                                )
                                IconButton(onClick = { onRemoveUnit(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove unit")
                                }
                            }
                        }
                        // Only bounded (non-top) units can have all their values named.
                        if (index > 0) {
                            OutlinedTextField(
                                value = unit.names,
                                onValueChange = { onUnitNamesChange(index, it) },
                                label = { Text("${unit.name.ifBlank { "Value" }} names (optional, one per line)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                TextButton(onClick = onAddUnit) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add unit")
                }

                Text("Weekday cycle (optional)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "A repeating list of named days that loops across the whole calendar (like " +
                        "Monday…Sunday). Choose which one the calendar's very first day starts on.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = draft.weekdayNames,
                    onValueChange = onWeekdayNamesChange,
                    label = { Text("Weekday names (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val weekdayOptions = draft.weekdayOptions
                if (weekdayOptions.isNotEmpty()) {
                    LabeledDropdown(
                        label = "Calendar starts on",
                        options = weekdayOptions,
                        selected = draft.weekdayStart.ifBlank { weekdayOptions.first() },
                        optionLabel = { it },
                        onSelected = onWeekdayStartChange,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
