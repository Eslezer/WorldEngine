# World Engine

A worldbuilding companion for writers and hobbyists — an Android app for creating and managing
original creative universes: **worlds** and the characters, maps, timelines, relationship trees &
matrices, and lore that live inside them, plus **AI-generated character art via the NovelAI API**.

Built for **CP3406/CP5307** by extending the provided
[utility-app starter template](https://github.com/JCU-Mobile-Technologies/CP3406_CP5307_UtilityAppStarterTemplate).

## Relationship to the starter template

This project began as the JCU starter template and grew from it; the template is the first commit in
the history. What was kept and how it was extended:

| Template element | In World Engine |
|------------------|-----------------|
| Single `ComponentActivity` + Jetpack Compose + Material 3 | Kept as the foundation |
| `ui/theme` (`Color.kt`, `Theme.kt`, `Type.kt`, Purple/Pink scheme) | Carried over; `Theme.kt` extended with light/dark mode + font scaling |
| `Scaffold` app shell | Kept |
| Bottom `NavigationBar` with Utility/Settings tabs | **Evolved into a `ModalNavigationDrawer`** (burger menu) to scale to many sections; Settings remains a destination |
| `UtilityScreen` counter demo | Replaced by real feature screens (Image Lab, Worlds, …) |

The navigation change is intentional and documented in `MainActivity.kt`.

## Status

| Section | State |
|---------|-------|
| Image Lab — NovelAI image generation | ✅ |
| Settings — dark mode, font size, encrypted API key | ✅ |
| Worlds — create/edit/delete (Room-backed) | ✅ |
| Characters / Timeline / Relationships / Maps / Lore | ⏳ planned (shown in the menu) |

## Architecture

Pragmatic **MVVM + Repository** with a single source of truth in the data layer.

```
UI (Compose, Material 3)
  → ViewModel (StateFlow UI state)
    → Repository (interface in domain/, impl in data/)
      → Data sources: Room (local DB), NovelAI (Retrofit/OkHttp),
        DataStore (preferences), EncryptedSharedPreferences (API key), local files
```

| Package | Responsibility |
|---------|----------------|
| `core/di` | Koin module (`appModule`) |
| `core/data/local` | Room `WorldEngineDatabase`, entities, DAOs |
| `core/data/remote/novelai` | Retrofit API, DTOs (NAI V4.5 format), auth interceptor, ZIP extractor |
| `core/data/prefs` | `SecureKeyStore` (encrypted key) + `AppPreferencesRepository` (DataStore) |
| `domain/model`, `domain/repository` | Domain models and repository interfaces |
| `data/repository`, `data/mapper` | Repository implementations and entity↔domain mappers |
| `feature/imagelab`, `feature/settings`, `feature/worlds` | Screens + ViewModels |
| `ui/theme`, `ui/components`, `ui/navigation` | Theming, reusable composables, drawer destinations |

### Tech stack

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Koin** for dependency injection
- **Room** for the local database
- **Retrofit** + **OkHttp** + **kotlinx.serialization** for networking
- **DataStore** (appearance prefs) + **security-crypto** (encrypted NovelAI key)
- **Coil** for image loading
- **Coroutines / StateFlow** for async, lifecycle-aware state

## Features

- **Worlds** — the root of everything. Create, edit and delete worlds; characters, maps, timelines,
  relationships and lore will each belong to a world.
- **Image Lab** — generate character art with NovelAI (model, resolution, sampler, noise schedule,
  steps, guidance, seed). Results are saved locally and shown in-app.
- **Settings** — light/dark/system theme, four font sizes (applied app-wide), and encrypted entry of
  your NovelAI API key.

## Building

Targets `minSdk 26` / `targetSdk 36`. Gradle (AGP 9.2.1) requires **JDK 17+**.

```powershell
# Android Studio uses its bundled JBR automatically. For the command line:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew :app:assembleDebug
```

## NovelAI setup

1. Subscribe to NovelAI and create a **persistent API token** (`pst-...`).
2. Run the app → **Settings** → paste the token (stored encrypted, never committed).
3. **Image Lab** → enter a prompt, adjust settings, and **Generate**.
