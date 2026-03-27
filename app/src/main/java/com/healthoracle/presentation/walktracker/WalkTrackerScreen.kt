package com.healthoracle.presentation.walktracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.healthoracle.data.local.entity.WalkSession
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkTrackerScreen(
    viewModel: WalkTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val walkHistory by viewModel.walkHistory.collectAsState(initial = emptyList())

    val locationPermission = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (!locationPermission.allPermissionsGranted) {
        PermissionRequestScreen(onRequest = { locationPermission.launchMultiplePermissionRequest() })
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Map (top half)
        OsmMapView(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
            routePoints = uiState.routePoints,
            currentLat = uiState.lastLocation?.latitude,
            currentLng = uiState.lastLocation?.longitude
        )

        // Stats Row
        if (uiState.isTracking) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(label = "Distance", value = viewModel.formatDistance(uiState.distanceMeters))
                StatCard(label = "Duration", value = viewModel.formatDuration(uiState.durationSeconds))
                StatCard(label = "Est. Steps", value = "${(uiState.distanceMeters / 0.762f).toInt()}")
                StatCard(label = "Speed", value = "%.1f km/h".format(uiState.currentSpeed * 3.6f))
            }
        }

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!uiState.isTracking) {
                Button(
                    onClick = { viewModel.startTracking() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01696f))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Walk", fontSize = 16.sp)
                }
            } else {
                if (!uiState.isPaused) {
                    Button(
                        onClick = { viewModel.pauseTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFda7101))
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Pause")
                    }
                } else {
                    Button(
                        onClick = { viewModel.resumeTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF437a22))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Resume")
                    }
                }

                Button(
                    onClick = { viewModel.stopAndSave() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFa12c7b))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop & Save")
                }
            }
        }

        if (uiState.sessionSaved) {
            Text(
                "✅ Walk saved!",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(4.dp),
                color = Color(0xFF437a22),
                fontWeight = FontWeight.SemiBold
            )
        }

        // Walk History
        Text(
            "Walk History",
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        if (walkHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No walks yet. Start your first walk!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(walkHistory) { session ->
                    WalkHistoryCard(
                        session = session,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF01696f))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WalkHistoryCard(
    session: WalkSession,
    viewModel: WalkTrackerViewModel,
    onViewRoute: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy  hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewRoute() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = Color(0xFF01696f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(session.startTime)),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "${viewModel.formatDistance(session.distanceMeters)}  •  ${viewModel.formatDuration(session.durationSeconds)}  •  ${session.steps} steps",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ArrowForwardIos,
                contentDescription = "View route",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF01696f)
        )
        Spacer(Modifier.height(16.dp))
        Text("Location Permission Required", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "HealthOracle needs your location to track your walks on the map.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01696f))
        ) {
            Text("Grant Location Permission")
        }
    }
}
