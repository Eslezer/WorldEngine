package com.example.worldengine.feature.relationships

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.RelationshipTypeRepository
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CharacterRelationship
import com.example.worldengine.domain.model.CustomRelationshipType
import com.example.worldengine.domain.model.RelationshipType
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How the relationship graph is laid out. */
enum class RelationshipViewMode { Web, Tree, Pyramid }

/**
 * A selectable relationship type in the editor: either a built-in [RelationshipType] ([customTypeId]
 * == null) or a [CustomRelationshipType] layered on one.
 */
data class RelationshipTypeOption(
    val label: String,
    val base: RelationshipType,
    val customTypeId: String?,
)

/** Editable form state for the add/edit-relationship dialog. */
data class RelationshipDraft(
    val id: Long = 0,
    val fromCharacterId: Long? = null,
    val toCharacterId: Long? = null,
    val type: RelationshipType = RelationshipType.FRIEND,
    val customTypeId: String? = null,
    val label: String = "",
) {
    /** Both ends must be set and distinct. */
    val canSave: Boolean
        get() = fromCharacterId != null && toCharacterId != null && fromCharacterId != toCharacterId
}

/**
 * Backs the Relationships section of a world: the directed edges, the world's characters (for the
 * pickers and graph), the user's global custom relationship types, the selected graph layout, and
 * the draft for the add/edit dialog.
 */
class RelationshipViewModel(
    private val worldId: Long,
    private val relationshipRepository: RelationshipRepository,
    characterRepository: CharacterRepository,
    relationshipTypeRepository: RelationshipTypeRepository,
) : ViewModel() {

    val relationships: StateFlow<List<CharacterRelationship>> =
        relationshipRepository.observeRelationships(worldId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characters: StateFlow<List<Character>> = characterRepository.observeCharacters(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customTypes: StateFlow<List<CustomRelationshipType>> = relationshipTypeRepository.types
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Built-in templates plus any custom types, for the editor's type picker. */
    val typeOptions: StateFlow<List<RelationshipTypeOption>> = relationshipTypeRepository.types
        .map { customs ->
            RelationshipType.entries.map { RelationshipTypeOption(it.label, it, null) } +
                customs.map { RelationshipTypeOption(it.name, it.base, it.id) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultTypeOptions())

    private val _viewMode = MutableStateFlow(RelationshipViewMode.Web)
    val viewMode: StateFlow<RelationshipViewMode> = _viewMode.asStateFlow()

    /** When set, the graph centres on this character and shows only its direct relationships. */
    private val _focusCharacterId = MutableStateFlow<Long?>(null)
    val focusCharacterId: StateFlow<Long?> = _focusCharacterId.asStateFlow()

    private val _draft = MutableStateFlow<RelationshipDraft?>(null)
    val draft: StateFlow<RelationshipDraft?> = _draft.asStateFlow()

    fun setViewMode(mode: RelationshipViewMode) { _viewMode.value = mode }
    fun setFocus(id: Long?) { _focusCharacterId.value = id }

    fun startCreate() {
        val chars = characters.value
        _draft.value = RelationshipDraft(
            fromCharacterId = chars.getOrNull(0)?.id,
            toCharacterId = chars.getOrNull(1)?.id,
        )
    }

    fun startEdit(relationship: CharacterRelationship) {
        _draft.value = RelationshipDraft(
            id = relationship.id,
            fromCharacterId = relationship.fromCharacterId,
            toCharacterId = relationship.toCharacterId,
            type = relationship.type,
            customTypeId = relationship.customTypeId,
            label = relationship.label,
        )
    }

    fun dismissDraft() { _draft.value = null }

    fun onFromSelect(id: Long) = updateDraft { it.copy(fromCharacterId = id) }
    fun onToSelect(id: Long) = updateDraft { it.copy(toCharacterId = id) }
    fun onLabelChange(value: String) = updateDraft { it.copy(label = value) }

    fun onTypeOptionSelect(option: RelationshipTypeOption) =
        updateDraft { it.copy(type = option.base, customTypeId = option.customTypeId) }

    fun save() {
        val d = _draft.value ?: return
        if (!d.canSave) return
        viewModelScope.launch {
            relationshipRepository.save(
                CharacterRelationship(
                    id = d.id,
                    worldId = worldId,
                    fromCharacterId = d.fromCharacterId!!,
                    toCharacterId = d.toCharacterId!!,
                    type = d.type,
                    label = d.label.trim(),
                    customTypeId = d.customTypeId,
                ),
            )
            _draft.value = null
        }
    }

    fun delete(relationship: CharacterRelationship) {
        viewModelScope.launch { relationshipRepository.delete(relationship) }
    }

    private inline fun updateDraft(transform: (RelationshipDraft) -> RelationshipDraft) {
        _draft.value = _draft.value?.let(transform)
    }

    private fun defaultTypeOptions(): List<RelationshipTypeOption> =
        RelationshipType.entries.map { RelationshipTypeOption(it.label, it, null) }
}
