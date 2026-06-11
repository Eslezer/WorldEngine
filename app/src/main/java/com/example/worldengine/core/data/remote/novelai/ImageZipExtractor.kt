package com.example.worldengine.core.data.remote.novelai

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** NovelAI returns a ZIP containing image_0.png, image_1.png, ... This pulls out the PNGs. */
object ImageZipExtractor {

    fun firstImage(zipBytes: ByteArray): ByteArray? = allImages(zipBytes).firstOrNull()

    fun allImages(zipBytes: ByteArray): List<ByteArray> {
        val images = mutableListOf<ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".png", ignoreCase = true)) {
                    images += zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        return images
    }
}
