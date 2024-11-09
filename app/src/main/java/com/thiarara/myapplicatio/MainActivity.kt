package com.thiarara.myapplicatio

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thiarara.myapplicatio.data.SettingsDataStore
import com.thiarara.myapplicatio.ui.screens.*
import com.thiarara.myapplicatio.ui.theme.MyApplicationTheme
import com.thiarara.myapplicatio.ui.components.PermissionHandler
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var permissionsGranted by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val settingsDataStore = remember { SettingsDataStore(context) }
            var isDarkMode by remember { mutableStateOf(false) }

            // Collect dark mode preference
            LaunchedEffect(Unit) {
                settingsDataStore.isDarkMode.collect { isDark ->
                    isDarkMode = isDark
                }
            }
            
            MyApplicationTheme(
                darkTheme = isDarkMode
            ) {
                if (!permissionsGranted) {
                    PermissionHandler(
                        onPermissionsGranted = {
                            permissionsGranted = true
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    onNavigateToCamera = { 
                                        navController.navigate("camera")
                                    },
                                    onImageSelected = { uri -> 
                                        val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                                        navController.navigate("result/$encodedUri")
                                    },
                                    onDiseaseImageSelected = { uri ->
                                        val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                                        navController.navigate("disease_result/$encodedUri")
                                    },
                                    onNavigateToSettings = { 
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            
                            composable("camera") {
                                CameraScreen(
                                    onImageCaptured = { uri, isDiseaseCheck ->
                                        val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                                        if (isDiseaseCheck) {
                                            navController.navigate("disease_result/$encodedUri")
                                        } else {
                                            navController.navigate("result/$encodedUri")
                                        }
                                    },
                                    onBackPressed = { 
                                        navController.navigateUp()
                                    }
                                )
                            }
                            
                            composable(
                                route = "result/{imageUri}",
                                arguments = listOf(
                                    navArgument("imageUri") { 
                                        type = NavType.StringType 
                                    }
                                )
                            ) { backStackEntry ->
                                ResultScreen(
                                    imageUri = backStackEntry.arguments?.getString("imageUri") ?: "",
                                    onBackPressed = { 
                                        navController.navigateUp()
                                    }
                                )
                            }
                            
                            composable(
                                route = "disease_result/{imageUri}",
                                arguments = listOf(
                                    navArgument("imageUri") { 
                                        type = NavType.StringType 
                                    }
                                )
                            ) { backStackEntry ->
                                DiseaseResultScreen(
                                    imageUri = backStackEntry.arguments?.getString("imageUri") ?: "",
                                    onBackPressed = { 
                                        navController.navigateUp()
                                    }
                                )
                            }
                            
                            composable("settings") {
                                SettingsScreen(
                                    onBackPressed = { 
                                        navController.navigateUp()
                                    },
                                    onNavigateToChangelog = {
                                        navController.navigate("changelog")
                                    }
                                )
                            }
                            
                            composable("changelog") {
                                ChangelogScreen(
                                    onBackPressed = { 
                                        navController.navigateUp()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "App started")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "App resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "App paused")
    }
}