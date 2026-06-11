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
import com.example.worldengine.domain.repository.ImageGenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface GenStatus {
    data object Idle : GenStatus
    data object Loading : GenStatus
    data class Error(val message: String) : GenStatus
}

data class ImageLabUiState(
    val prompt: String = "",
    val negativePrompt: String = DEFAULT_NEGATIVE,
    val settings: GenerationSettings = GenerationSettings(),
    val status: GenStatus = GenStatus.Idle,
    val history: List<GeneratedImage> = emptyList(),
    val hasApiKey: Boolean = false,
) {
    val latestImagePath: String? get() = history.firstOrNull()?.filePath
    val canGenerate: Boolean get() = prompt.isNotBlank() && hasApiKey && status != GenStatus.Loading

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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageLabUiState(hasApiKey = keyStore.hasApiKey()))
    val uiState: StateFlow<ImageLabUiState> = _uiState.asStateFlow()

    /** Re-check key presence (e.g. when returning from the Settings tab). */
    fun refreshKeyState() = _uiState.update { it.copy(hasApiKey = keyStore.hasApiKey()) }

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
        _uiState.update { it.copy(status = GenStatus.Loading) }
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
