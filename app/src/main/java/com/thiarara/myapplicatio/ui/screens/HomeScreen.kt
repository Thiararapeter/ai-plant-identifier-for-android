package com.thiarara.myapplicatio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var isFromCamera by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    var showNoApiKeyDialog by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf<String?>(null) }

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

    // Add animation for content visibility
    var isContentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isContentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wrap content in AnimatedVisibility
        AnimatedVisibility(
            visible = isContentVisible,
            enter = fadeIn(animationSpec = tween(1000)) +
                    slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(1000, easing = EaseOutQuart)
                    ),
        ) {
            // Enhanced App Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .graphicsLayer {
                        shadowElevation = 20f
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                
                // Multiple gradient animations
                val primaryGradientAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(20000, easing = LinearEasing)
                    )
                )
                
                val secondaryGradientAngle by infiniteTransition.animateFloat(
                    initialValue = 360f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(25000, easing = LinearEasing)
                    )
                )
                
                // Animated gradient alpha
                val gradientAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Background gradient layers
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = gradientAlpha),
                                        Color.Black.copy(alpha = gradientAlpha * 0.7f),
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            )
                            .graphicsLayer {
                                rotationZ = primaryGradientAngle
                            }
                    )
                    
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = gradientAlpha * 0.4f),
                                        Color.Black.copy(alpha = gradientAlpha * 0.5f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = gradientAlpha * 0.3f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = gradientAlpha * 0.4f)
                                    )
                                )
                            )
                            .graphicsLayer {
                                rotationZ = secondaryGradientAngle
                            }
                    )

                    // Content
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Animated icon with multiple effects
                        val iconScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        val iconRotationX by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(5000, easing = LinearEasing)
                            )
                        )

                        val iconRotationY by infiniteTransition.animateFloat(
                            initialValue = -10f,
                            targetValue = 10f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                        rotationZ = iconRotationX
                                        rotationY = iconRotationY
                                    },
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "AI Plant Identifier",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 32.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Plant & Disease Identification",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Enhanced AI badge
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        )
                                    )
                                ),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Powered by Advanced AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animate buttons with delay
        AnimatedVisibility(
            visible = isContentVisible,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(1000, delayMillis = 300, easing = EaseOutQuart)
                    ),
        ) {
            Column {
                // Replace existing buttons with AnimatedButton
                AnimatedButton(
                    onClick = {
                        if (apiKey.isNullOrBlank()) {
                            showNoApiKeyDialog = true
                        } else {
                            isFromCamera = true
                            showAnalysisDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    text = "Take Photo",
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedButton(
                    onClick = {
                        if (apiKey.isNullOrBlank()) {
                            showNoApiKeyDialog = true
                        } else {
                            isFromCamera = false
                            showAnalysisDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    text = "Choose from Gallery",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Animate features card
        AnimatedVisibility(
            visible = isContentVisible,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(1000, delayMillis = 600, easing = EaseOutQuart)
                    ),
        ) {
            // Enhanced Key Features Card with animated items
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(vertical = 16.dp)
                    .graphicsLayer {
                        shadowElevation = 16f
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                // Add gradient background
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Background gradient
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        // Animated title with icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val iconRotation by infiniteTransition.animateFloat(
                                initialValue = -10f,
                                targetValue = 10f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )

                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        rotationZ = iconRotation
                                    },
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = "AI-Powered Features",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 22.sp
                            )
                        }

                        // Animated feature items with enhanced visuals
                        features.forEachIndexed { index, feature ->
                            AnimatedVisibility(
                                visible = isContentVisible,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        delayMillis = 1000 + (index * 150)
                                    )
                                ) +
                                slideInVertically(
                                    initialOffsetY = { it + 50 },
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        delayMillis = 1000 + (index * 150),
                                        easing = EaseOutQuart
                                    )
                                ),
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            rotationX = if (isContentVisible) 0f else 30f
                                        },
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Animated checkmark with delayed appearance
                                        val infiniteTransition = rememberInfiniteTransition()
                                        val iconScale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.2f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000),
                                                repeatMode = RepeatMode.Reverse
                                            )
                                        )

                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .graphicsLayer {
                                                    scaleX = iconScale
                                                    scaleY = iconScale
                                                    alpha = if (isContentVisible) 1f else 0f
                                                }
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Text(
                                            text = feature,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.graphicsLayer {
                                                alpha = if (isContentVisible) 1f else 0f
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animate footer
        AnimatedVisibility(
            visible = isContentVisible,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 1200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Settings button with enhanced visibility
                    TextButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(bottom = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "App Settings",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Copyright footer
                    CopyrightFooter()
                }
            }
        }

        // Analysis Type Dialog
        if (showAnalysisDialog) {
            AlertDialog(
                onDismissRequest = { showAnalysisDialog = false },
                title = { Text("Choose Analysis Type") },
                text = { Text("Would you like to perform a disease-specific analysis or general plant identification?") },
                confirmButton = {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                showAnalysisDialog = false
                                if (isFromCamera) {
                                    onNavigateToCamera()
                                } else {
                                    diseaseGalleryLauncher.launch("image/*")
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
                                showAnalysisDialog = false
                                if (isFromCamera) {
                                    onNavigateToCamera()
                                } else {
                                    galleryLauncher.launch("image/*")
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
                        onClick = { showAnalysisDialog = false }
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
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    containerColor: Color,
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Multiple animations for different effects
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val infiniteTransition = rememberInfiniteTransition()
    
    // Subtle glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Enhanced icon animations
    val iconRotationZ by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        )
    )
    
    val iconHoverY by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .padding(vertical = 8.dp)
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 20.dp,
                    spotColor = containerColor.copy(alpha = glowAlpha),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        // Main button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
            ),
            shadowElevation = if (isPressed) 0.dp else 8.dp,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                containerColor,
                                containerColor.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Animated icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                        .graphicsLayer {
                            rotationZ = if (isPressed) 0f else iconRotationZ
                            translationY = iconHoverY
                        }
                ) {
                    icon()
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Button text
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Arrow indicator
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            alpha = if (isPressed) 0.7f else 1f
                            translationX = if (isPressed) -8f else 0f
                        },
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Add this list of features
private val features = listOf(
    "Real-time plant identification",
    "Advanced disease detection",
    "Detailed care instructions",
    "Climate-specific guidance",
    "Smart treatment recommendations"
) 