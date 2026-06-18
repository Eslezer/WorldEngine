package com.example.worldengine.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.TimelineEvent
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How the timeline is laid out on screen. */
enum class TimelineViewMode { Vertical, Horizontal }

/**
 * Editable form state for the add/edit-event dialog. [sortKey] is kept as text so the field can be
 * blank while typing; it's parsed to a number on save. [canSave] enforces the two required fields.
 */
data class EventDraft(
    val id: Long = 0,
    val name: String = "",
    val dateLabel: String = "",
    val sortKey: String = "",
    val description: String = "",
    val characterId: Long? = null,
    val location: String = "",
    val duration: String = "",
    val createdAt: Long = 0,
) {
    val canSave: Boolean get() = name.isNotBlank() && dateLabel.isNotBlank()
}

/**
 * Backs the Timeline section of a world. Exposes the world's events (ordered earliest → latest), the
 * world's characters for the "attach character" picker, the current view mode / ordering, and the
 * draft for the add/edit dialog. Mirrors the Worlds editor's dialog-driven create/edit pattern.
 */
class TimelineViewModel(
    private val worldId: Long,
    private val timelineRepository: TimelineRepository,
    characterRepository: CharacterRepository,
) : ViewModel() {

    val events: StateFlow<List<TimelineEvent>> = timelineRepository.observeEvents(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characters: StateFlow<List<Character>> = characterRepository.observeCharacters(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _viewMode = MutableStateFlow(TimelineViewMode.Vertical)
    val viewMode: StateFlow<TimelineViewMode> = _viewMode.asStateFlow()

    /** Vertical default: present/future at the top, first event at the bottom (newest first). */
    private val _newestFirst = MutableStateFlow(true)
    val newestFirst: StateFlow<Boolean> = _newestFirst.asStateFlow()

    /** The event currently being created/edited in the dialog, or null when closed. */
    private val _draft = MutableStateFlow<EventDraft?>(null)
    val draft: StateFlow<EventDraft?> = _draft.asStateFlow()

    fun setViewMode(mode: TimelineViewMode) { _viewMode.value = mode }
    fun setNewestFirst(value: Boolean) { _newestFirst.value = value }

    fun startCreate() {
        // Suggest a sort key just after the latest event so new entries append in order by default.
        val nextKey = (events.value.maxOfOrNull { it.sortKey } ?: 0L) + 1
        _draft.value = EventDraft(sortKey = nextKey.toString())
    }

    fun startEdit(event: TimelineEvent) {
        _draft.value = EventDraft(
            id = event.id,
            name = event.name,
            dateLabel = event.dateLabel,
            sortKey = event.sortKey.toString(),
            description = event.description,
            characterId = event.characterId,
            location = event.location,
            duration = event.duration,
            createdAt = event.createdAt,
        )
    }

    fun dismissDraft() { _draft.value = null }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }
    fun onDateChange(value: String) = updateDraft { it.copy(dateLabel = value) }
    fun onSortKeyChange(value: String) = updateDraft { it.copy(sortKey = value.filter { c -> c.isDigit() || c == '-' }) }
    fun onDescriptionChange(value: String) = updateDraft { it.copy(description = value) }
    fun onCharacterSelect(id: Long?) = updateDraft { it.copy(characterId = id) }
    fun onLocationChange(value: String) = updateDraft { it.copy(location = value) }
    fun onDurationChange(value: String) = updateDraft { it.copy(duration = value) }

    fun save() {
        val d = _draft.value ?: return
        if (!d.canSave) return
        viewModelScope.launch {
            timelineRepository.save(
                TimelineEvent(
                    id = d.id,
                    worldId = worldId,
                    name = d.name.trim(),
                    dateLabel = d.dateLabel.trim(),
                    sortKey = d.sortKey.toLongOrNull() ?: 0L,
                    description = d.description.trim(),
                    characterId = d.characterId,
                    location = d.location.trim(),
                    duration = d.duration.trim(),
                    createdAt = d.createdAt,
                ),
            )
            _draft.value = null
        }
    }

    fun delete(event: TimelineEvent) {
        viewModelScope.launch { timelineRepository.delete(event) }
    }

    private inline fun updateDraft(transform: (EventDraft) -> EventDraft) {
        _draft.value = _draft.value?.let(transform)
    }
}
