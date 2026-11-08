package com.anacatalina.androidiconsadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.anacatalina.androidiconsadmin.ui.MainViewModel
import com.anacatalina.androidiconsadmin.ui.screens.AppsDashboardScreen
import com.anacatalina.androidiconsadmin.ui.screens.DeliveryAndUndoScreen
import com.anacatalina.androidiconsadmin.ui.screens.RulesScreen
import com.anacatalina.androidiconsadmin.ui.screens.StyleStudioScreen
import com.anacatalina.androidiconsadmin.ui.theme.AndroidIconsAdminTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidIconsAdminTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                                label = { Text("Apps") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                label = { Text("Estilos") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = null) },
                                label = { Text("Reglas") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("Entrega & Undo") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (selectedTab) {
                            0 -> AppsDashboardScreen(viewModel)
                            1 -> StyleStudioScreen(viewModel)
                            2 -> RulesScreen(viewModel)
                            3 -> DeliveryAndUndoScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
