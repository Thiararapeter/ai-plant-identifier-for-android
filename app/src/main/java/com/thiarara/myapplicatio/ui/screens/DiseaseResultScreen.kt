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
import androidx.compose.ui.graphics.Color
import com.thiarara.myapplicatio.ui.components.CopyrightFooter
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Camera
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import com.thiarara.myapplicatio.ui.components.RateLimitInfo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import com.thiarara.myapplicatio.ui.components.ProTips
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class DiseaseInfo(
    val plantName: String = "",
    val scientificName: String = "",
    val diseaseName: String = "",
    val severity: String = "",
    val symptoms: String = "",
    val causes: String = "",
    val controlMeasures: String = "",
    val prevention: String = "",
    val additionalNotes: String = ""
)

@Composable
fun DiseaseResultScreen(
    imageUri: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore(context) }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var diseaseInfo by remember { mutableStateOf<DiseaseInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var remainingRequests by remember { mutableStateOf(10) }
    var timeUntilReset by remember { mutableStateOf(0L) }
    var showRateLimit by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var isDiseaseOnlyMode by remember { mutableStateOf(false) }

    // Parse the URI
    val uri = remember(imageUri) {
        try {
            Uri.parse(imageUri)
        } catch (e: Exception) {
            Log.e("DiseaseResultScreen", "Error parsing URI", e)
            null
        }
    }

    // Load API key
    LaunchedEffect(Unit) {
        settingsDataStore.geminiApiKey.collect { key ->
            apiKey = key
        }
    }

    fun parseResponse(response: String): DiseaseInfo {
        val sections = response.split("\n")
        var currentSection = ""
        val infoMap = mutableMapOf<String, StringBuilder>()

        for (line in sections) {
            // Remove only asterisks (*) but keep bullet points (•)
            val cleanLine = line.trim().replace(Regex("\\*"), "")
            
            when {
                cleanLine.contains("Common name:", ignoreCase = true) -> {
                    currentSection = "plantName"
                    infoMap[currentSection] = StringBuilder()
                    val name = cleanLine.substringAfter(":").trim()
                    if (name.isNotBlank()) {
                        infoMap[currentSection]?.append(name)
                    }
                }
                cleanLine.contains("Scientific name:", ignoreCase = true) -> {
                    currentSection = "scientificName"
                    infoMap[currentSection] = StringBuilder()
                    val name = cleanLine.substringAfter(":").trim()
                    if (name.isNotBlank()) {
                        infoMap[currentSection]?.append(name)
                    }
                }
                cleanLine.contains("Disease name:", ignoreCase = true) -> {
                    currentSection = "diseaseName"
                    infoMap[currentSection] = StringBuilder()
                    val name = cleanLine.substringAfter(":").trim()
                    if (name.isNotBlank()) {
                        infoMap[currentSection]?.append(name)
                    }
                }
                cleanLine.contains("Severity:", ignoreCase = true) -> {
                    currentSection = "severity"
                    infoMap[currentSection] = StringBuilder()
                    val severity = cleanLine.substringAfter(":").trim()
                    if (severity.isNotBlank()) {
                        infoMap[currentSection]?.append(severity)
                    }
                }
                cleanLine.contains("Symptoms:", ignoreCase = true) -> {
                    currentSection = "symptoms"
                    infoMap[currentSection] = StringBuilder()
                }
                cleanLine.contains("Causes:", ignoreCase = true) -> {
                    currentSection = "causes"
                    infoMap[currentSection] = StringBuilder()
                }
                cleanLine.contains("Control Measures:", ignoreCase = true) -> {
                    currentSection = "controlMeasures"
                    infoMap[currentSection] = StringBuilder()
                }
                cleanLine.contains("Prevention:", ignoreCase = true) -> {
                    currentSection = "prevention"
                    infoMap[currentSection] = StringBuilder()
                }
                cleanLine.contains("Additional Notes:", ignoreCase = true) -> {
                    currentSection = "additionalNotes"
                    infoMap[currentSection] = StringBuilder()
                }
                cleanLine.isNotBlank() && currentSection.isNotEmpty() -> {
                    if (currentSection !in listOf("plantName", "scientificName", "diseaseName", "severity")) {
                        if (!cleanLine.startsWith("•")) {
                            infoMap[currentSection]?.append("• ")
                        }
                    }
                    infoMap[currentSection]?.append(cleanLine)?.append("\n")
                }
            }
        }

        return DiseaseInfo(
            plantName = infoMap["plantName"]?.toString()?.trim() ?: "Unknown",
            scientificName = infoMap["scientificName"]?.toString()?.trim() ?: "Unknown",
            diseaseName = infoMap["diseaseName"]?.toString()?.trim() ?: "No disease detected",
            severity = infoMap["severity"]?.toString()?.trim() ?: "Not applicable",
            symptoms = infoMap["symptoms"]?.toString()?.trim() ?: "",
            causes = infoMap["causes"]?.toString()?.trim() ?: "",
            controlMeasures = infoMap["controlMeasures"]?.toString()?.trim() ?: "",
            prevention = infoMap["prevention"]?.toString()?.trim() ?: "",
            additionalNotes = infoMap["additionalNotes"]?.toString()?.trim() ?: ""
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

            Spacer(modifier = Modifier.height(24.dp))

            // Add Pro Tips
            ProTips()

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Disease Analysis Only",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isDiseaseOnlyMode,
                    onCheckedChange = { isDiseaseOnlyMode = it }
                )
            }

            if (apiKey == null) {
                Text("Please set your Gemini API key in settings")
                Button(onClick = onBackPressed) {
                    Text("Go Back")
                }
            } else {
                if (diseaseInfo == null && !isLoading) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    error = null
                                    val bitmap = uriToBitmap(uri)
                                    val geminiService = GeminiService(apiKey!!)
                                    val result = if (isDiseaseOnlyMode) {
                                        geminiService.identifyDiseaseOnly(bitmap)
                                    } else {
                                        geminiService.identifyPlantDisease(bitmap)
                                    }
                                    
                                    remainingRequests = result.remainingRequests
                                    timeUntilReset = result.timeUntilReset
                                    showRateLimit = true
                                    
                                    if (result.success) {
                                        diseaseInfo = parseResponse(result.message)
                                    } else {
                                        error = result.message
                                        showErrorDialog = true
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                    showErrorDialog = true
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDiseaseOnlyMode) "Analyze Disease Only" else "Full Plant Health Analysis")
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Add RateLimitInfo display here
                if (showRateLimit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RateLimitInfo(
                        remainingRequests = remainingRequests,
                        initialTimeUntilReset = timeUntilReset
                    )
                }

                diseaseInfo?.let { info ->
                    // Show disease analysis results
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
                                text = "Plant Health Analysis",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            InfoSection(
                                title = "Plant Name",
                                content = info.plantName
                            )
                            
                            InfoSection(
                                title = "Scientific Name",
                                content = info.scientificName
                            )
                            
                            InfoSection(
                                title = "Disease Name",
                                content = info.diseaseName,
                                isDisease = true
                            )
                            
                            InfoSection(
                                title = "Severity",
                                content = info.severity,
                                isSeverity = true
                            )
                            
                            InfoSection(
                                title = "Symptoms",
                                content = info.symptoms,
                                isSymptoms = true
                            )
                            
                            InfoSection(
                                title = "Causes",
                                content = info.causes
                            )
                            
                            InfoSection(
                                title = "Control Measures",
                                content = info.controlMeasures,
                                isControlMeasures = true
                            )
                            
                            InfoSection(
                                title = "Prevention",
                                content = info.prevention,
                                isPrevention = true
                            )
                            
                            if (info.additionalNotes.isNotBlank()) {
                                Text(
                                    text = info.additionalNotes,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                    text = "Analysis Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "We couldn't analyze this plant. This could be due to:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("• Image quality or lighting")
                    Text("• Plant not clearly visible")
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
private fun InfoSection(
    title: String,
    content: String,
    isDisease: Boolean = false,
    isSeverity: Boolean = false,
    isSymptoms: Boolean = false,
    isControlMeasures: Boolean = false,
    isPrevention: Boolean = false
) {
    if (content.isBlank()) return

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
                isDisease -> MaterialTheme.colorScheme.error
                isSeverity -> getSeverityColor(content)
                isControlMeasures || isPrevention -> Color(0xFF2E7D32)
                isSymptoms -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        when {
            title == "Plant Name" || title == "Scientific Name" || title == "Disease Name" -> {
                Text(
                    text = content,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            isSeverity -> {
                Text(
                    text = content,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = getSeverityColor(content)
                )
            }
            isSymptoms || isControlMeasures || isPrevention -> {
                Column(
                    modifier = Modifier
                        .background(
                            when {
                                isSymptoms -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            }
                        )
                        .padding(8.dp)
                ) {
                    val lines = content.split("\n")
                    lines.forEach { line ->
                        if (line.isNotBlank()) {
                            Text(
                                text = line,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSymptoms -> MaterialTheme.colorScheme.error
                                    isControlMeasures || isPrevention -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun getSeverityColor(severity: String): Color {
    return when (severity.lowercase()) {
        "mild" -> Color(0xFF4CAF50)    // Green
        "moderate" -> Color(0xFFFFA726) // Orange
        "severe" -> Color(0xFFD32F2F)   // Red
        else -> Color.Gray
    }
}
