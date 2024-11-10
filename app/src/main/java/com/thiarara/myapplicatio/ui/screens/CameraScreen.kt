package com.thiarara.myapplicatio.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import java.io.File
import androidx.core.content.FileProvider
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import com.thiarara.myapplicatio.data.SettingsDataStore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

// Import ViewGroup
import android.view.ViewGroup

// Keep these imports for flash mode constants
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF

// Import OutputFileOptions and OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OnImageSavedCallback

@Composable
private fun TipItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun GridOverlay() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = 1.dp.toPx()
        
        // Draw vertical lines
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(size.width / 3f, 0f),
            end = Offset(size.width / 3f, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(2 * size.width / 3f, 0f),
            end = Offset(2 * size.width / 3f, size.height),
            strokeWidth = strokeWidth
        )
        
        // Draw horizontal lines
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, size.height / 3f),
            end = Offset(size.width, size.height / 3f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, 2 * size.height / 3f),
            end = Offset(size.width, 2 * size.height / 3f),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun CameraScreen(
    onImageCaptured: (String, Boolean) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    
    // State variables using appropriate mutable state
    var showGridLines by remember { mutableStateOf(true) }
    var showTips by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(FLASH_MODE_AUTO) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedPhotoUri by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }
    
    // Transform gesture states
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            onImageCaptured(it.toString(), false)
        }
    }

    // Load settings
    LaunchedEffect(Unit) {
        settingsDataStore.showGrid.collect { showGrid -> 
            showGridLines = showGrid 
        }
        settingsDataStore.showTips.collect { tips -> 
            showTips = tips 
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (capturedPhotoUri == null) {
                // Camera Preview
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        val previewView = PreviewView(context).apply {
                            this.scaleType = PreviewView.ScaleType.FILL_CENTER
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                
                                val preview = Preview.Builder()
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setTargetRotation(previewView.display.rotation)
                                    .build()
                                
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraScreen", "Use case binding failed", e)
                                }
                            } catch (e: Exception) {
                                Log.e("CameraScreen", "Camera initialization failed", e)
                            }
                        }, getMainExecutor(context))
                        
                        previewView
                    }
                )
                
                // Grid overlay
                if (showGridLines) {
                    GridOverlay()
                }

                // Photography Tips Card
                if (showTips) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Photography Tips",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TipItem("Ensure good lighting")
                            TipItem("Keep the camera steady")
                            TipItem("Focus on distinctive features")
                            TipItem("Include multiple parts of the plant")
                            TipItem("Avoid shadows on the subject")
                            TipItem("Get close, but maintain focus")
                            TipItem("Use a plain background if possible")
                        }
                    }
                }

                // Bottom controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash mode toggle
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                FLASH_MODE_AUTO -> FLASH_MODE_ON
                                FLASH_MODE_ON -> FLASH_MODE_OFF
                                else -> FLASH_MODE_AUTO
                            }
                            imageCapture?.flashMode = flashMode
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                FLASH_MODE_ON -> Icons.Default.FlashOn
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "Flash mode",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Grid toggle
                    IconButton(
                        onClick = {
                            scope.launch {
                                settingsDataStore.saveShowGrid(!showGridLines)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle grid",
                            tint = if (showGridLines) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Camera capture button
                    IconButton(
                        onClick = {
                            val imageCaptureInstance = imageCapture
                            if (imageCaptureInstance != null) {
                                try {
                                    val photoFile = File(
                                        context.cacheDir,
                                        "plant_image_${System.currentTimeMillis()}.jpg"
                                    ).apply {
                                        createNewFile()
                                    }

                                    val outputOptions = OutputFileOptions.Builder(photoFile).build()

                                    imageCaptureInstance.takePicture(
                                        outputOptions,
                                        getMainExecutor(context),
                                        object : OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                try {
                                                    val contentUri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        photoFile
                                                    )
                                                    context.grantUriPermission(
                                                        context.packageName,
                                                        contentUri,
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    )
                                                    capturedPhotoUri = contentUri.toString()
                                                } catch (e: Exception) {
                                                    showError = "Failed to process image: ${e.message}"
                                                    Log.e("CameraScreen", "Failed to process image", e)
                                                }
                                            }

                                            override fun onError(exc: ImageCaptureException) {
                                                showError = "Failed to capture image: ${exc.message}"
                                                Log.e("CameraScreen", "Photo capture failed", exc)
                                            }
                                        }
                                    )
                                } catch (e: Exception) {
                                    showError = "Error taking photo: ${e.message}"
                                    Log.e("CameraScreen", "Error taking photo", e)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Take photo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Gallery button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Choose from gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Tips toggle button
                    IconButton(
                        onClick = { showTips = !showTips },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Toggle tips",
                            tint = if (showTips) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // Photo Preview with confirmation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 2.5f) // Limit zoom between 0.5x and 2.5x
                                        offsetX = (offsetX + pan.x).coerceIn(-200f, 200f) // Limit panning
                                        offsetY = (offsetY + pan.y).coerceIn(-200f, 200f)
                                    }
                                }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(Uri.parse(capturedPhotoUri)),
                                contentDescription = "Captured photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { capturedPhotoUri = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Retake")
                        }
                        
                        Button(
                            onClick = {
                                capturedPhotoUri?.let { uri -> 
                                    onImageCaptured(uri, false)
                                }
                            }
                        ) {
                            Text("Identify Plant")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            capturedPhotoUri?.let { uri -> 
                                onImageCaptured(uri, true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Check for diseases",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Diseases")
                        }
                    }
                }
            }
        }
    }

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Error") },
            text = { Text(text = showError ?: "") },
            confirmButton = {
                Button(onClick = { showError = null }) {
                    Text("OK")
                }
            }
        )
    }
}