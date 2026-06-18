package com.example.worldengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.worldengine.core.data.prefs.AppPreferencesRepository
import com.example.worldengine.domain.model.AppPreferences
import com.example.worldengine.feature.characters.CharacterEditorScreen
import com.example.worldengine.feature.imagelab.ImageLabScreen
import com.example.worldengine.feature.settings.SettingsScreen
import com.example.worldengine.feature.worlds.WorldDetailScreen
import com.example.worldengine.feature.worlds.WorldsScreen
import com.example.worldengine.ui.navigation.Destination
import com.example.worldengine.ui.navigation.Routes
import com.example.worldengine.ui.theme.WorldEngineTheme
import org.koin.compose.koinInject

/**
 * Built on the JCU CP3406/CP5307 utility-app starter template. The template makes a single-Activity
 * Jetpack Compose + Material 3 app whose [UtilityApp][com.example.utilityapp] composable wraps a
 * [Scaffold] with a bottom [NavigationBar] (Utility / Settings tabs) and switches screens from the
 * selected tab.
 *
 * World Engine keeps that exact base function: a [Scaffold] driven by a bottom [NavigationBar]. The
 * only expansion over the template is that the two starter tabs grow to the three top-level sections
 * this app needs. Image Generation, Worlds and Settings (see [Destination]) and tab selection is
 * given through a [NavHost] so each section can unfold into (Worlds → World → Character) while the
 * theme in `ui/theme` is carried over from the template.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferencesRepository = koinInject<AppPreferencesRepository>()
            val preferences by preferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = AppPreferences())
            WorldEngineTheme(
                themeMode = preferences.themeMode,
                fontScale = preferences.fontSize.scale,
            ) {
                WorldEngineRoot()
            }
        }
    }
}

/** Sections shown in the bottom [NavigationBar]. Drill-down routes (world/character) are not here. */
private val TOP_LEVEL_ROUTES = setOf(Routes.IMAGE_GENERATION, Routes.WORLDS, Routes.SETTINGS)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldEngineRoot() {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in TOP_LEVEL_ROUTES

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleForRoute(currentRoute)) },
                navigationIcon = {
                    // Top-level sections are reached from the bottom bar, so they need no up button;
                    // drill-down screens (world/character) get a back arrow.
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            // The template's horizontal 3-button menu. Hidden while drilling into a world or
            // character so those flows get the full screen height.
            if (isTopLevel) {
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        // Standard bottom-bar behaviour: keep a single copy of each
                                        // section on the back stack and restore its saved state.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.IMAGE_GENERATION,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.IMAGE_GENERATION) { ImageLabScreen() }

            composable(Routes.WORLDS) {
                WorldsScreen(onOpenWorld = { id -> navController.navigate(Routes.worldDetail(id)) })
            }

            composable(
                route = Routes.WORLD_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_WORLD_ID) { type = NavType.LongType }),
            ) { entry ->
                val worldId = entry.arguments?.getLong(Routes.ARG_WORLD_ID) ?: 0L
                WorldDetailScreen(
                    worldId = worldId,
                    onAddCharacter = { navController.navigate(Routes.characterEditor(worldId, 0)) },
                    onOpenCharacter = { cid -> navController.navigate(Routes.characterEditor(worldId, cid)) },
                )
            }

            composable(
                route = Routes.CHARACTER_EDITOR,
                arguments = listOf(
                    navArgument(Routes.ARG_WORLD_ID) { type = NavType.LongType },
                    navArgument(Routes.ARG_CHARACTER_ID) { type = NavType.LongType },
                ),
            ) { entry ->
                val worldId = entry.arguments?.getLong(Routes.ARG_WORLD_ID) ?: 0L
                val characterId = entry.arguments?.getLong(Routes.ARG_CHARACTER_ID) ?: 0L
                CharacterEditorScreen(
                    worldId = worldId,
                    characterId = characterId,
                    onDone = { navController.navigateUp() },
                )
            }

            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

private fun titleForRoute(route: String?): String = when (route) {
    Routes.IMAGE_GENERATION -> "Image Generation"
    Routes.WORLDS -> "Worlds"
    Routes.SETTINGS -> "Settings"
    Routes.WORLD_DETAIL -> "World"
    Routes.CHARACTER_EDITOR -> "Character"
    else -> "World Engine"
}
