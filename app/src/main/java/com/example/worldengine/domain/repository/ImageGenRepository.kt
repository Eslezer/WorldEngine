package com.example.worldengine.domain.repository

import com.example.worldengine.core.util.GenResult
import com.example.worldengine.domain.model.GeneratedImage
import com.example.worldengine.domain.model.GenerationRequest

interface ImageGenRepository {
    /** Generates an image via NovelAI, saves the PNG locally, and returns its metadata. */
    suspend fun generate(request: GenerationRequest): GenResult<GeneratedImage>

    /** Whether a NovelAI API key is currently stored. */
    fun hasApiKey(): Boolean
}
