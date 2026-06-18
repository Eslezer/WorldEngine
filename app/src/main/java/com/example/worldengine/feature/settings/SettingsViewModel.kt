package com.example.worldengine.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.AppPreferencesRepository
import com.example.worldengine.core.data.prefs.SecureKeyStore
import com.example.worldengine.domain.model.AppPreferences
import com.example.worldengine.domain.model.FontSize
import com.example.worldengine.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeyInput: String = "",
    val hasSavedKey: Boolean = false,
    val savedConfirmation: Boolean = false,
    val preferences: AppPreferences = AppPreferences(),
)

class SettingsViewModel(
    private val keyStore: SecureKeyStore,
    private val prefsRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsUiState(hasSavedKey = keyStore.hasApiKey()))

    val uiState: StateFlow<SettingsUiState> =
        combine(_localState, prefsRepository.preferences) { local, prefs ->
            local.copy(preferences = prefs)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = _localState.value,
        )

    fun onApiKeyChange(value: String) =
        _localState.update { it.copy(apiKeyInput = value, savedConfirmation = false) }

    fun saveApiKey() {
        val key = _localState.value.apiKeyInput.trim()
        if (key.isBlank()) return
        keyStore.setApiKey(key)
        _localState.update { it.copy(apiKeyInput = "", hasSavedKey = true, savedConfirmation = true) }
    }

    fun clearApiKey() {
        keyStore.clearApiKey()
        _localState.update { it.copy(hasSavedKey = false, savedConfirmation = false) }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    fun setFontSize(size: FontSize) = viewModelScope.launch { prefsRepository.setFontSize(size) }
}
