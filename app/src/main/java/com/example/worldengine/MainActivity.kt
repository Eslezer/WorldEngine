package com.example.worldengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.core.data.prefs.AppPreferencesRepository
import com.example.worldengine.domain.model.AppPreferences
import com.example.worldengine.feature.imagelab.ImageLabScreen
import com.example.worldengine.feature.imagelab.ImageLabViewModel
import com.example.worldengine.feature.settings.SettingsScreen
import com.example.worldengine.feature.worlds.WorldsScreen
import com.example.worldengine.ui.components.PlaceholderScreen
import com.example.worldengine.ui.navigation.Destination
import com.example.worldengine.ui.theme.WorldEngineTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
  Built on the JCU CP3406/CP5307 utility-app starter template: this began as the
  template's single-Activity Jetpack Compose + Material 3 setup with a [Scaffold]. I changed the template's
  bottom [androidx.compose.material3.NavigationBar] (Utility/Settings tabs)
   into a [ModalNavigationDrawer] (burger menu) so the app can scale to more many planned
  sections (that would be useful for myself given I do actually
  want to use the app for myself), and the colour/typography theme in `ui/theme` is carried over from the template.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldEngineRoot() {
    var current by remember { mutableStateOf(Destination.ImageLab) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Activity-scoped so Image Lab state (prompt, history) survives navigation.
    val imageLabViewModel: ImageLabViewModel = koinViewModel()

    // Re-check the API key whenever the user navigates to Image Lab.
    LaunchedEffect(current) {
        if (current == Destination.ImageLab) imageLabViewModel.refreshKeyState()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "World Engine",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                Destination.entries.forEach { destination ->
                    NavigationDrawerItem(
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                        badge = { if (!destination.implemented) Text("Soon") },
                        selected = destination == current,
                        onClick = {
                            current = destination
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (current) {
                    Destination.ImageLab -> ImageLabScreen(imageLabViewModel)
                    Destination.Worlds -> WorldsScreen()
                    Destination.Settings -> SettingsScreen()
                    else -> PlaceholderScreen(current.label)
                }
            }
        }
    }
}
