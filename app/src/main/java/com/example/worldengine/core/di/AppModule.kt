package com.example.worldengine.core.di

import com.example.worldengine.core.data.prefs.SecureKeyStore
import com.example.worldengine.core.data.remote.novelai.AuthInterceptor
import com.example.worldengine.core.data.remote.novelai.NovelAiApi
import com.example.worldengine.data.repository.ImageGenRepositoryImpl
import com.example.worldengine.domain.repository.ImageGenRepository
import com.example.worldengine.feature.imagelab.ImageLabViewModel
import com.example.worldengine.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {

    single { SecureKeyStore(androidContext()) }

    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    single { AuthInterceptor(get()) }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(logging)
            // Image generation can take a while; give it generous timeouts.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    single {
        val json: Json = get()
        Retrofit.Builder()
            .baseUrl(NovelAiApi.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NovelAiApi::class.java)
    }

    single<ImageGenRepository> {
        ImageGenRepositoryImpl(
            api = get(),
            keyStore = get(),
            appContext = androidContext(),
            ioDispatcher = Dispatchers.IO,
        )
    }

    viewModel { ImageLabViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
