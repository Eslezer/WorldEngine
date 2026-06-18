package com.example.worldengine.domain.model

/** User-facing appearance preferences, persisted via DataStore. */

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class FontSize(val scale: Float, val label: String) {
    SMALL(0.85f, "Small"),
    NORMAL(1.0f, "Normal"),
    LARGE(1.15f, "Large"),
    EXTRA_LARGE(1.3f, "Extra large"),
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.NORMAL,
)
