package com.example.worldengine.feature.worlds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.domain.model.World
import com.example.worldengine.domain.repository.WorldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorldsViewModel(
    private val repository: WorldRepository,
) : ViewModel() {

    val worlds: StateFlow<List<World>> = repository.observeWorlds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The world currently being created/edited in the dialog, or null when closed. */
    private val _editing = MutableStateFlow<World?>(null)
    val editing: StateFlow<World?> = _editing.asStateFlow()

    fun startCreate() {
        _editing.value = World(name = "")
    }

    fun startEdit(world: World) {
        _editing.value = world
    }

    fun dismissEditor() {
        _editing.value = null
    }

    fun save(name: String, description: String) {
        val current = _editing.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.save(current.copy(name = name.trim(), description = description.trim()))
            _editing.value = null
        }
    }

    fun delete(world: World) {
        viewModelScope.launch { repository.delete(world) }
    }
}
