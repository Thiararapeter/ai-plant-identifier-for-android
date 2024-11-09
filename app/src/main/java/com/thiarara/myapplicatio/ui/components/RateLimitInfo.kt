package com.thiarara.myapplicatio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thiarara.myapplicatio.api.GeminiService
import kotlinx.coroutines.delay

@Composable
fun RateLimitInfo(
    remainingRequests: Int,
    initialTimeUntilReset: Long
) {
    var timeUntilReset by remember { mutableStateOf(initialTimeUntilReset) }
    var showInfo by remember { mutableStateOf(true) }
    
    // Update the timer every second
    LaunchedEffect(initialTimeUntilReset) {
        while (timeUntilReset > 0 && showInfo) {
            delay(1000)
            timeUntilReset = GeminiService.getTimeUntilReset()
            if (timeUntilReset <= 0) {
                showInfo = false
            }
        }
    }

    if (showInfo) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Remaining Requests: $remainingRequests",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Resets in: ${(timeUntilReset / 1000)} seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
} 