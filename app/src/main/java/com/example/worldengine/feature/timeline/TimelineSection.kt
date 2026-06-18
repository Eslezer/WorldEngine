package com.example.worldengine.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.TimelineEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Timeline section shown inside a world (the [com.example.worldengine.feature.worlds.WorldDetailScreen]
 * "Timeline" tab). Self-contained: owns its [TimelineViewModel], the add/edit dialog and its own FAB,
 * so the host screen only needs to place it. Events can be viewed as a vertical or horizontal line and
 * ordered newest-first (present/future leading) or oldest-first.
 */
@Composable
fun TimelineSection(
    worldId: Long,
    viewModel: TimelineViewModel = koinViewModel { parametersOf(worldId) },
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val newestFirst by viewModel.newestFirst.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    // Character id → name, for showing the attached character on each event.
    val characterNames = remember(characters) { characters.associate { it.id to it.name } }
    val ordered = remember(events, newestFirst) { if (newestFirst) events.reversed() else events }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TimelineControls(
                viewMode = viewMode,
                newestFirst = newestFirst,
                onViewModeChange = viewModel::setViewMode,
                onNewestFirstChange = viewModel::setNewestFirst,
            )

            if (ordered.isEmpty()) {
                EmptyTimeline()
            } else when (viewMode) {
                TimelineViewMode.Vertical -> VerticalTimeline(
                    events = ordered,
                    characterNames = characterNames,
                    onEdit = viewModel::startEdit,
                    onDelete = viewModel::delete,
                )
                TimelineViewMode.Horizontal -> HorizontalTimeline(
                    events = ordered,
                    characterNames = characterNames,
                    onEdit = viewModel::startEdit,
                )
            }
        }

        FloatingActionButton(
            onClick = viewModel::startCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add event")
        }
    }

    draft?.let {
        EventEditorDialog(
            draft = it,
            characters = characters,
            onNameChange = viewModel::onNameChange,
            onDateChange = viewModel::onDateChange,
            onSortKeyChange = viewModel::onSortKeyChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCharacterSelect = viewModel::onCharacterSelect,
            onLocationChange = viewModel::onLocationChange,
            onDurationChange = viewModel::onDurationChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissDraft,
        )
    }
}

@Composable
private fun TimelineControls(
    viewMode: TimelineViewMode,
    newestFirst: Boolean,
    onViewModeChange: (TimelineViewMode) -> Unit,
    onNewestFirstChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = viewMode == TimelineViewMode.Vertical,
            onClick = { onViewModeChange(TimelineViewMode.Vertical) },
            label = { Text("Vertical") },
        )
        FilterChip(
            selected = viewMode == TimelineViewMode.Horizontal,
            onClick = { onViewModeChange(TimelineViewMode.Horizontal) },
            label = { Text("Horizontal") },
        )
        Spacer(Modifier.weight(1f))
        Text("Newest first", style = MaterialTheme.typography.labelMedium)
        Switch(checked = newestFirst, onCheckedChange = onNewestFirstChange)
    }
}

@Composable
private fun EmptyTimeline() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("No events yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add a dated event with the + button. Each event needs a date and a name; you can also " +
                "attach a character, a location and a duration.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Vertical layout: a connecting rail down the left with one card per event.
// ---------------------------------------------------------------------------------------------

@Composable
private fun VerticalTimeline(
    events: List<TimelineEvent>,
    characterNames: Map<Long, String>,
    onEdit: (TimelineEvent) -> Unit,
    onDelete: (TimelineEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
    ) {
        items(events, key = { it.id }) { event ->
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                TimelineRail()
                Spacer(Modifier.width(8.dp))
                EventCard(
                    event = event,
                    characterName = characterNames[event.characterId],
                    onClick = { onEdit(event) },
                    onDelete = { onDelete(event) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

/** The left rail: a vertical line with a node dot aligned to the top of the event card. */
@Composable
private fun TimelineRail() {
    Box(
        modifier = Modifier
            .width(24.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier = Modifier
                .offset(y = 18.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Horizontal layout: a connecting rail across the top with one card per event.
// ---------------------------------------------------------------------------------------------

@Composable
private fun HorizontalTimeline(
    events: List<TimelineEvent>,
    characterNames: Map<Long, String>,
    onEdit: (TimelineEvent) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
    ) {
        items(events, key = { it.id }) { event ->
            Column(modifier = Modifier.width(220.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                EventCard(
                    event = event,
                    characterName = characterNames[event.characterId],
                    onClick = { onEdit(event) },
                    onDelete = null,
                    modifier = Modifier.padding(end = 16.dp, top = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared event card
// ---------------------------------------------------------------------------------------------

@Composable
private fun EventCard(
    event: TimelineEvent,
    characterName: String?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete event")
                    }
                }
            }

            val meta = buildList {
                characterName?.let { add("👤 $it") }
                event.location.takeIf { it.isNotBlank() }?.let { add("📍 $it") }
                event.duration.takeIf { it.isNotBlank() }?.let { add("⏳ $it") }
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            event.description.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
