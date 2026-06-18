package com.example.worldengine.feature.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterEditorUiState(
    val name: String = "",
    val role: String = "",
    val description: String = "",
    val portraitPath: String? = null,
    val isNew: Boolean = true,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/**
 * Backs the character create/edit screen. [characterId] == 0 means "new".
 *
 * Portraits are *not* generated here: the editor only displays the current portrait. Generating an
 * image and attaching it as a portrait is done from the Image Generation tab
 * ([com.example.worldengine.feature.imagelab.ImageLabViewModel.assignLatestToCharacter]), which is
 * why this view model no longer depends on the image-generation repository.
 */
class CharacterEditorViewModel(
    private val worldId: Long,
    private val characterId: Long,
    private val characterRepository: CharacterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterEditorUiState(isNew = characterId == 0L))
    val uiState: StateFlow<CharacterEditorUiState> = _uiState.asStateFlow()

    private var createdAt = 0L

    init {
        if (characterId != 0L) {
            viewModelScope.launch {
                characterRepository.getCharacter(characterId)?.let { c ->
                    createdAt = c.createdAt
                    _uiState.update {
                        it.copy(
                            name = c.name,
                            role = c.role,
                            description = c.description,
                            portraitPath = c.portraitPath,
                            isNew = false,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onRoleChange(value: String) = _uiState.update { it.copy(role = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            characterRepository.save(
                Character(
                    id = characterId,
                    worldId = worldId,
                    name = state.name.trim(),
                    role = state.role.trim(),
                    description = state.description.trim(),
                    portraitPath = state.portraitPath,
                    createdAt = createdAt,
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
