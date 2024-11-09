package com.thiarara.myapplicatio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)

val changelog = listOf(
    ChangelogEntry(
        version = "1.0.3",
        date = "March 20, 2024",
        changes = listOf(
            "Added image zoom controls with slider",
            "Added permission handling for camera and storage",
            "Improved image preview capabilities",
            "Added reset view button for image preview",
            "Enhanced user experience with permission explanations",
            "Added disease-only analysis mode",
            "Added dark mode support",
            "Added camera grid toggle"
        )
    ),
    ChangelogEntry(
        version = "1.0.2",
        date = "March 2024",
        changes = listOf(
            "Added photography pro tips",
            "Improved UI design with green theme",
            "Enhanced camera controls",
            "Added API key management",
            "Better error handling",
            "Improved rate limit information display",
            "Performance improvements",
            "Added detailed disease analysis"
        )
    ),
    ChangelogEntry(
        version = "1.0.1",
        date = "February 2024",
        changes = listOf(
            "Added gallery image selection",
            "Improved camera focus",
            "Better image quality handling",
            "Fixed UI layout issues",
            "Added basic error handling",
            "Performance optimizations",
            "Improved plant identification accuracy"
        )
    ),
    ChangelogEntry(
        version = "1.0.0",
        date = "February 2024",
        changes = listOf(
            "Initial release",
            "Basic plant identification",
            "Camera and gallery support",
            "Simple settings menu",
            "Basic UI implementation",
            "API integration"
        )
    )
)

@Composable
fun ChangelogScreen(
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "What's New",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Changelog entries
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(changelog) { entry ->
                ChangelogEntryCard(entry)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Back button
        OutlinedButton(
            onClick = onBackPressed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ChangelogEntryCard(entry: ChangelogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Version and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version ${entry.version}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = entry.date,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Changes
            entry.changes.forEach { change ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = change,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
} 