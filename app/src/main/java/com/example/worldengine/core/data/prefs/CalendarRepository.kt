package com.example.worldengine.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.worldengine.domain.model.CustomCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.calendarDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "world_engine_calendars")

/**
 * Stores user-defined [CustomCalendar]s globally (app-wide, not per world) in DataStore as a JSON
 * list. Mirrors [AppPreferencesRepository]'s DataStore pattern; chosen over a Room table because
 * calendars are global app data, which also keeps them out of the per-world database schema.
 */
class CalendarRepository(
    private val context: Context,
    private val json: Json,
) {
    val calendars: Flow<List<CustomCalendar>> = context.calendarDataStore.data.map { prefs ->
        prefs[KEY_CALENDARS]?.let {
            runCatching { json.decodeFromString<List<CustomCalendar>>(it) }.getOrNull()
        } ?: emptyList()
    }

    /** Inserts a new calendar or replaces the existing one with the same id. */
    suspend fun save(calendar: CustomCalendar) {
        val current = calendars.first().toMutableList()
        val index = current.indexOfFirst { it.id == calendar.id }
        if (index >= 0) current[index] = calendar else current.add(calendar)
        persist(current)
    }

    suspend fun delete(id: String) {
        persist(calendars.first().filterNot { it.id == id })
    }

    /**
     * Seeds the built-in default calendar once, on first run. Tracked by a separate flag so the
     * default does not reappear if the user later deletes it.
     */
    suspend fun seedDefaultsIfNeeded() {
        val seeded = context.calendarDataStore.data.first()[KEY_SEEDED] ?: false
        if (seeded) return
        save(CustomCalendar.builtInGregorian())
        context.calendarDataStore.edit { it[KEY_SEEDED] = true }
    }

    private suspend fun persist(list: List<CustomCalendar>) {
        context.calendarDataStore.edit { it[KEY_CALENDARS] = json.encodeToString(list) }
    }

    companion object {
        private val KEY_CALENDARS = stringPreferencesKey("calendars_json")
        private val KEY_SEEDED = booleanPreferencesKey("calendars_seeded")
    }
}
