package com.example.worldengine.feature.relationships

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.RelationshipTypeRepository
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CharacterLoreLink
import com.example.worldengine.domain.model.CharacterRelationship
import com.example.worldengine.domain.model.CustomRelationshipType
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.model.RelationshipCategory
import com.example.worldengine.domain.model.RelationshipStructure
import com.example.worldengine.domain.model.RelationshipType
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.LoreRepository
import com.example.worldengine.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How the relationship graph is laid out. */
enum class RelationshipViewMode(val label: String) {
    Web("Web"),
    Familial("Familial"),
    Factional("Factional"),
    Social("Social"),
}

/**
 * A selectable relationship type in the editor: either a built-in [RelationshipType] ([customTypeId]
 * == null) or a [CustomRelationshipType] layered on one.
 */
data class RelationshipTypeOption(
    val label: String,
    val base: RelationshipType,
    val customTypeId: String?,
    val category: RelationshipCategory = base.category,
    val structure: RelationshipStructure = base.structure,
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
    loreRepository: LoreRepository,
) : ViewModel() {

    val relationships: StateFlow<List<CharacterRelationship>> =
        relationshipRepository.observeRelationships(worldId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characters: StateFlow<List<Character>> = characterRepository.observeCharacters(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customTypes: StateFlow<List<CustomRelationshipType>> = relationshipTypeRepository.types
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loreCategories: StateFlow<List<LoreCategory>> = loreRepository.observeCategories(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loreEntries: StateFlow<List<LoreEntry>> = loreRepository.observeEntries(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val characterLoreLinks: StateFlow<List<CharacterLoreLink>> = loreRepository.observeCharacterLinks(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Built-in templates plus any custom types, for the editor's type picker. */
    val typeOptions: StateFlow<List<RelationshipTypeOption>> = relationshipTypeRepository.types
        .map { customs ->
            RelationshipType.entries.map { RelationshipTypeOption(it.label, it, null) } +
                customs.map { RelationshipTypeOption(it.name, it.base, it.id, it.category, it.structure) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultTypeOptions())

    private val _viewMode = MutableStateFlow(RelationshipViewMode.Web)
    val viewMode: StateFlow<RelationshipViewMode> = _viewMode.asStateFlow()

    private val _filterCategoryId = MutableStateFlow<String?>(null)
    val filterCategoryId: StateFlow<String?> = _filterCategoryId.asStateFlow()

    private val _filterEntryId = MutableStateFlow<Long?>(null)
    val filterEntryId: StateFlow<Long?> = _filterEntryId.asStateFlow()

    private val _draft = MutableStateFlow<RelationshipDraft?>(null)
    val draft: StateFlow<RelationshipDraft?> = _draft.asStateFlow()

    private val _selectedCharacterId = MutableStateFlow<Long?>(null)
    val selectedCharacterId: StateFlow<Long?> = _selectedCharacterId.asStateFlow()

    private val _centeredCharacterId = MutableStateFlow<Long?>(null)
    val centeredCharacterId: StateFlow<Long?> = _centeredCharacterId.asStateFlow()

    private val _pendingDelete = MutableStateFlow<CharacterRelationship?>(null)
    val pendingDelete: StateFlow<CharacterRelationship?> = _pendingDelete.asStateFlow()

    private val _lastDeleted = MutableStateFlow<CharacterRelationship?>(null)
    val lastDeleted: StateFlow<CharacterRelationship?> = _lastDeleted.asStateFlow()

    init {
        viewModelScope.launch { loreRepository.ensureDefaultCategories(worldId) }
    }

    fun setViewMode(mode: RelationshipViewMode) { _viewMode.value = mode }

    fun setFilterCategory(id: String?) {
        _filterCategoryId.value = id
        _filterEntryId.value = null
    }

    fun setFilterEntry(id: Long?) {
        _filterEntryId.value = id
    }

    fun startCreate() {
        val chars = characters.value
        _draft.value = RelationshipDraft(
            fromCharacterId = chars.getOrNull(0)?.id,
            toCharacterId = chars.getOrNull(1)?.id,
        )
    }

    fun startCreateBetween(fromCharacterId: Long, toCharacterId: Long) {
        _draft.value = RelationshipDraft(
            fromCharacterId = fromCharacterId,
            toCharacterId = toCharacterId,
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
    fun selectCharacter(id: Long?) {
        _selectedCharacterId.value = id
        if (id != null) _centeredCharacterId.value = id
    }

    fun centerCharacter(id: Long?) {
        _centeredCharacterId.value = id
    }

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

    fun askDelete(relationship: CharacterRelationship) {
        _pendingDelete.value = relationship
    }

    fun dismissDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val relationship = _pendingDelete.value ?: return
        viewModelScope.launch {
            relationshipRepository.delete(relationship)
            _lastDeleted.value = relationship
            _pendingDelete.value = null
        }
    }

    fun restoreLastDeleted() {
        val relationship = _lastDeleted.value ?: return
        viewModelScope.launch {
            relationshipRepository.save(relationship)
            _lastDeleted.value = null
        }
    }

    private inline fun updateDraft(transform: (RelationshipDraft) -> RelationshipDraft) {
        _draft.value = _draft.value?.let(transform)
    }

    private fun defaultTypeOptions(): List<RelationshipTypeOption> =
        RelationshipType.entries.map { RelationshipTypeOption(it.label, it, null) }
}
