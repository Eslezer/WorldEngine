package com.example.worldengine.domain.repository

import com.example.worldengine.core.util.GenResult
import com.example.worldengine.domain.model.GeneratedImage
import com.example.worldengine.domain.model.GenerationRequest

interface ImageGenRepository {
    /** Generates an image via NovelAI, saves the PNG locally, and returns its metadata. */
    suspend fun generate(request: GenerationRequest): GenResult<GeneratedImage>

    /** Scans locally saved generations, newest first. */
    suspend fun listGeneratedImages(): List<GeneratedImage>

    /** Lists available local generation folders. */
    suspend fun listFolders(): List<String>

    /** Creates a local generation folder and returns its sanitized display name. */
    suspend fun createFolder(name: String): GenResult<String>

    /** Moves a locally saved generation into another gallery folder. */
    suspend fun moveImage(image: GeneratedImage, folder: String): GenResult<GeneratedImage>

    /** Deletes a locally saved generation from device storage. */
    suspend fun deleteImage(image: GeneratedImage): GenResult<Unit>

    /** Saves a copy of a locally generated image into the device's public Pictures library. */
    suspend fun exportImage(image: GeneratedImage): GenResult<String>

    /** Whether a NovelAI API key is currently stored. */
    fun hasApiKey(): Boolean
}
