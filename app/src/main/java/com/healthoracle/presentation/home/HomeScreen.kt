package com.healthoracle.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.healthoracle.presentation.profile.ProfileViewModel
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS (local to this screen — no logic change)
// ─────────────────────────────────────────────────────────────────────────────

private data class FeatureCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

private data class SectionData(
    val heading: String,
    val cards: List<FeatureCard>
)

// ─────────────────────────────────────────────────────────────────────────────
// GREETING HELPER
// ─────────────────────────────────────────────────────────────────────────────

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else      -> "Good night"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN  (all navigation params / ViewModel kept identical)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSkinDisease: () -> Unit,
    onNavigateToDiabetes: () -> Unit,
    onNavigateToForum: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToWalkTracker: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onNavigateToPrescriptions: (patientId: String, doctorId: String) -> Unit,
    onNavigateToChat: (patientId: String, doctorId: String, contactName: String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.uiState.collectAsState()
    val profile = profileState.profile

    // ── Define accent colors per feature (not random — health-semantic) ──────
    val teal   = Color(0xFF0B877D)
    val indigo = Color(0xFF4B5FD6)
    val amber  = Color(0xFFE8A020)
    val rose   = Color(0xFFD64B6A)
    val violet = Color(0xFF7C4DCC)
    val green  = Color(0xFF29845A)
    val sky    = Color(0xFF1B8FC5)

    val sections = listOf(
        SectionData(
            heading = "Health Diagnostics",
            cards = listOf(
                FeatureCard("Skin Disease Scanner", "AI-powered visual diagnosis", Icons.Default.CameraAlt,     rose,   onNavigateToSkinDisease),
                FeatureCard("Diabetes Risk Check",  "Assess your health metrics",  Icons.Default.MonitorHeart, indigo, onNavigateToDiabetes)
            )
        ),
        SectionData(
            heading = "Fitness",
            cards = listOf(
                FeatureCard("Walk Tracker", "GPS-mapped walks & history", Icons.Default.DirectionsWalk, green, onNavigateToWalkTracker)
            )
        ),
        SectionData(
            heading = "My Data",
            cards = listOf(
                FeatureCard("Health History",     "Saved AI plans & results",         Icons.Default.History,      teal,   onNavigateToHistory),
                FeatureCard("Appointments",       "Manage health appointments",        Icons.Default.DateRange,    sky,    onNavigateToCalendar),
                FeatureCard("Today's Tasks",      "Calendar to-do list for today",     Icons.Default.CheckCircle,  amber,  onNavigateToTodo),
                FeatureCard("My Prescriptions",   "View prescriptions from doctor",    Icons.Default.Description,  violet) {
                    onNavigateToPrescriptions(profile.uid, profile.assignedDoctorId ?: "none")
                }
            )
        ),
        SectionData(
            heading = "Community",
            cards = listOf(
                FeatureCard("Community Forum", "Connect, share & learn", Icons.Default.Forum, green, onNavigateToForum)
            )
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                name         = profile.name,
                avatarUrl    = profile.profilePictureUrl,
                onAvatarClick = onNavigateToProfile
            )
        },
        floatingActionButton = {
            // Preserved exactly — only visible when patient has assigned doctor
            if (profile.role == "patient" && !profile.assignedDoctorId.isNullOrEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter   = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onNavigateToChat(profile.uid, profile.assignedDoctorId!!, "My Doctor")
                        },
                        icon = {
                            Icon(Icons.Default.Chat, contentDescription = null)
                        },
                        text = {
                            Text("Message Doctor", fontWeight = FontWeight.SemiBold)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                        modifier       = Modifier.shadow(8.dp, RoundedCornerShape(50))
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero greeting banner ─────────────────────────────────────
            HeroBanner(name = profile.name.ifBlank { "there" })

            Spacer(modifier = Modifier.height(8.dp))

            // ── Quick-stats strip ────────────────────────────────────────
            QuickStatStrip()

            Spacer(modifier = Modifier.height(24.dp))

            // ── Feature sections ─────────────────────────────────────────
            sections.forEach { section ->
                FeatureSection(section = section)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(96.dp)) // FAB clearance
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    name: String,
    avatarUrl: String?,
    onAvatarClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "HealthOracle",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            // Notification bell placeholder
            IconButton(onClick = { /* future: notifications */ }) {
                Icon(
                    imageVector     = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint            = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Avatar
            IconButton(onClick = onAvatarClick) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model              = avatarUrl,
                        contentDescription = "Profile",
                        modifier           = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier        = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                text  = "${getGreeting()},",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = name.split(" ").firstOrNull() ?: name,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "How are you feeling today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
            )
        }

        // Decorative circle — purely visual
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp, y = (-8).dp)
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 24.dp)
                .background(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = CircleShape
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK STAT STRIP  (static display — add ViewModel data when available)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickStatStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatChip(
            label    = "Steps",
            value    = "—",
            icon     = Icons.Default.DirectionsWalk,
            modifier = Modifier.weight(1f)
        )
        QuickStatChip(
            label    = "Active",
            value    = "—",
            icon     = Icons.Default.FitnessCenter,
            modifier = Modifier.weight(1f)
        )
        QuickStatChip(
            label    = "Tasks",
            value    = "—",
            icon     = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatChip(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureSection(section: SectionData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section header
        Text(
            text       = section.heading,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier   = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Grid or list depending on count
        if (section.cards.size == 2) {
            // Side-by-side compact cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                section.cards.forEach { card ->
                    CompactFeatureCard(card = card, modifier = Modifier.weight(1f))
                }
            }
        } else {
            // Full-width list cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.cards.forEach { card ->
                    ListFeatureCard(card = card)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPACT FEATURE CARD  (used when 2 cards sit side-by-side)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactFeatureCard(
    card: FeatureCard,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue  = if (pressed) 0.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label        = "cardElevation"
    )

    ElevatedCard(
        modifier  = modifier
            .clickable(
                onClick     = card.onClick,
                onClickLabel = card.title
            ),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon badge
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(card.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = card.icon,
                    contentDescription = null,
                    tint               = card.accentColor,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text       = card.title,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = card.subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST FEATURE CARD  (full-width row card)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ListFeatureCard(card: FeatureCard) {
    var pressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue  = if (pressed) 0.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label        = "listCardElevation"
    )

    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = card.onClick, onClickLabel = card.title),
        shape     = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            // Coloured icon badge
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(card.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = card.icon,
                    contentDescription = null,
                    tint               = card.accentColor,
                    modifier           = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = card.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = card.subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector        = Icons.Rounded.ArrowForwardIos,
                contentDescription = "Open",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}
