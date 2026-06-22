# World Engine

World Engine is the Android utility app I built for CP3406/CP5307 Assignment 1 by extending the
provided JCU utility app starter template.

The core function of the app is image generation using the NovelAI API and a key. However
I took interest in further developing the app for writing creative works and facilitating viewing them, as I might 
even use the app myself or recommend it to friends.

## Part 1: Assessment Core - Image Generation Utility

The core utility feature is the Image Generation section. This is the part of the app that I did that
directly addresses the assignment requirement for a focused utility-style mobile application.

The Image Generation workflow allows users to:

- Enter a prompt and negative prompt for AI image generation.
- Configure generation settings such as model, resolution, sampler, steps, guidance, noise schedule,
  and seed.
- Generate images through the NovelAI API using Retrofit and OkHttp.
- Save generated images locally on the device.
- Choose which folder new generations should be saved into.
- Open a dedicated image gallery from the Image Generation screen.
- View generated images at larger size in the gallery.
- Select multiple images at once.
- Move selected images between folders.
- Delete selected images with confirmation.
- Download/export selected images to the device's public Pictures folder.
- Assign the latest generated image as a character portrait.

The Settings section supports the image utility by allowing the user to:

- Store a NovelAI API key securely. (I should have provided a test key with the ZIP and in JCU)
- Change app theme mode.
- Change app font size.

The API key is stored using encrypted preferences and is not committed to the project.

## Part 2: Additional Exploration - Worldbuilding Features

After building the image generation utility, I expanded World Engine into a broader creative
worldbuilding companion. These features go beyond the basic assessment requirement, but they support
the image-generation workflow by giving generated images a meaningful creative context. These would all
be just done as sort of an experiment and seeing how far I could get with upgrading
the app past the requirement.

Additional worldbuilding features include:

- **Worlds**: create, edit, and delete fictional worlds.
- **Characters**: create character profiles and attach generated portraits.
- **Lore/Codex**: create lore categories and entries, including custom categories.
- **Character lore links**: connect characters to places, factions, magic systems, cultures, or other
  lore entries.
- **Timeline**: create world events, custom calendar dates, and link events to lore.
- **Relationships**: create typed character relationships.
- **Relationship views**: display relationships as web, tree, or hierarchy-style views.
- **Lore filtering**: filter relationship graphs by lore category and entry.
- **Custom relationship types**: create relationship types with familial, factional, or social
  behaviour.

- I wished to implement maps for the MVP but they seem to be quite a big endeavour for now.

## Relationship to the Starter Template

This project began from the JCU CP3406/CP5307 utility app starter template.

The starter template provided:

- A single `ComponentActivity`.
- Jetpack Compose UI.
- Material Design 3 styling.
- A `Scaffold`.
- Bottom navigation.
- A basic utility screen.
- A settings screen.

World Engine keeps the same basic app structure, but replaces the starter utility screen with the
Image Generation utility and expands the navigation into three top-level sections:

- Image Generation
- Worlds
- Settings

Navigation is handled through a Compose `NavHost`, allowing the Worlds section to open deeper screens
such as world details and character editing.

## Architecture and Implementation

World Engine uses a pragmatic MVVM architecture with repositories separating UI state from data
access.


UI (Jetpack Compose + Material 3)
  -> ViewModel (StateFlow UI state)
    -> Repository interface
      -> Data sources


Main technologies used:

- Kotlin
- Jetpack Compose
- Material Design 3
- Android ViewModel
- Kotlin Coroutines and StateFlow
- Repository pattern
- Koin dependency injection
- Room database
- DataStore preferences
- EncryptedSharedPreferences
- Retrofit and OkHttp
- kotlinx.serialization
- Coil image loading
- NovelAI image generation API

## Data Storage

Worldbuilding data is stored locally using Room. This includes worlds, characters, timeline events,
relationships, lore categories, lore entries, and lore links.

Generated images are stored as local PNG files in app storage. The gallery scans these files, groups
them by folder, and allows moving, deleting, and exporting them.

## Networking

The app uses Retrofit and OkHttp to connect to the NovelAI image generation API. NovelAI returns image
results as a ZIP archive, so the app extracts the first PNG image and saves it locally.

The user's API key is entered in Settings and injected into API requests through an authentication
interceptor. I also intend to support other image generation API's if possible in the future.

## Build and Run

The project targets:

- minSdk 26
- targetSdk 36

Open the project in Android Studio and run it on an emulator or Android device.

Command line build:

```powershell
./gradlew :app:assembleDebug
```

Run tests:

```powershell
./gradlew test
```

## NovelAI Setup

To use image generation:

1. Create or obtain a NovelAI persistent API token. (I recommend having
2. an opus plan, as it can provide you unlimited generations as long as you stay within 1K resolution
3. and 28 steps. The provided key is an Opus plan key)
2. Open the app.
3. Go to Settings.
4. Paste the API token.
5. Return to Image Generation and generate an image.

If no API key is saved, the app shows a message asking the user to add one in Settings.

## MVP Scope

The assignment-facing MVP is the image generation utility:

- Generate images.
- Configure generation settings.
- Store generated images locally.
- Manage image folders.
- View generated images in a gallery.
- Move, delete, and export images.
- Use generated images as character portraits.

The broader worldbuilding tools are included as extra exploration and future-facing development.

## Notes and AI Acknowledgement

This app was developed as an individual assignment project for CP3406/CP5307. The project extends the
provided JCU starter template and uses external libraries and APIs listed above.

Generative AI assistance was used during development as a support tool for brainstorming, debugging,
code review, wording, and documentation drafting. Final design decisions, code changes, testing, and
submission responsibility remain my own.

Suggested APA-style reference for the GenAI tool used:

OpenAI. (2026). *ChatGPT* [Large language model]. https://chatgpt.com
Anthropic. (2026) *Claude* [Large Language model] https://claude.ai

I do intend to implement the future features and maybe port to IOs (I have an IOs) so the current
repository may be subject to change (Though possibly i will just fork this one)