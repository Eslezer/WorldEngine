package com.example.worldengine.feature.worlds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.World
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.WorldRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorldDetailViewModel(
    private val worldId: Long,
    worldRepository: WorldRepository,
    private val characterRepository: CharacterRepository,
) : ViewModel() {

    val world: StateFlow<World?> = flow { emit(worldRepository.getWorld(worldId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val characters: StateFlow<List<Character>> = characterRepository.observeCharacters(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteCharacter(character: Character) {
        viewModelScope.launch { characterRepository.delete(character) }
    }
}
