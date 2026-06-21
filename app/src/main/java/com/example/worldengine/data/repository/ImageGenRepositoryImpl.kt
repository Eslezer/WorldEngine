package com.example.worldengine.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
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

    override suspend fun listGeneratedImages(): List<GeneratedImage> = withContext(ioDispatcher) {
        normalizeRootImages()
        generatedRoot()
            .walkTopDown()
            .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                GeneratedImage(
                    filePath = file.absolutePath,
                    prompt = "",
                    seedUsed = 0L,
                    createdAt = file.lastModified(),
                )
            }
            .toList()
    }

    override suspend fun listFolders(): List<String> = withContext(ioDispatcher) {
        normalizeRootImages()
        (generatedRoot().listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList())
            .plus(GeneratedImage.DEFAULT_FOLDER)
            .distinct()
            .sorted()
    }

    override suspend fun createFolder(name: String): GenResult<String> = withContext(ioDispatcher) {
        try {
            val folderName = name.safeFolderName()
            val dir = File(generatedRoot(), folderName)
            if (!dir.exists() && !dir.mkdirs()) {
                return@withContext GenResult.Error("Could not create folder.")
            }
            GenResult.Success(folderName)
        } catch (e: Exception) {
            GenResult.Error(e.message ?: "Could not create folder.", e)
        }
    }

    override suspend fun moveImage(image: GeneratedImage, folder: String): GenResult<GeneratedImage> =
        withContext(ioDispatcher) {
            try {
                val source = File(image.filePath)
                if (!source.exists()) {
                    return@withContext GenResult.Error("That image is no longer on device storage.")
                }

                val targetDir = File(generatedRoot(), folder.safeFolderName()).apply { mkdirs() }
                val target = uniqueFile(targetDir, source.name)
                val moved = source.renameTo(target)
                if (!moved) {
                    source.copyTo(target, overwrite = false)
                    if (!source.delete()) {
                        target.delete()
                        return@withContext GenResult.Error("Could not remove the original image after moving it.")
                    }
                }
                target.setLastModified(image.createdAt)

                GenResult.Success(
                    image.copy(
                        filePath = target.absolutePath,
                        createdAt = target.lastModified(),
                    )
                )
            } catch (e: Exception) {
                GenResult.Error(e.message ?: "Could not move image.", e)
            }
        }

    override suspend fun deleteImage(image: GeneratedImage): GenResult<Unit> = withContext(ioDispatcher) {
        try {
            val file = File(image.filePath)
            if (!file.exists() || file.delete()) {
                GenResult.Success(Unit)
            } else {
                GenResult.Error("Could not delete image.")
            }
        } catch (e: Exception) {
            GenResult.Error(e.message ?: "Could not delete image.", e)
        }
    }

    override suspend fun exportImage(image: GeneratedImage): GenResult<String> = withContext(ioDispatcher) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@withContext GenResult.Error("Exporting to Pictures requires Android 10 or newer.")
            }

            val source = File(image.filePath)
            if (!source.exists()) {
                return@withContext GenResult.Error("That image is no longer on device storage.")
            }

            val resolver = appContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, source.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/World Engine")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext GenResult.Error("Could not create a Pictures entry for the image.")

            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext GenResult.Error("Could not write image to Pictures.")

            val completed = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completed, null, null)
            GenResult.Success("Saved to Pictures/World Engine.")
        } catch (e: Exception) {
            GenResult.Error(e.message ?: "Could not export image.", e)
        }
    }

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

                val file = saveImage(png, request.folder)
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

    private fun saveImage(pngBytes: ByteArray, folder: String): File {
        val dir = File(generatedRoot(), folder.safeFolderName()).apply { mkdirs() }
        val file = File(dir, "we_${System.currentTimeMillis()}.png")
        file.writeBytes(pngBytes)
        return file
    }

    private fun generatedRoot(): File = File(appContext.filesDir, "generated").apply { mkdirs() }

    private fun normalizeRootImages() {
        val root = generatedRoot()
        val defaultDir = File(root, GeneratedImage.DEFAULT_FOLDER).apply { mkdirs() }
        root.listFiles()
            ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            ?.forEach { file ->
                val createdAt = file.lastModified()
                val target = uniqueFile(defaultDir, file.name)
                if (file.renameTo(target)) {
                    target.setLastModified(createdAt)
                }
            }
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate

        val base = candidate.nameWithoutExtension.ifBlank { "image" }
        val extension = candidate.extension
        var index = 1
        while (true) {
            val suffix = if (extension.isBlank()) "" else ".$extension"
            val next = File(dir, "${base}_$index$suffix")
            if (!next.exists()) return next
            index += 1
        }
    }

    private fun String.safeFolderName(): String =
        trim().ifBlank { GeneratedImage.DEFAULT_FOLDER }
            .replace(Regex("[^A-Za-z0-9 _.-]"), "_")

    private fun mapHttpError(code: Int): String = when (code) {
        400 -> "Bad request (400) — check the prompt and generation settings."
        401 -> "Invalid or expired API key (401). Update it in Settings."
        402 -> "Insufficient Anlas (402). This generation costs credits — try a free preset."
        429 -> "Rate limited (429). Wait a moment and try again."
        in 500..599 -> "NovelAI server error ($code). Try again shortly."
        else -> "Generation failed (HTTP $code)."
    }
}
