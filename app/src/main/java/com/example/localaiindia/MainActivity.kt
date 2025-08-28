package com.example.localaiindia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.localaiindia.ui.screens.ChatScreen
import com.example.localaiindia.ui.theme.LocalAIIndiaTheme
import com.example.localaiindia.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val themePreferences = remember { ThemePreferences(context) }
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

            // REMOVED: Deprecated ProvideWindowInsets - use WindowInsets directly in composables
            LocalAIIndiaTheme(darkTheme = isDarkTheme) {
                ChatScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}