package com.healthoracle.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSkinDisease: () -> Unit,
    onNavigateToDiabetes: () -> Unit,
    onNavigateToForum: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit // NEW PARAMETER
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Hello there,",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "How are you feeling today?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Health Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            DashboardCard(
                title = "Skin Disease Identifier",
                subtitle = "AI-powered visual diagnosis",
                icon = Icons.Default.CameraAlt,
                onClick = onNavigateToSkinDisease
            )

            Spacer(modifier = Modifier.height(16.dp))

            DashboardCard(
                title = "Diabetes Predictor",
                subtitle = "Assess your risk metrics",
                icon = Icons.Default.MonitorHeart,
                onClick = onNavigateToDiabetes
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "My Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // NEW: History Dashboard Card
            DashboardCard(
                title = "My History",
                subtitle = "View saved AI timetables",
                icon = Icons.Default.History,
                onClick = onNavigateToHistory
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Community",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            DashboardCard(
                title = "Community Forum",
                subtitle = "Connect, share, and learn",
                icon = Icons.Default.Forum,
                onClick = onNavigateToForum
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForwardIos,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}