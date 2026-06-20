package com.example.worldengine.feature.calendars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.CalendarRepository
import com.example.worldengine.domain.model.CalendarUnit
import com.example.worldengine.domain.model.CustomCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Editable row for one calendar unit. [count] and [names] are text for editing convenience: [count]
 * is the number per parent, [names] is an optional newline-separated list naming each value in order
 * (e.g. month names).
 */
data class UnitDraft(val name: String, val count: String, val names: String = "")

data class CalendarDraft(
    val id: String,
    val name: String,
    val units: List<UnitDraft>,
    /** Optional newline-separated looping weekday names. */
    val weekdayNames: String = "",
    /** Which weekday the calendar's day 0 lands on (must match one of [weekdayNames]). */
    val weekdayStart: String = "",
) {
    val canSave: Boolean get() = name.isNotBlank() && units.isNotEmpty() && units.all { it.name.isNotBlank() }

    /** Weekday names parsed from the multiline field, for the "starts on" picker. */
    val weekdayOptions: List<String> get() = weekdayNames.toNameList()
}

/**
 * Manages the global list of [CustomCalendar]s (create/edit/delete) via [CalendarRepository]. New
 * calendars default to a familiar Year/Month/Day structure the user can tweak, including naming each
 * month and defining a looping weekday cycle.
 */
class CalendarsViewModel(
    private val repository: CalendarRepository,
) : ViewModel() {

    val calendars: StateFlow<List<CustomCalendar>> = repository.calendars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow<CalendarDraft?>(null)
    val draft: StateFlow<CalendarDraft?> = _draft.asStateFlow()

    fun startCreate() {
        _draft.value = CalendarDraft(
            id = "",
            name = "",
            units = listOf(
                UnitDraft("Year", "0"),
                UnitDraft("Month", "12"),
                UnitDraft("Day", "30"),
            ),
        )
    }

    fun startEdit(calendar: CustomCalendar) {
        _draft.value = CalendarDraft(
            id = calendar.id,
            name = calendar.name,
            units = calendar.units.map {
                UnitDraft(it.name, it.countInParent.toString(), it.valueNames.joinToString("\n"))
            },
            weekdayNames = calendar.weekdayNames.joinToString("\n"),
            weekdayStart = calendar.weekdayNames.getOrNull(calendar.weekdayStartIndex).orEmpty(),
        )
    }

    fun dismissDraft() { _draft.value = null }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }

    fun onUnitNameChange(index: Int, value: String) = updateDraft { d ->
        d.copy(units = d.units.mapIndexed { i, u -> if (i == index) u.copy(name = value) else u })
    }

    fun onUnitCountChange(index: Int, value: String) = updateDraft { d ->
        val digits = value.filter(Char::isDigit)
        d.copy(units = d.units.mapIndexed { i, u -> if (i == index) u.copy(count = digits) else u })
    }

    fun onUnitNamesChange(index: Int, value: String) = updateDraft { d ->
        d.copy(units = d.units.mapIndexed { i, u -> if (i == index) u.copy(names = value) else u })
    }

    fun onWeekdayNamesChange(value: String) = updateDraft { it.copy(weekdayNames = value) }
    fun onWeekdayStartChange(value: String) = updateDraft { it.copy(weekdayStart = value) }

    fun addUnit() = updateDraft { it.copy(units = it.units + UnitDraft("", "1")) }

    fun removeUnit(index: Int) = updateDraft { d ->
        if (d.units.size <= 1) d else d.copy(units = d.units.filterIndexed { i, _ -> i != index })
    }

    fun save() {
        val d = _draft.value ?: return
        if (!d.canSave) return
        val weekdays = d.weekdayNames.toNameList()
        val calendar = CustomCalendar(
            id = d.id.ifBlank { UUID.randomUUID().toString() },
            name = d.name.trim(),
            units = d.units.mapIndexed { i, u ->
                // The top unit is unbounded; deeper units need a count of at least 1.
                val count = if (i == 0) 0 else (u.count.toIntOrNull() ?: 1).coerceAtLeast(1)
                CalendarUnit(
                    name = u.name.trim(),
                    countInParent = count,
                    valueNames = u.names.toValueNameList(),
                )
            },
            weekdayNames = weekdays,
            weekdayStartIndex = weekdays.indexOf(d.weekdayStart).coerceAtLeast(0),
        )
        viewModelScope.launch {
            repository.save(calendar)
            _draft.value = null
        }
    }

    fun delete(calendar: CustomCalendar) {
        viewModelScope.launch { repository.delete(calendar.id) }
    }

    private inline fun updateDraft(transform: (CalendarDraft) -> CalendarDraft) {
        _draft.value = _draft.value?.let(transform)
    }
}

/** Parses a newline-separated field into names, dropping blank entries (used for weekday names). */
private fun String.toNameList(): List<String> =
    split("\n").map { it.trim() }.filter { it.isNotEmpty() }

/** Like [toNameList] but keeps interior blanks so names stay aligned to their 1-based positions. */
private fun String.toValueNameList(): List<String> =
    split("\n").map { it.trim() }.dropLastWhile { it.isEmpty() }
