package com.example.worldengine.feature.worlds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CharacterLoreLink
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.model.World
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.LoreRepository
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
    loreRepository: LoreRepository,
) : ViewModel() {

    val world: StateFlow<World?> = flow { emit(worldRepository.getWorld(worldId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val characters: StateFlow<List<Character>> = characterRepository.observeCharacters(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loreCategories: StateFlow<List<LoreCategory>> = loreRepository.observeCategories(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loreEntries: StateFlow<List<LoreEntry>> = loreRepository.observeEntries(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characterLoreLinks: StateFlow<List<CharacterLoreLink>> = loreRepository.observeCharacterLinks(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteCharacter(character: Character) {
        viewModelScope.launch { characterRepository.delete(character) }
    }
}
