package com.example.worldengine.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.worldengine.domain.model.CustomRelationshipType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.relationshipTypeDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "world_engine_relationship_types")

/**
 * Stores user-defined [CustomRelationshipType]s globally (app-wide) in DataStore as a JSON list,
 * mirroring [CalendarRepository]. Built-in [com.example.worldengine.domain.model.RelationshipType]s
 * are always available; this holds only the custom ones layered on top of them.
 */
class RelationshipTypeRepository(
    private val context: Context,
    private val json: Json,
) {
    val types: Flow<List<CustomRelationshipType>> = context.relationshipTypeDataStore.data.map { prefs ->
        prefs[KEY_TYPES]?.let {
            runCatching { json.decodeFromString<List<CustomRelationshipType>>(it) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun save(type: CustomRelationshipType) {
        val current = types.first().toMutableList()
        val index = current.indexOfFirst { it.id == type.id }
        if (index >= 0) current[index] = type else current.add(type)
        persist(current)
    }

    suspend fun delete(id: String) {
        persist(types.first().filterNot { it.id == id })
    }

    private suspend fun persist(list: List<CustomRelationshipType>) {
        context.relationshipTypeDataStore.edit { it[KEY_TYPES] = json.encodeToString(list) }
    }

    companion object {
        private val KEY_TYPES = stringPreferencesKey("relationship_types_json")
    }
}
