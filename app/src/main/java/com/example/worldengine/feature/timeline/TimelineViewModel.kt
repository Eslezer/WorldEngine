package com.example.worldengine.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.CalendarRepository
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CustomCalendar
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
 * Editable form state for the add/edit-event dialog.
 *
 * Two date modes share this draft. In **freeform** mode ([calendarId] == null) the user types a
 * [dateLabel] and a numeric [sortKey] directly. In **calendar** mode the user fills per-unit
 * [startComponents] (and, for a period, [endComponents]); the label and sort key are computed from
 * the chosen [CustomCalendar] on save.
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
    val calendarId: String? = null,
    val startComponents: List<String> = emptyList(),
    val isPeriod: Boolean = false,
    val endComponents: List<String> = emptyList(),
) {
    /** Calendar mode supplies the date, so only the name is required there; freeform also needs a date. */
    val canSave: Boolean get() = name.isNotBlank() && (calendarId != null || dateLabel.isNotBlank())
}

/**
 * Backs the Timeline section of a world. Exposes the world's events (ordered earliest → latest), the
 * world's characters (for the "attach character" picker), the user's global [CustomCalendar]s, the
 * current view mode / ordering, and the draft for the add/edit dialog.
 */
class TimelineViewModel(
    private val worldId: Long,
    private val timelineRepository: TimelineRepository,
    characterRepository: CharacterRepository,
    calendarRepository: CalendarRepository,
) : ViewModel() {

    val events: StateFlow<List<TimelineEvent>> = timelineRepository.observeEvents(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characters: StateFlow<List<Character>> = characterRepository.observeCharacters(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calendars: StateFlow<List<CustomCalendar>> = calendarRepository.calendars
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
        // Suggest a sort key just after the latest event so new freeform entries append in order.
        val nextKey = (events.value.maxOfOrNull { it.sortKey } ?: 0L) + 1
        _draft.value = EventDraft(sortKey = nextKey.toString())
    }

    fun startEdit(event: TimelineEvent) {
        val calendar = event.calendarId?.let { id -> calendars.value.firstOrNull { it.id == id } }
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
            calendarId = event.calendarId,
            // Recover the per-unit fields from the stored keys so calendar dates can be edited.
            startComponents = calendar?.decode(event.sortKey)?.map { it.toString() } ?: emptyList(),
            isPeriod = event.endSortKey != null,
            endComponents = calendar?.let { c -> event.endSortKey?.let { c.decode(it).map { v -> v.toString() } } }
                ?: emptyList(),
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
    fun setPeriod(value: Boolean) = updateDraft { it.copy(isPeriod = value) }

    /** Switches the draft between freeform (null) and a custom calendar, sizing the component fields. */
    fun onCalendarSelect(id: String?) = updateDraft { d ->
        if (id == null) {
            d.copy(calendarId = null)
        } else {
            val size = calendars.value.firstOrNull { it.id == id }?.units?.size ?: 0
            val blanks = List(size) { i -> if (i == 0) "" else "1" }
            d.copy(
                calendarId = id,
                startComponents = blanks,
                endComponents = blanks,
            )
        }
    }

    fun onStartComponentChange(index: Int, value: String) = updateDraft { d ->
        d.copy(startComponents = d.startComponents.replaceAt(index, value.digitsAllowingSign()))
    }

    fun onEndComponentChange(index: Int, value: String) = updateDraft { d ->
        d.copy(endComponents = d.endComponents.replaceAt(index, value.digitsAllowingSign()))
    }

    fun save() {
        val d = _draft.value ?: return
        if (!d.canSave) return
        val calendar = d.calendarId?.let { id -> calendars.value.firstOrNull { it.id == id } }

        val (sortKey, dateLabel) = if (calendar != null) {
            val values = calendar.parse(d.startComponents)
            calendar.encode(values) to calendar.format(values)
        } else {
            (d.sortKey.toLongOrNull() ?: 0L) to d.dateLabel.trim()
        }

        val endSortKey: Long?
        val endDateLabel: String?
        if (calendar != null && d.isPeriod) {
            val values = calendar.parse(d.endComponents)
            endSortKey = calendar.encode(values)
            endDateLabel = calendar.format(values)
        } else {
            endSortKey = null
            endDateLabel = null
        }

        viewModelScope.launch {
            timelineRepository.save(
                TimelineEvent(
                    id = d.id,
                    worldId = worldId,
                    name = d.name.trim(),
                    dateLabel = dateLabel,
                    sortKey = sortKey,
                    description = d.description.trim(),
                    characterId = d.characterId,
                    location = d.location.trim(),
                    duration = d.duration.trim(),
                    createdAt = d.createdAt,
                    calendarId = d.calendarId,
                    endSortKey = endSortKey,
                    endDateLabel = endDateLabel,
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

private fun List<String>.replaceAt(index: Int, value: String): List<String> =
    mapIndexed { i, s -> if (i == index) value else s }

private fun String.digitsAllowingSign(): String = filter { it.isDigit() || it == '-' }

/** Turns the per-unit text fields into numeric component values (top unbounded, deeper 1-based). */
private fun CustomCalendar.parse(components: List<String>): List<Long> =
    units.mapIndexed { i, _ ->
        val raw = components.getOrNull(i)?.toLongOrNull()
        if (i == 0) raw ?: 0L else (raw ?: 1L).coerceAtLeast(1L)
    }
