package com.healthoracle.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BloodType
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSkinDisease: () -> Unit,
    onNavigateToDiabetes: () -> Unit,
    onNavigateToForum: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HealthOracle",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Your AI-Powered Health Companion",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            HomeFeatureButton(
                label = "Skin Disease Identifier",
                description = "Capture or upload a skin image for AI diagnosis",
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp)) },
                onClick = onNavigateToSkinDisease
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeFeatureButton(
                label = "Diabetes Predictor",
                description = "Enter health metrics to assess diabetes risk",
                icon = { Icon(Icons.Default.BloodType, contentDescription = null, modifier = Modifier.size(28.dp)) },
                onClick = onNavigateToDiabetes
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeFeatureButton(
                label = "Community Forum",
                description = "Connect, share, and learn with others",
                icon = { Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(28.dp)) },
                onClick = onNavigateToForum
            )
        }
    }
}

@Composable
private fun HomeFeatureButton(
    label: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.size(12.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
