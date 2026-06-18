package com.example.worldengine.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The three top-level sections shown in the bottom [androidx.compose.material3.NavigationBar]
 * (the template's horizontal button menu). Everything else (characters, timelines, maps,
 * relationships, lore) lives *inside* a world, reached from Worlds.
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    ImageGeneration(Routes.IMAGE_GENERATION, "Image Generation", Icons.Default.AutoAwesome),
    Worlds(Routes.WORLDS, "Worlds", Icons.Default.Public),
    Settings(Routes.SETTINGS, "Settings", Icons.Default.Settings),
}
