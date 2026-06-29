package com.offline.videoconverter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.offline.videoconverter.ui.screens.HistoryScreen
import com.offline.videoconverter.ui.screens.HomeScreen
import com.offline.videoconverter.ui.screens.SettingsScreen
import com.offline.videoconverter.ui.theme.VideoConverterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("home") }

                    // Handle system back buttons on sub-screens
                    BackHandler(enabled = currentScreen != "home") {
                        currentScreen = "home"
                    }

                    // Permission Checker & Requester
                    PermissionRequester()

                    // Screen Navigation Route Manager
                    when (currentScreen) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = "settings" },
                            onNavigateToHistory = { currentScreen = "history" }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "home" },
                            onNavigateToHistory = { currentScreen = "history" }
                        )
                        "history" -> HistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequester() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Gather required permissions based on Android API level
    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Handle result outcomes if necessary
    }

    LaunchedEffect(Unit) {
        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions.toTypedArray())
        }
    }
}
