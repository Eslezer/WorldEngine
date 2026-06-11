package com.example.worldengine.domain.model

/**
 * Domain models for image generation. These are UI/business-facing and decoupled from the
 * NovelAI wire format (see core/data/remote/novelai). Optional fields for image reference
 * (img2img / vibe transfer) and multi-character prompts are intentionally left as future
 * extension points so adding them later is additive rather than a refactor.
 */

enum class ImageModel(val apiName: String, val label: String) {
    V4_5_FULL("nai-diffusion-4-5-full", "NAI Diffusion V4.5 Full"),
    V4_5_CURATED("nai-diffusion-4-5-curated", "NAI Diffusion V4.5 Curated"),
    V4_FULL("nai-diffusion-4-full", "NAI Diffusion V4 Full"),
    V3("nai-diffusion-3", "NAI Diffusion V3 (legacy)"),
}

enum class Sampler(val apiName: String, val label: String) {
    EULER_ANCESTRAL("k_euler_ancestral", "Euler Ancestral"),
    EULER("k_euler", "Euler"),
    DPMPP_2S_ANCESTRAL("k_dpmpp_2s_ancestral", "DPM++ 2S Ancestral"),
    DPMPP_2M("k_dpmpp_2m", "DPM++ 2M"),
    DPMPP_2M_SDE("k_dpmpp_2m_sde", "DPM++ 2M SDE"),
    DPMPP_SDE("k_dpmpp_sde", "DPM++ SDE"),
    DDIM("ddim", "DDIM"),
}

enum class NoiseSchedule(val apiName: String, val label: String) {
    KARRAS("karras", "Karras"),
    NATIVE("native", "Native"),
    EXPONENTIAL("exponential", "Exponential"),
    POLYEXPONENTIAL("polyexponential", "Polyexponential"),
}

data class ResolutionPreset(val label: String, val width: Int, val height: Int) {
    companion object {
        val PORTRAIT = ResolutionPreset("Portrait", 832, 1216)
        val LANDSCAPE = ResolutionPreset("Landscape", 1216, 832)
        val SQUARE = ResolutionPreset("Square", 1024, 1024)
        val SMALL = ResolutionPreset("Small", 512, 768)
        val all = listOf(PORTRAIT, LANDSCAPE, SQUARE, SMALL)
    }
}

/** Tunable generation parameters. seed == 0 means "pick a random seed". */
data class GenerationSettings(
    val model: ImageModel = ImageModel.V4_5_FULL,
    val width: Int = 832,
    val height: Int = 1216,
    val steps: Int = 28,
    val scale: Double = 5.0,
    val sampler: Sampler = Sampler.EULER_ANCESTRAL,
    val noiseSchedule: NoiseSchedule = NoiseSchedule.KARRAS,
    val seed: Long = 0L,
) {
    /** Opus-tier free generation: <= 1024x1024, <= 28 steps, single sample. */
    val isFreeGeneration: Boolean
        get() = steps <= 28 && width.toLong() * height.toLong() <= 1024L * 1024L
}

data class GenerationRequest(
    val prompt: String,
    val negativePrompt: String,
    val settings: GenerationSettings,
)

/** A generated image persisted to local storage. */
data class GeneratedImage(
    val filePath: String,
    val prompt: String,
    val seedUsed: Long,
    val createdAt: Long,
)
