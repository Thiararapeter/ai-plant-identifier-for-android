package com.thiarara.myapplicatio.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    )

    var showRationale by remember { mutableStateOf(false) }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    when {
        permissionsState.allPermissionsGranted -> {
            // Permissions granted, do nothing and let the callback handle it
        }
        permissionsState.shouldShowRationale || showRationale -> {
            AlertDialog(
                onDismissRequest = { showRationale = false },
                icon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Icon(Icons.Default.PermMedia, contentDescription = null)
                    }
                },
                title = {
                    Text("Permissions Required")
                },
                text = {
                    Text(
                        "This app needs access to your camera and photos to function properly. " +
                        "We use these permissions to:\n\n" +
                        "• Take photos of plants for identification\n" +
                        "• Access your photo gallery to select existing plant images\n\n" +
                        "Without these permissions, you won't be able to use the app's main features."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRationale = false
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    ) {
                        Text("Grant Permissions")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            // Open app settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open Settings")
                    }
                }
            )
        }
        else -> {
            LaunchedEffect(Unit) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
} 