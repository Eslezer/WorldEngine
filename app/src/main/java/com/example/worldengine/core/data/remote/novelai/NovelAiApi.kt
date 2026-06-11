package com.example.worldengine.core.data.remote.novelai

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NovelAiApi {

    /**
     * Generates an image. NovelAI returns a ZIP archive (not JSON) containing the PNG(s),
     * so the body is the raw [ResponseBody] to be unzipped by [ImageZipExtractor].
     */
    @POST("ai/generate-image")
    suspend fun generateImage(@Body request: NovelAiRequest): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://image.novelai.net/"
    }
}
