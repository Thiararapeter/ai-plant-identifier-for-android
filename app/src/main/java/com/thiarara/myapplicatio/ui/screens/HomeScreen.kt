package com.thiarara.myapplicatio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.thiarara.myapplicatio.ui.components.CopyrightFooter
import com.thiarara.myapplicatio.ui.components.NoApiKeyDialog
import com.thiarara.myapplicatio.data.SettingsDataStore

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onImageSelected: (String) -> Unit,
    onDiseaseImageSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showNoApiKeyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showDiseaseDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery launcher for regular identification
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            onImageSelected(it.toString())
        }
    }

    // Gallery launcher for disease analysis
    val diseaseGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            onDiseaseImageSelected(it.toString())
        }
    }

    LaunchedEffect(Unit) {
        settingsDataStore.geminiApiKey.collect { key ->
            apiKey = key
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Plant Identifier",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Plant & Disease Identification",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Text(
                    text = "Version 1.0.3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Instantly identify plants and diagnose plant diseases using advanced AI technology",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // Camera Button
        Button(
            onClick = {
                if (apiKey.isNullOrBlank()) {
                    showNoApiKeyDialog = true
                } else {
                    showDiseaseDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Take Photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Gallery Button
        Button(
            onClick = {
                if (apiKey.isNullOrBlank()) {
                    showNoApiKeyDialog = true
                } else {
                    showDiseaseDialog = true
                    selectedUri = null
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose from Gallery",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Features Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Key Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FeatureItem("Instant plant identification")
                FeatureItem("Detailed disease analysis")
                FeatureItem("Care instructions")
                FeatureItem("Growing conditions")
                FeatureItem("Treatment recommendations")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Small settings link
        TextButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.padding(bottom = 4.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Add footer
        CopyrightFooter()
    }

    // Disease Analysis Dialog
    if (showDiseaseDialog) {
        AlertDialog(
            onDismissRequest = { showDiseaseDialog = false },
            title = { Text("Choose Analysis Type") },
            text = { Text("Would you like to perform a disease-specific analysis or general plant identification?") },
            confirmButton = {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Button(
                        onClick = {
                            showDiseaseDialog = false
                            if (selectedUri != null) {
                                onDiseaseImageSelected(selectedUri.toString())
                            } else {
                                onNavigateToCamera()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disease Analysis")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showDiseaseDialog = false
                            if (selectedUri != null) {
                                onImageSelected(selectedUri.toString())
                            } else {
                                onNavigateToCamera()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("General Identification")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiseaseDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNoApiKeyDialog) {
        NoApiKeyDialog(
            onDismiss = { showNoApiKeyDialog = false },
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
} 