package com.example.worldengine.core.data.remote.novelai

import com.example.worldengine.domain.model.GenerationRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for POST https://image.novelai.net/ai/generate-image.
 * V4/V4.5 models read the nested v4_prompt / v4_negative_prompt structures; the flat
 * input / negative_prompt fields are kept for backward compatibility with V3.
 */
@Serializable
data class NovelAiRequest(
    val input: String,
    val model: String,
    val action: String = "generate",
    val parameters: NovelAiParameters,
)

@Serializable
data class NovelAiParameters(
    @SerialName("params_version") val paramsVersion: Int = 3,
    val width: Int,
    val height: Int,
    val scale: Double,
    val sampler: String,
    val steps: Int,
    @SerialName("n_samples") val nSamples: Int = 1,
    val seed: Long,
    val ucPreset: Int = 0,
    val qualityToggle: Boolean = true,
    @SerialName("cfg_rescale") val cfgRescale: Double = 0.0,
    @SerialName("noise_schedule") val noiseSchedule: String,
    @SerialName("use_coords") val useCoords: Boolean = false,
    val legacy: Boolean = false,
    @SerialName("legacy_v3_extend") val legacyV3Extend: Boolean = false,
    @SerialName("dynamic_thresholding") val dynamicThresholding: Boolean = false,
    @SerialName("add_original_image") val addOriginalImage: Boolean = true,
    @SerialName("controlnet_strength") val controlnetStrength: Double = 1.0,
    @SerialName("negative_prompt") val negativePrompt: String,
    @SerialName("v4_prompt") val v4Prompt: V4Prompt,
    @SerialName("v4_negative_prompt") val v4NegativePrompt: V4NegativePrompt,
)

@Serializable
data class V4Prompt(
    val caption: V4Caption,
    @SerialName("use_coords") val useCoords: Boolean = false,
    @SerialName("use_order") val useOrder: Boolean = true,
)

@Serializable
data class V4NegativePrompt(
    val caption: V4Caption,
    @SerialName("legacy_uc") val legacyUc: Boolean = false,
)

@Serializable
data class V4Caption(
    @SerialName("base_caption") val baseCaption: String,
    // Multi-character captions are a future extension point; empty for single-subject prompts.
    @SerialName("char_captions") val charCaptions: List<String> = emptyList(),
)

/** Maps a domain GenerationRequest to the NovelAI wire format with a resolved (non-zero) seed. */
fun GenerationRequest.toNovelAiRequest(resolvedSeed: Long): NovelAiRequest {
    val s = settings
    return NovelAiRequest(
        input = prompt,
        model = s.model.apiName,
        action = "generate",
        parameters = NovelAiParameters(
            width = s.width,
            height = s.height,
            scale = s.scale,
            sampler = s.sampler.apiName,
            steps = s.steps,
            seed = resolvedSeed,
            noiseSchedule = s.noiseSchedule.apiName,
            negativePrompt = negativePrompt,
            v4Prompt = V4Prompt(caption = V4Caption(baseCaption = prompt)),
            v4NegativePrompt = V4NegativePrompt(caption = V4Caption(baseCaption = negativePrompt)),
        ),
    )
}
