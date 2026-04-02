package com.healthoracle.presentation.walktracker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onNavigateToWalkDetail: (Long) -> Unit,
    viewModel: WalkTrackerViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Map view (unchanged — preserving OsmMapView integration) ──────────
        OsmMapView(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            routePoints  = uiState.routePoints,
            currentLat   = uiState.lastLocation?.latitude,
            currentLng   = uiState.lastLocation?.longitude
        )

        // ── Live stats strip (shown only while tracking) ──────────────────────
        AnimatedVisibility(
            visible = uiState.isTracking,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut()
        ) {
            LiveStatsStrip(uiState = uiState, viewModel = viewModel)
        }

        // ── Control row ───────────────────────────────────────────────────────
        WalkControlRow(
            isTracking = uiState.isTracking,
            isPaused   = uiState.isPaused,
            onStart    = { viewModel.startTracking() },
            onPause    = { viewModel.pauseTracking() },
            onResume   = { viewModel.resumeTracking() },
            onStop     = { viewModel.stopAndSave() }
        )

        // ── Session saved confirmation ────────────────────────────────────────
        AnimatedVisibility(visible = uiState.sessionSaved) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Walk saved successfully!",
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Walk History ──────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Walk History",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.weight(1f)
            )
            if (walkHistory.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${walkHistory.size}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (walkHistory.isEmpty()) {
            EmptyWalkHistory()
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(walkHistory) { session ->
                    WalkHistoryCard(
                        session     = session,
                        viewModel   = viewModel,
                        onViewRoute = { onNavigateToWalkDetail(session.id) },
                        onDelete    = { viewModel.deleteSession(session) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIVE STATS STRIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LiveStatsStrip(uiState: WalkTrackerUiState, viewModel: WalkTrackerViewModel) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiveStatChip(
            label    = "Distance",
            value    = viewModel.formatDistance(uiState.distanceMeters),
            icon     = Icons.Outlined.Straighten,
            modifier = Modifier.weight(1f)
        )
        LiveStatChip(
            label    = "Duration",
            value    = viewModel.formatDuration(uiState.durationSeconds),
            icon     = Icons.Outlined.Timer,
            modifier = Modifier.weight(1f)
        )
        LiveStatChip(
            label    = "Steps",
            value    = "${(uiState.distanceMeters / 0.762f).toInt()}",
            icon     = Icons.Outlined.DirectionsWalk,
            modifier = Modifier.weight(1f)
        )
        LiveStatChip(
            label    = "Speed",
            value    = "%.1f km/h".format(uiState.currentSpeed * 3.6f),
            icon     = Icons.Outlined.Speed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LiveStatChip(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WALK CONTROL ROW
// ─────────────────────────────────────────────────────────────────────────────

private val tealGreen = Color(0xFF0B877D)
private val amber     = Color(0xFFE8A020)
private val safeGreen = Color(0xFF29845A)
private val rose      = Color(0xFFD64B6A)

@Composable
private fun WalkControlRow(
    isTracking: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isTracking) {
            // Start button — full width
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = tealGreen)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Walk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            if (!isPaused) {
                Button(
                    onClick  = onPause,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = amber)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick  = onResume,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = safeGreen)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume", fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick  = onStop,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = rose)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop & Save", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WALK HISTORY CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WalkHistoryCard(
    session: WalkSession,
    viewModel: WalkTrackerViewModel,
    onViewRoute: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy  •  hh:mm a", Locale.getDefault())

    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onViewRoute() },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green badge
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(safeGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null,
                    tint = safeGreen, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(session.startTime)),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WalkStat(value = viewModel.formatDistance(session.distanceMeters), icon = Icons.Outlined.Straighten)
                    WalkStat(value = viewModel.formatDuration(session.durationSeconds), icon = Icons.Outlined.Timer)
                    WalkStat(value = "${session.steps}", icon = Icons.Outlined.DirectionsWalk)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun WalkStat(value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyWalkHistory() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("No walks recorded yet",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Start your first walk to see history here",
                style       = MaterialTheme.typography.bodySmall,
                color       = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign   = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PERMISSION REQUEST SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PermissionRequestScreen(onRequest: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint     = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Location Access Needed",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "HealthOracle uses your location to map your walks and calculate accurate distances.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick  = onRequest,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Location Access", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LEGACY STAT CARD (kept for backward compatibility with any callers)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
