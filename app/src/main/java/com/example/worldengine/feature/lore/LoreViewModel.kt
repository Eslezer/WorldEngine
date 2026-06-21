package com.example.worldengine.feature.lore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.domain.model.LoreCategory
import com.example.worldengine.domain.model.LoreEntry
import com.example.worldengine.domain.repository.LoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class LoreEntryDraft(
    val id: Long = 0,
    val categoryId: String? = null,
    val title: String = "",
    val summary: String = "",
    val body: String = "",
    val aliases: String = "",
    val tags: String = "",
    val imagePath: String? = null,
    val createdAt: Long = 0,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

data class LoreCategoryDraft(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val colorHex: String = "#6750A4",
    val icon: String = "Article",
    val isDefault: Boolean = false,
    val sortOrder: Int = 100,
    val createdAt: Long = 0,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

class LoreViewModel(
    private val worldId: Long,
    private val repository: LoreRepository,
) : ViewModel() {

    val categories: StateFlow<List<LoreCategory>> = repository.observeCategories(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val entries: StateFlow<List<LoreEntry>> = repository.observeEntries(worldId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredEntries: StateFlow<List<LoreEntry>> =
        combine(entries, selectedCategoryId, searchQuery) { all, categoryId, query ->
            all.filter { entry ->
                (categoryId == null || entry.categoryId == categoryId) && entry.matches(query)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _entryDraft = MutableStateFlow<LoreEntryDraft?>(null)
    val entryDraft: StateFlow<LoreEntryDraft?> = _entryDraft.asStateFlow()

    private val _categoryDraft = MutableStateFlow<LoreCategoryDraft?>(null)
    val categoryDraft: StateFlow<LoreCategoryDraft?> = _categoryDraft.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureDefaultCategories(worldId) }
    }

    fun selectCategory(id: String?) {
        _selectedCategoryId.value = id
    }

    fun onSearchChange(value: String) {
        _searchQuery.value = value
    }

    fun startCreateEntry() {
        _entryDraft.value = LoreEntryDraft(categoryId = selectedCategoryId.value)
    }

    fun startEditEntry(entry: LoreEntry) {
        _entryDraft.value = LoreEntryDraft(
            id = entry.id,
            categoryId = entry.categoryId,
            title = entry.title,
            summary = entry.summary,
            body = entry.body,
            aliases = entry.aliases.joinToString(", "),
            tags = entry.tags.joinToString(", "),
            imagePath = entry.imagePath,
            createdAt = entry.createdAt,
        )
    }

    fun dismissEntryDraft() {
        _entryDraft.value = null
    }

    fun onEntryCategoryChange(id: String?) = updateEntryDraft { it.copy(categoryId = id) }
    fun onEntryTitleChange(value: String) = updateEntryDraft { it.copy(title = value) }
    fun onEntrySummaryChange(value: String) = updateEntryDraft { it.copy(summary = value) }
    fun onEntryBodyChange(value: String) = updateEntryDraft { it.copy(body = value) }
    fun onEntryAliasesChange(value: String) = updateEntryDraft { it.copy(aliases = value) }
    fun onEntryTagsChange(value: String) = updateEntryDraft { it.copy(tags = value) }

    fun saveEntry() {
        val draft = _entryDraft.value ?: return
        if (!draft.canSave) return
        viewModelScope.launch {
            repository.saveEntry(
                LoreEntry(
                    id = draft.id,
                    worldId = worldId,
                    categoryId = draft.categoryId,
                    title = draft.title.trim(),
                    summary = draft.summary.trim(),
                    body = draft.body.trim(),
                    aliases = draft.aliases.toTokenList(),
                    tags = draft.tags.toTokenList(),
                    imagePath = draft.imagePath,
                    createdAt = draft.createdAt,
                ),
            )
            _entryDraft.value = null
        }
    }

    fun deleteEntry(entry: LoreEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun startCreateCategory() {
        val nextOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: 99) + 1
        _categoryDraft.value = LoreCategoryDraft(sortOrder = nextOrder)
    }

    fun startEditCategory(category: LoreCategory) {
        _categoryDraft.value = LoreCategoryDraft(
            id = category.id,
            name = category.name,
            description = category.description,
            colorHex = category.colorHex,
            icon = category.icon,
            isDefault = category.isDefault,
            sortOrder = category.sortOrder,
            createdAt = category.createdAt,
        )
    }

    fun dismissCategoryDraft() {
        _categoryDraft.value = null
    }

    fun onCategoryNameChange(value: String) = updateCategoryDraft { it.copy(name = value) }
    fun onCategoryDescriptionChange(value: String) = updateCategoryDraft { it.copy(description = value) }
    fun onCategoryColorChange(value: String) = updateCategoryDraft { it.copy(colorHex = value) }
    fun onCategoryIconChange(value: String) = updateCategoryDraft { it.copy(icon = value) }

    fun saveCategory() {
        val draft = _categoryDraft.value ?: return
        if (!draft.canSave) return
        viewModelScope.launch {
            repository.saveCategory(
                LoreCategory(
                    id = draft.id.ifBlank { UUID.randomUUID().toString() },
                    worldId = worldId,
                    name = draft.name.trim(),
                    description = draft.description.trim(),
                    colorHex = draft.colorHex.trim().ifBlank { "#6750A4" },
                    icon = draft.icon.trim().ifBlank { "Article" },
                    isDefault = draft.isDefault,
                    sortOrder = draft.sortOrder,
                    createdAt = draft.createdAt,
                ),
            )
            _categoryDraft.value = null
        }
    }

    fun deleteCategory(category: LoreCategory) {
        if (category.isDefault) return
        viewModelScope.launch {
            repository.deleteCategory(category)
            if (selectedCategoryId.value == category.id) selectCategory(null)
        }
    }

    private inline fun updateEntryDraft(transform: (LoreEntryDraft) -> LoreEntryDraft) {
        _entryDraft.value = _entryDraft.value?.let(transform)
    }

    private inline fun updateCategoryDraft(transform: (LoreCategoryDraft) -> LoreCategoryDraft) {
        _categoryDraft.value = _categoryDraft.value?.let(transform)
    }
}

private fun String.toTokenList(): List<String> =
    split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
