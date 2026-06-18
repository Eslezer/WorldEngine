package com.example.worldengine.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.worldengine.domain.model.AppPreferences
import com.example.worldengine.domain.model.FontSize
import com.example.worldengine.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "world_engine_settings")

/** Stores appearance preferences (theme mode, font size) in DataStore as an observable Flow. */
class AppPreferencesRepository(private val context: Context) {

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[KEY_THEME_MODE].toEnumOr(ThemeMode.SYSTEM),
            fontSize = prefs[KEY_FONT_SIZE].toEnumOr(FontSize.NORMAL),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setFontSize(size: FontSize) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size.name }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
    }
}
