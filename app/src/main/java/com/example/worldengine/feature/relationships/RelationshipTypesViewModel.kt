package com.example.worldengine.feature.relationships

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.RelationshipTypeRepository
import com.example.worldengine.domain.model.CustomRelationshipType
import com.example.worldengine.domain.model.RelationshipType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TypeDraft(
    val id: String = "",
    val name: String = "",
    val base: RelationshipType = RelationshipType.FRIEND,
    val colorArgb: Int? = null,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/**
 * Manages the global list of [CustomRelationshipType]s (create/edit/delete). Each is a custom name +
 * optional colour layered on a built-in [RelationshipType] template, which supplies its directedness.
 */
class RelationshipTypesViewModel(
    private val repository: RelationshipTypeRepository,
) : ViewModel() {

    val types: StateFlow<List<CustomRelationshipType>> = repository.types
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow<TypeDraft?>(null)
    val draft: StateFlow<TypeDraft?> = _draft.asStateFlow()

    fun startCreate() { _draft.value = TypeDraft() }

    fun startEdit(type: CustomRelationshipType) {
        _draft.value = TypeDraft(id = type.id, name = type.name, base = type.base, colorArgb = type.colorArgb)
    }

    fun dismissDraft() { _draft.value = null }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value) }
    fun onBaseChange(base: RelationshipType) = updateDraft { it.copy(base = base) }
    fun onColorChange(colorArgb: Int?) = updateDraft { it.copy(colorArgb = colorArgb) }

    fun save() {
        val d = _draft.value ?: return
        if (!d.canSave) return
        val type = CustomRelationshipType(
            id = d.id.ifBlank { UUID.randomUUID().toString() },
            name = d.name.trim(),
            base = d.base,
            colorArgb = d.colorArgb,
        )
        viewModelScope.launch {
            repository.save(type)
            _draft.value = null
        }
    }

    fun delete(type: CustomRelationshipType) {
        viewModelScope.launch { repository.delete(type.id) }
    }

    private inline fun updateDraft(transform: (TypeDraft) -> TypeDraft) {
        _draft.value = _draft.value?.let(transform)
    }
}
