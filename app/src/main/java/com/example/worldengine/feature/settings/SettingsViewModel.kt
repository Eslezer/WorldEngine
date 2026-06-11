package com.example.worldengine.feature.settings

import androidx.lifecycle.ViewModel
import com.example.worldengine.core.data.prefs.SecureKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val apiKeyInput: String = "",
    val hasSavedKey: Boolean = false,
    val savedConfirmation: Boolean = false,
)

class SettingsViewModel(
    private val keyStore: SecureKeyStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(hasSavedKey = keyStore.hasApiKey()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onApiKeyChange(value: String) =
        _uiState.update { it.copy(apiKeyInput = value, savedConfirmation = false) }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) return
        keyStore.setApiKey(key)
        _uiState.update {
            it.copy(apiKeyInput = "", hasSavedKey = true, savedConfirmation = true)
        }
    }

    fun clearApiKey() {
        keyStore.clearApiKey()
        _uiState.update { it.copy(hasSavedKey = false, savedConfirmation = false) }
    }
}
