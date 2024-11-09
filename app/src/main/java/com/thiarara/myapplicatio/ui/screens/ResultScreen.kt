package com.thiarara.myapplicatio.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.thiarara.myapplicatio.api.GeminiService
import com.thiarara.myapplicatio.data.SettingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.graphics.Color
import com.thiarara.myapplicatio.ui.components.CopyrightFooter
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Camera
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import com.thiarara.myapplicatio.ui.components.RateLimitInfo
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Slider
import com.thiarara.myapplicatio.ui.components.ProTips

data class PlantInfo(
    val commonName: String = "",
    val scientificName: String = "",
    val characteristics: String = "",
    val growingConditions: String = "",
    val careInstructions: String = "",
    val issuesAndDiseases: String = "",
    val productInfo: String = "",
    val isProduct: Boolean = false
)

@Composable
fun ResultScreen(
    imageUri: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore(context) }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var plantInfo by remember { mutableStateOf<PlantInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var remainingRequests by remember { mutableStateOf(10) }
    var timeUntilReset by remember { mutableStateOf(0L) }
    var showRateLimit by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Parse the URI
    val uri = remember(imageUri) {
        try {
            Uri.parse(imageUri)
        } catch (e: Exception) {
            Log.e("ResultScreen", "Error parsing URI", e)
            null
        }
    }

    // Load API key
    LaunchedEffect(Unit) {
        settingsDataStore.geminiApiKey.collect { key ->
            apiKey = key
        }
    }

    fun parseResponse(response: String): PlantInfo {
        val sections = response.split("\n")
        var currentSection = ""
        val infoMap = mutableMapOf<String, StringBuilder>()
        var isProduct = false

        for (line in sections) {
            when {
                line.contains("Product identification:", ignoreCase = true) -> {
                    isProduct = true
                    currentSection = "productInfo"
                    infoMap[currentSection] = StringBuilder()
                }
                line.contains("Common English name:", ignoreCase = true) -> {
                    currentSection = "commonName"
                    infoMap[currentSection] = StringBuilder()
                    val name = line.substringAfter(":").trim().replace("*", "")
                    if (name.isNotBlank()) {
                        infoMap[currentSection]?.append(name)
                    }
                }
                line.contains("Scientific name:", ignoreCase = true) -> {
                    currentSection = "scientificName"
                    infoMap[currentSection] = StringBuilder()
                    val name = line.substringAfter(":").trim().replace("*", "")
                    if (name.isNotBlank()) {
                        infoMap[currentSection]?.append(name)
                    }
                }
                line.contains("Plant characteristics", ignoreCase = true) -> {
                    currentSection = "characteristics"
                    infoMap[currentSection] = StringBuilder()
                }
                line.contains("Growing conditions", ignoreCase = true) -> {
                    currentSection = "growingConditions"
                    infoMap[currentSection] = StringBuilder()
                }
                line.contains("Care instructions", ignoreCase = true) -> {
                    currentSection = "careInstructions"
                    infoMap[currentSection] = StringBuilder()
                }
                line.contains("Common Issues and Diseases", ignoreCase = true) -> {
                    currentSection = "issuesAndDiseases"
                    infoMap[currentSection] = StringBuilder()
                }
                line.isNotBlank() && currentSection.isNotEmpty() -> {
                    if (currentSection != "commonName" && currentSection != "scientificName") {
                        val cleanedLine = line.replace("*", "").trim()
                        if (!cleanedLine.startsWith("•") && !cleanedLine.startsWith("-")) {
                            infoMap[currentSection]?.append("• ")
                        }
                        infoMap[currentSection]?.append(cleanedLine)?.append("\n")
                    }
                }
            }
        }

        return PlantInfo(
            commonName = infoMap["commonName"]?.toString()?.trim()?.replace("*", "") ?: "Unknown",
            scientificName = infoMap["scientificName"]?.toString()?.trim()?.replace("*", "") ?: "Unknown",
            characteristics = infoMap["characteristics"]?.toString()?.trim() ?: "",
            growingConditions = infoMap["growingConditions"]?.toString()?.trim() ?: "",
            careInstructions = infoMap["careInstructions"]?.toString()?.trim() ?: "",
            issuesAndDiseases = infoMap["issuesAndDiseases"]?.toString()?.trim() ?: "",
            productInfo = infoMap["productInfo"]?.toString()?.trim() ?: "",
            isProduct = isProduct
        )
    }

    // Function to convert Uri to Bitmap
    fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    // Add zoom state variables
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uri == null) {
            Text(
                text = "Error loading image",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            // Display the image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 3f)
                                offsetX = (offsetX + pan.x).coerceIn(-500f * scale, 500f * scale)
                                offsetY = (offsetY + pan.y).coerceIn(-500f * scale, 500f * scale)
                            }
                        }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Selected plant image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Add zoom slider control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom out",
                    modifier = Modifier.size(24.dp)
                )
                Slider(
                    value = scale,
                    onValueChange = { newScale ->
                        scale = newScale
                        // Adjust offsets when zooming out to prevent image from getting stuck
                        offsetX = offsetX.coerceIn(-500f * scale, 500f * scale)
                        offsetY = offsetY.coerceIn(-500f * scale, 500f * scale)
                    },
                    valueRange = 0.5f..3f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom in",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Keep the reset zoom button for quick reset
            if (scale != 1f || offsetX != 0f || offsetY != 0f) {
                TextButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                ) {
                    Text("Reset View")
                }
            }

            // Add Pro Tips
            ProTips()

            Spacer(modifier = Modifier.height(24.dp))

            if (apiKey == null) {
                Text("Please set your Gemini API key in settings")
                Button(onClick = onBackPressed) {
                    Text("Go Back")
                }
            } else {
                if (plantInfo == null && !isLoading) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    error = null
                                    val bitmap = uriToBitmap(uri)
                                    val geminiService = GeminiService(apiKey!!)
                                    val result = geminiService.identifyPlant(bitmap)
                                    
                                    remainingRequests = result.remainingRequests
                                    timeUntilReset = result.timeUntilReset
                                    showRateLimit = true  // Show rate limit info after each request
                                    
                                    if (result.success) {
                                        plantInfo = parseResponse(result.message)
                                    } else {
                                        error = result.message
                                        showErrorDialog = true  // Show error dialog on failure
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                    showErrorDialog = true  // Show error dialog on exception
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Identify Plant")
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (error != null) {
                    showErrorDialog = true
                }

                if (showRateLimit && remainingRequests < 10) {
                    RateLimitInfo(remainingRequests, timeUntilReset)
                }

                plantInfo?.let { info ->
                    if (info.commonName == "Unknown" && info.scientificName == "Unknown") {
                        // Show error state when plant wasn't identified
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(bottom = 16.dp)
                                )
                                
                                Text(
                                    text = "Plant Not Identified",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "We couldn't identify this plant. Please try again with:",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Column {
                                    Text("• A clearer photo")
                                    Text("• Better lighting")
                                    Text("• Different angle")
                                    Text("• Closer view of distinctive features")
                                }
                            }
                        }
                    } else {
                        // Show regular plant info card when identification succeeded
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Plant Identification Results",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                InfoSection(
                                    title = "Common Name",
                                    content = info.commonName
                                )
                                
                                InfoSection(
                                    title = "Scientific Name",
                                    content = info.scientificName
                                )
                                
                                InfoSection(
                                    title = "Characteristics",
                                    content = info.characteristics
                                )
                                
                                InfoSection(
                                    title = "Growing Conditions",
                                    content = info.growingConditions
                                )
                                
                                InfoSection(
                                    title = "Care Instructions",
                                    content = info.careInstructions
                                )
                                
                                InfoSection(
                                    title = "Common Issues and Control Measures",
                                    content = info.issuesAndDiseases,
                                    isIssueSection = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onBackPressed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Add footer at the bottom
        CopyrightFooter()
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Identification Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "We couldn't identify this plant. This could be due to:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("• Image quality or lighting")
                    Text("• Plant not clearly visible")
                    Text("• Plant not in our database")
                    Text("• Network connection issues")
                    
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Error details: ${error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onBackPressed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Another Photo")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showErrorDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun InfoSection(
    title: String,
    content: String,
    isIssueSection: Boolean = false,
    isProductInfo: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isIssueSection -> Color(0xFFD32F2F)
                isProductInfo -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        when {
            isProductInfo -> {
                Column(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = content,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            title == "Common Name" || title == "Scientific Name" -> {
                Text(
                    text = content,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            isIssueSection -> {
                val lines = content.split("\n")
                Column(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        )
                        .padding(8.dp)
                ) {
                    lines.forEach { line ->
                        if (line.isNotBlank()) {
                            val isControlMeasure = line.contains("control", ignoreCase = true) || 
                                                 line.contains("treatment", ignoreCase = true) ||
                                                 line.contains("solution", ignoreCase = true) ||
                                                 line.contains("prevent", ignoreCase = true) ||
                                                 line.contains("manage", ignoreCase = true)
                            
                            Text(
                                text = line,
                                fontSize = 16.sp,
                                color = when {
                                    isControlMeasure -> Color(0xFF2E7D32)
                                    else -> Color(0xFFD32F2F)
                                },
                                fontWeight = FontWeight.Medium
                            )
                            if (line != lines.last()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = content,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    }
}