package com.example.worldengine.core.di

import androidx.room.Room
import com.example.worldengine.core.data.local.MIGRATION_2_3
import com.example.worldengine.core.data.local.MIGRATION_3_4
import com.example.worldengine.core.data.local.MIGRATION_4_5
import com.example.worldengine.core.data.local.MIGRATION_5_6
import com.example.worldengine.core.data.local.MIGRATION_6_7
import com.example.worldengine.core.data.local.MIGRATION_7_8
import com.example.worldengine.core.data.local.WorldEngineDatabase
import com.example.worldengine.core.data.prefs.AppPreferencesRepository
import com.example.worldengine.core.data.prefs.CalendarRepository
import com.example.worldengine.core.data.prefs.RelationshipTypeRepository
import com.example.worldengine.core.data.prefs.SecureKeyStore
import com.example.worldengine.core.data.remote.novelai.AuthInterceptor
import com.example.worldengine.core.data.remote.novelai.NovelAiApi
import com.example.worldengine.data.repository.CharacterRepositoryImpl
import com.example.worldengine.data.repository.ImageGenRepositoryImpl
import com.example.worldengine.data.repository.LoreRepositoryImpl
import com.example.worldengine.data.repository.RelationshipRepositoryImpl
import com.example.worldengine.data.repository.TimelineRepositoryImpl
import com.example.worldengine.data.repository.WorldRepositoryImpl
import com.example.worldengine.domain.repository.CharacterRepository
import com.example.worldengine.domain.repository.ImageGenRepository
import com.example.worldengine.domain.repository.LoreRepository
import com.example.worldengine.domain.repository.RelationshipRepository
import com.example.worldengine.domain.repository.TimelineRepository
import com.example.worldengine.domain.repository.WorldRepository
import com.example.worldengine.feature.calendars.CalendarsViewModel
import com.example.worldengine.feature.characters.CharacterEditorViewModel
import com.example.worldengine.feature.imagelab.ImageLabViewModel
import com.example.worldengine.feature.lore.LoreViewModel
import com.example.worldengine.feature.relationships.RelationshipTypesViewModel
import com.example.worldengine.feature.relationships.RelationshipViewModel
import com.example.worldengine.feature.settings.SettingsViewModel
import com.example.worldengine.feature.timeline.TimelineViewModel
import com.example.worldengine.feature.worlds.WorldDetailViewModel
import com.example.worldengine.feature.worlds.WorldsViewModel
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
    single { AppPreferencesRepository(androidContext()) }

    // Local database (Room) + DAOs + repositories
    single {
        Room.databaseBuilder(
            androidContext(),
            WorldEngineDatabase::class.java,
            WorldEngineDatabase.NAME,
        )
            // Preserve user data across schema changes. Only ancient (pre-v2) installs, which predate
            // exported schemas, fall back to a one-time destructive rebuild.
            .addMigrations(
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
            )
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .build()
    }
    single { get<WorldEngineDatabase>().worldDao() }
    single { get<WorldEngineDatabase>().characterDao() }
    single { get<WorldEngineDatabase>().timelineEventDao() }
    single { get<WorldEngineDatabase>().characterRelationshipDao() }
    single { get<WorldEngineDatabase>().loreDao() }
    single<WorldRepository> { WorldRepositoryImpl(get(), Dispatchers.IO) }
    single<CharacterRepository> { CharacterRepositoryImpl(get(), Dispatchers.IO) }
    single<TimelineRepository> { TimelineRepositoryImpl(get(), Dispatchers.IO) }
    single<RelationshipRepository> { RelationshipRepositoryImpl(get(), Dispatchers.IO) }
    single<LoreRepository> { LoreRepositoryImpl(get(), Dispatchers.IO) }

    // Global custom calendars and relationship types live in DataStore (JSON), not the Room database.
    single { CalendarRepository(androidContext(), get()) }
    single { RelationshipTypeRepository(androidContext(), get()) }

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

    // ImageLabViewModel(imageGenRepository, keyStore, characterRepository, worldRepository)
    viewModel { ImageLabViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { CalendarsViewModel(get()) }
    viewModel { RelationshipTypesViewModel(get()) }
    viewModel { WorldsViewModel(get()) }
    viewModel { (worldId: Long) -> WorldDetailViewModel(worldId, get(), get(), get()) }
    // TimelineViewModel(worldId, timelineRepository, characterRepository, calendarRepository)
    viewModel { (worldId: Long) -> TimelineViewModel(worldId, get(), get(), get(), get()) }
    // RelationshipViewModel(worldId, relationshipRepository, characterRepository, relationshipTypeRepository)
    viewModel { (worldId: Long) -> RelationshipViewModel(worldId, get(), get(), get(), get()) }
    viewModel { (worldId: Long) -> LoreViewModel(worldId, get()) }
    viewModel { (worldId: Long, characterId: Long) ->
        CharacterEditorViewModel(worldId, characterId, get(), get())
    }
}
