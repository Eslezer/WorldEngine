package com.example.worldengine.data.repository

import android.content.Context
import com.example.worldengine.core.data.prefs.SecureKeyStore
import com.example.worldengine.core.data.remote.novelai.ImageZipExtractor
import com.example.worldengine.core.data.remote.novelai.NovelAiApi
import com.example.worldengine.core.data.remote.novelai.toNovelAiRequest
import com.example.worldengine.core.util.GenResult
import com.example.worldengine.domain.model.GeneratedImage
import com.example.worldengine.domain.model.GenerationRequest
import com.example.worldengine.domain.repository.ImageGenRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class ImageGenRepositoryImpl(
    private val api: NovelAiApi,
    private val keyStore: SecureKeyStore,
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : ImageGenRepository {

    override fun hasApiKey(): Boolean = keyStore.hasApiKey()

    override suspend fun generate(request: GenerationRequest): GenResult<GeneratedImage> =
        withContext(ioDispatcher) {
            if (!keyStore.hasApiKey()) {
                return@withContext GenResult.Error("No NovelAI API key set. Add it in Settings.")
            }
            if (request.prompt.isBlank()) {
                return@withContext GenResult.Error("Enter a prompt first.")
            }

            // Resolve the seed client-side so a "random" generation is still reproducible.
            val seed = if (request.settings.seed <= 0L) {
                Random.nextLong(1, 9_999_999_999L)
            } else {
                request.settings.seed
            }

            try {
                val response = api.generateImage(request.toNovelAiRequest(seed))
                if (!response.isSuccessful) {
                    return@withContext GenResult.Error(mapHttpError(response.code()))
                }
                val zipBytes = response.body()?.bytes()
                    ?: return@withContext GenResult.Error("Empty response from NovelAI.")
                val png = ImageZipExtractor.firstImage(zipBytes)
                    ?: return@withContext GenResult.Error("No image found in NovelAI response.")

                val file = saveImage(png)
                GenResult.Success(
                    GeneratedImage(
                        filePath = file.absolutePath,
                        prompt = request.prompt,
                        seedUsed = seed,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            } catch (e: Exception) {
                GenResult.Error(e.message ?: "Network error while contacting NovelAI.", e)
            }
        }

    private fun saveImage(pngBytes: ByteArray): File {
        val dir = File(appContext.filesDir, "generated").apply { mkdirs() }
        val file = File(dir, "we_${System.currentTimeMillis()}.png")
        file.writeBytes(pngBytes)
        return file
    }

    private fun mapHttpError(code: Int): String = when (code) {
        400 -> "Bad request (400) — check the prompt and generation settings."
        401 -> "Invalid or expired API key (401). Update it in Settings."
        402 -> "Insufficient Anlas (402). This generation costs credits — try a free preset."
        429 -> "Rate limited (429). Wait a moment and try again."
        in 500..599 -> "NovelAI server error ($code). Try again shortly."
        else -> "Generation failed (HTTP $code)."
    }
}
