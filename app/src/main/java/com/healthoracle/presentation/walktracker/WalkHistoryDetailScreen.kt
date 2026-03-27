package com.healthoracle.presentation.walktracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.healthoracle.data.local.entity.WalkSession
import com.healthoracle.data.model.RoutePoint
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkHistoryDetailScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WalkTrackerViewModel = hiltViewModel()
) {
    val walkHistory by viewModel.walkHistory.collectAsState(initial = emptyList())
    val session = walkHistory.find { it.id == sessionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Walk Route", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            WalkRouteContent(
                session = session,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun WalkRouteContent(
    session: WalkSession,
    viewModel: WalkTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val gson = remember { Gson() }
    val routePoints: List<RoutePoint> = remember(session.routePointsJson) {
        try {
            val type = object : TypeToken<List<RoutePoint>>() {}.type
            gson.fromJson(session.routePointsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val startPoint = routePoints.firstOrNull()
    val endPoint = routePoints.lastOrNull()
    val dateFormat = SimpleDateFormat("MMM d, yyyy  hh:mm a", Locale.getDefault())

    Column(modifier = modifier.fillMaxSize()) {

        // Map showing the route
        OsmMapView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
            routePoints = routePoints,
            currentLat = endPoint?.latitude,
            currentLng = endPoint?.longitude,
            showStartEndMarkers = true,
            startPoint = startPoint,
            endPoint = endPoint
        )

        // Stats
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = dateFormat.format(Date(session.startTime)),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Distance", value = viewModel.formatDistance(session.distanceMeters))
                StatItem(label = "Duration", value = viewModel.formatDuration(session.durationSeconds))
                StatItem(label = "Steps", value = "${session.steps}")
            }

            // Route info
            if (routePoints.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Route Info", fontWeight = FontWeight.Bold)
                        Text(
                            "📍 Start: ${"%.5f".format(startPoint?.latitude)}, ${"%.5f".format(startPoint?.longitude)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "🏁 End: ${"%.5f".format(endPoint?.latitude)}, ${"%.5f".format(endPoint?.longitude)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "📊 GPS Points: ${routePoints.size}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF01696f))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
