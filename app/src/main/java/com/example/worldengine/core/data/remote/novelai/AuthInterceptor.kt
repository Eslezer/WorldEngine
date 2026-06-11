package com.example.worldengine.core.data.remote.novelai

import com.example.worldengine.core.data.prefs.SecureKeyStore
import okhttp3.Interceptor
import okhttp3.Response

/** Injects the user's NovelAI bearer token (read fresh from secure storage on every call). */
class AuthInterceptor(private val keyStore: SecureKeyStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/x-zip-compressed")
        keyStore.getApiKey()?.takeIf { it.isNotBlank() }?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
