package com.example.worldengine.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation destinations shown in the burger (navigation drawer) menu.
 *
 * The app is designed to grow many sections, so destinations live in one ordered list here and the
 * drawer renders from it. [implemented] sections route to a real screen; the rest show a
 * placeholder until built, keeping the menu honest about what's available.
 */
enum class Destination(
    val label: String,
    val icon: ImageVector,
    val implemented: Boolean,
) {
    ImageLab("Image Lab", Icons.Default.AutoAwesome, implemented = true),
    Worlds("Worlds", Icons.Default.Public, implemented = true),
    Characters("Characters", Icons.Default.Groups, implemented = false),
    Timeline("Timeline", Icons.Default.Timeline, implemented = false),
    Relationships("Relationships", Icons.Default.AccountTree, implemented = false),
    Maps("Maps", Icons.Default.Map, implemented = false),
    Lore("Lore", Icons.Default.MenuBook, implemented = false),
    Settings("Settings", Icons.Default.Settings, implemented = true),
}
