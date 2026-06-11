package com.example.worldengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.worldengine.feature.imagelab.ImageLabScreen
import com.example.worldengine.feature.imagelab.ImageLabViewModel
import com.example.worldengine.feature.settings.SettingsScreen
import com.example.worldengine.ui.theme.WorldEngineTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldEngineTheme {
                WorldEngineRoot()
            }
        }
    }
}

private enum class Tab(val label: String) { ImageLab("Image Lab"), Settings("Settings") }

@Composable
fun WorldEngineRoot() {
    var selectedTab by remember { mutableStateOf(Tab.ImageLab) }

    // Activity-scoped so the Image Lab state (prompt, history) survives tab switches.
    val imageLabViewModel: ImageLabViewModel = koinViewModel()

    // Re-check the API key whenever the user returns to the Image Lab tab.
    LaunchedEffect(selectedTab) {
        if (selectedTab == Tab.ImageLab) imageLabViewModel.refreshKeyState()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = Tab.ImageLab.label) },
                    label = { Text(Tab.ImageLab.label) },
                    selected = selectedTab == Tab.ImageLab,
                    onClick = { selectedTab = Tab.ImageLab },
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = Tab.Settings.label) },
                    label = { Text(Tab.Settings.label) },
                    selected = selectedTab == Tab.Settings,
                    onClick = { selectedTab = Tab.Settings },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Tab.ImageLab -> ImageLabScreen(imageLabViewModel)
                Tab.Settings -> SettingsScreen()
            }
        }
    }
}
