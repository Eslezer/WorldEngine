package com.example.worldengine.feature.imagelab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldengine.core.data.prefs.SecureKeyStore
import com.example.worldengine.core.util.GenResult
import com.example.worldengine.domain.model.GeneratedImage
import com.example.worldengine.domain.model.GenerationRequest
import com.example.worldengine.domain.model.GenerationSettings
import com.example.worldengine.domain.model.ImageModel
import com.example.worldengine.domain.model.NoiseSchedule
import com.example.worldengine.domain.model.ResolutionPreset
import com.example.worldengine.domain.model.Sampler
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.ImageGenRepository
import com.example.worldengine.domain.repository.WorldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface GenStatus {
    data object Idle : GenStatus
    data object Loading : GenStatus
    data class Error(val message: String) : GenStatus
}

/** A character offered in the "use as portrait" picker, labelled with its owning world. */
data class CharacterChoice(val id: Long, val name: String, val worldName: String) {
    val label: String get() = if (worldName.isBlank()) name else "$name · $worldName"
}

data class ImageLabUiState(
    val prompt: String = "",
    val negativePrompt: String = DEFAULT_NEGATIVE,
    val settings: GenerationSettings = GenerationSettings(),
    val status: GenStatus = GenStatus.Idle,
    val history: List<GeneratedImage> = emptyList(),
    val hasApiKey: Boolean = false,
    val characters: List<CharacterChoice> = emptyList(),
    val selectedCharacterId: Long? = null,
    val assignMessage: String? = null,
) {
    val latestImagePath: String? get() = history.firstOrNull()?.filePath
    val canGenerate: Boolean get() = prompt.isNotBlank() && hasApiKey && status != GenStatus.Loading

    /** The latest image can be assigned only when one exists and a character is selected. */
    val canAssignPortrait: Boolean
        get() = latestImagePath != null && selectedCharacterId != null

    companion object {
        const val DEFAULT_NEGATIVE =
            "lowres, bad anatomy, bad hands, text, error, missing fingers, " +
                "extra digit, fewer digits, cropped, worst quality, low quality, jpeg artifacts, " +
                "signature, watermark, username, blurry"
    }
}

class ImageLabViewModel(
    private val repository: ImageGenRepository,
    private val keyStore: SecureKeyStore,
    private val characterRepository: CharacterRepository,
    worldRepository: WorldRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageLabUiState(hasApiKey = keyStore.hasApiKey()))
    val uiState: StateFlow<ImageLabUiState> = _uiState.asStateFlow()

    init {
        // Keep the portrait picker in sync with every world's characters, labelling each by its
        // world so identically named characters stay distinguishable.
        viewModelScope.launch {
            combine(
                worldRepository.observeWorlds(),
                characterRepository.observeAllCharacters(),
            ) { worlds, characters ->
                val worldNames = worlds.associate { it.id to it.name }
                characters.map { c ->
                    CharacterChoice(id = c.id, name = c.name, worldName = worldNames[c.worldId] ?: "")
                }
            }.collect { choices ->
                _uiState.update { state ->
                    state.copy(
                        characters = choices,
                        // Preserve the current selection if it still exists, else default to the first.
                        selectedCharacterId = state.selectedCharacterId
                            ?.takeIf { id -> choices.any { it.id == id } }
                            ?: choices.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    /** Re-check key presence (e.g. when returning from the Settings tab). */
    fun refreshKeyState() = _uiState.update { it.copy(hasApiKey = keyStore.hasApiKey()) }

    fun onCharacterSelected(id: Long) =
        _uiState.update { it.copy(selectedCharacterId = id, assignMessage = null) }

    /**
     * Assigns the most recently generated image as the selected character's profile picture. This is
     * the single place portraits are set: the character editor only displays them.
     */
    fun assignLatestToCharacter() {
        val state = _uiState.value
        val path = state.latestImagePath ?: return
        val charId = state.selectedCharacterId ?: return
        viewModelScope.launch {
            characterRepository.setPortrait(charId, path)
            val name = state.characters.firstOrNull { it.id == charId }?.name ?: "character"
            _uiState.update { it.copy(assignMessage = "Set as $name's profile picture ✓") }
        }
    }

    fun onPromptChange(value: String) = _uiState.update { it.copy(prompt = value) }
    fun onNegativePromptChange(value: String) = _uiState.update { it.copy(negativePrompt = value) }

    fun onModelChange(model: ImageModel) = updateSettings { it.copy(model = model) }
    fun onSamplerChange(sampler: Sampler) = updateSettings { it.copy(sampler = sampler) }
    fun onNoiseScheduleChange(schedule: NoiseSchedule) = updateSettings { it.copy(noiseSchedule = schedule) }
    fun onStepsChange(steps: Int) = updateSettings { it.copy(steps = steps.coerceIn(1, 28)) }
    fun onScaleChange(scale: Double) = updateSettings { it.copy(scale = scale.coerceIn(0.0, 10.0)) }

    fun onResolutionChange(preset: ResolutionPreset) =
        updateSettings { it.copy(width = preset.width, height = preset.height) }

    fun onSeedChange(seed: Long) = updateSettings { it.copy(seed = seed.coerceAtLeast(0L)) }
    fun randomizeSeed() = updateSettings { it.copy(seed = Random.nextLong(1, 9_999_999_999L)) }
    fun clearSeed() = updateSettings { it.copy(seed = 0L) }

    fun generate() {
        val state = _uiState.value
        if (!state.canGenerate) return
        _uiState.update { it.copy(status = GenStatus.Loading, assignMessage = null) }
        viewModelScope.launch {
            val request = GenerationRequest(
                prompt = state.prompt.trim(),
                negativePrompt = state.negativePrompt.trim(),
                settings = state.settings,
            )
            when (val result = repository.generate(request)) {
                is GenResult.Success -> _uiState.update {
                    it.copy(status = GenStatus.Idle, history = listOf(result.data) + it.history)
                }
                is GenResult.Error -> _uiState.update {
                    it.copy(status = GenStatus.Error(result.message))
                }
            }
        }
    }

    private inline fun updateSettings(transform: (GenerationSettings) -> GenerationSettings) =
        _uiState.update { it.copy(settings = transform(it.settings)) }
}
