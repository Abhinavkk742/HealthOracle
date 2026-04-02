package com.healthoracle.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.healthoracle.core.dashboard.*
import com.healthoracle.core.ui.components.*
import com.healthoracle.core.ui.theme.*
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// HOME SCREEN
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Chat FAB preserved — only shown when patient has assigned doctor
            val p = state.profile
            if (p.role == "patient" && !p.assignedDoctorId.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToChat(p.uid, p.assignedDoctorId!!, "My Doctor") },
                    icon    = { Icon(Icons.Default.Chat, contentDescription = null) },
                    text    = { Text("Chat", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero Header ───────────────────────────────────────────────────
            HeroHeader(
                name      = state.profile.name,
                avatarUrl = state.profile.profilePictureUrl,
                onAvatar  = onNavigateToProfile
            )

            // ── Health Score Card ─────────────────────────────────────────────
            state.healthScore?.let { score ->
                HealthScoreCard(
                    score     = score,
                    data      = state.healthData,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Streak Banner ─────────────────────────────────────────────────
            if (state.streakDays >= 2) {
                StreakBanner(days = state.streakDays, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(20.dp))
            }

            // ── Smart Insights ────────────────────────────────────────────────
            if (state.insights.isNotEmpty()) {
                SectionHeader(
                    title       = "Smart Insights",
                    actionLabel = "See all",
                    modifier    = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding        = PaddingValues(horizontal = 16.dp)
                ) {
                    items(state.insights) { insight ->
                        val color = insightColor(insight.accentColorKey)
                        InsightChip(
                            emoji       = insight.emoji,
                            message     = insight.message,
                            highlight   = insight.highlight,
                            accentColor = color,
                            onClick     = {}
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Today's Metrics Grid ──────────────────────────────────────────
            SectionHeader(
                title       = "Today's Metrics",
                actionLabel = "Goals →",
                modifier    = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            MetricsGrid(
                data              = state.healthData,
                onSteps           = onNavigateToWalkTracker,
                onWater           = {},
                onSleep           = {},
                onMood            = {},
                modifier          = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))

            // ── Mood Logger ───────────────────────────────────────────────────
            MoodLogger(
                currentMood = state.healthData.moodScore,
                onSelect    = viewModel::logMood,
                modifier    = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))

            // ── Weekly Chart ──────────────────────────────────────────────────
            SectionHeader(
                title    = "Weekly Steps",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            WeeklyStepsChart(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(24.dp))

            // ── Feature Tiles ─────────────────────────────────────────────────
            SectionHeader(
                title    = "Health Tools",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            FeatureTilesGrid(
                onSkinDisease   = onNavigateToSkinDisease,
                onDiabetes      = onNavigateToDiabetes,
                onWalkTracker   = onNavigateToWalkTracker,
                onCalendar      = onNavigateToCalendar,
                onHistory       = onNavigateToHistory,
                onTodo          = onNavigateToTodo,
                onForum         = onNavigateToForum,
                onPrescriptions = {
                    onNavigateToPrescriptions(
                        state.profile.uid,
                        state.profile.assignedDoctorId ?: "none"
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(80.dp)) // FAB clearance
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(
    name: String,
    avatarUrl: String?,
    onAvatar: () -> Unit
) {
    val hour     = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else      -> "Good night"
    }
    val firstName = name.split(" ").firstOrNull() ?: name

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text  = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "$firstName ✦",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, AccentViolet)
                        )
                    )
                    .clickable(onClick = onAvatar)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model              = avatarUrl,
                        contentDescription = "Avatar",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(
                        text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEALTH SCORE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(
    score: HealthScore,
    data: DailyHealthData,
    modifier: Modifier = Modifier
) {
    GradientCard(
        modifier       = modifier.fillMaxWidth(),
        gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF13172E)),
        borderColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(
                        text          = "HEALTH SCORE",
                        style         = MaterialTheme.typography.labelSmall,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = score.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    val deltaSign  = if (score.delta >= 0) "↑ +${score.delta}" else "↓ ${score.delta}"
                    val deltaColor = if (score.delta >= 0) AccentGreen else AccentRose
                    Text(
                        text  = "$deltaSign pts from yesterday",
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor
                    )
                }

                CircularProgressRing(
                    progress        = score.total / 100f,
                    size            = 96.dp,
                    strokeWidth     = 8.dp,
                    progressColors  = listOf(Primary, Teal)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "${score.total}",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFFA5B4FC)
                        )
                        Text(
                            text  = "/ 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Metric mini chips row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    icon       = "👟",
                    value      = data.steps.formatK(),
                    label      = "steps",
                    valueColor = PrimaryLight,
                    modifier   = Modifier.weight(1f)
                )
                MetricChip(
                    icon       = "💧",
                    value      = "${data.waterGlasses}/${data.waterGoal}",
                    label      = "glasses",
                    valueColor = Teal,
                    modifier   = Modifier.weight(1f)
                )
                MetricChip(
                    icon       = "🌙",
                    value      = if (data.sleepHours > 0f) "${data.sleepHours}h" else "--",
                    label      = "sleep",
                    valueColor = AccentViolet,
                    modifier   = Modifier.weight(1f)
                )
                MetricChip(
                    icon       = "😊",
                    value      = moodLabel(data.moodScore),
                    label      = "mood",
                    valueColor = AccentAmber,
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STREAK BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StreakBanner(days: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = AccentAmber.copy(alpha = 0.1f),
        border   = BorderStroke(0.5.dp, AccentAmber.copy(alpha = 0.3f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔥", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "$days-day streak!",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = AccentAmber
                )
                Text(
                    text  = "You've logged health data $days days in a row",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// METRICS GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricsGrid(
    data: DailyHealthData,
    onSteps: () -> Unit,
    onWater: () -> Unit,
    onSleep: () -> Unit,
    onMood: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                icon       = "👟",
                title      = "Steps",
                value      = data.steps.formatK(),
                goal       = data.stepGoal.formatK(),
                progress   = data.steps.toFloat() / data.stepGoal,
                barColor   = Primary,
                trend      = "+12%",
                trendUp    = true,
                onClick    = onSteps,
                modifier   = Modifier.weight(1f)
            )
            MetricCard(
                icon       = "💧",
                title      = "Water",
                value      = "${data.waterGlasses}",
                goal       = "${data.waterGoal} glasses",
                progress   = data.waterGlasses.toFloat() / data.waterGoal,
                barColor   = Teal,
                trend      = if (data.waterGlasses < data.waterGoal) "↓ behind" else "✓ done",
                trendUp    = data.waterGlasses >= data.waterGoal,
                onClick    = onWater,
                modifier   = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                icon       = "🌙",
                title      = "Sleep",
                value      = if (data.sleepHours > 0f) "${"%.1f".format(data.sleepHours)}h" else "--",
                goal       = "${data.sleepGoal.toInt()}h goal",
                progress   = if (data.sleepGoal > 0f) data.sleepHours / data.sleepGoal else 0f,
                barColor   = AccentViolet,
                trend      = if (data.sleepHours >= data.sleepGoal) "✓ great" else "↓ low",
                trendUp    = data.sleepHours >= data.sleepGoal,
                onClick    = onSleep,
                modifier   = Modifier.weight(1f)
            )
            MetricCard(
                icon       = "😊",
                title      = "Mood",
                value      = moodEmoji(data.moodScore),
                goal       = moodLabel(data.moodScore),
                progress   = data.moodScore / 5f,
                barColor   = AccentAmber,
                trend      = "→ Stable",
                trendUp    = null,
                onClick    = onMood,
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: String,
    title: String,
    value: String,
    goal: String,
    progress: Float,
    barColor: Color,
    trend: String,
    trendUp: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier  = modifier,
        onClick   = onClick,
        glowColor = barColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                val trendColor = when (trendUp) {
                    true  -> AccentGreen
                    false -> AccentRose
                    null  -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = trendColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text     = trend,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = trendColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Goal: $goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(10.dp))
            AnimatedProgressBar(
                progress   = progress,
                color      = barColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MOOD LOGGER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoodLogger(
    currentMood: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = listOf("😞" to "Rough", "😐" to "Meh", "😊" to "Good", "😄" to "Great", "🤩" to "Amazing")

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "How are you feeling today?",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moods.forEachIndexed { index, (emoji, label) ->
                    val score    = index + 1
                    val selected = currentMood == score
                    val scale by animateFloatAsState(
                        targetValue   = if (selected) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label         = "moodScale$index"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) AccentAmber.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)
                            )
                            .border(
                                width = if (selected) 1.dp else 0.5.dp,
                                color = if (selected) AccentAmber.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(score) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(emoji, fontSize = 22.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) AccentAmber
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WEEKLY STEPS CHART
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyStepsChart(modifier: Modifier = Modifier) {
    // Placeholder data — in production wire up from Room / Firestore
    val days = listOf(
        DayBarData("Mon", 0.72f),
        DayBarData("Tue", 0.53f),
        DayBarData("Wed", 0.88f),
        DayBarData("Thu", 0.61f),
        DayBarData("Fri", 0.79f),
        DayBarData("Sat", 0.45f),
        DayBarData("Today", 0.66f, isToday = true)
    )

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            WeeklyBarChart(data = days, modifier = Modifier.height(80.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE TILES GRID  (2-column, preserving all original navigation targets)
// ─────────────────────────────────────────────────────────────────────────────

private data class FeatureTile(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val badge: String? = null,
    val onClick: () -> Unit
)

@Composable
private fun FeatureTilesGrid(
    onSkinDisease: () -> Unit,
    onDiabetes: () -> Unit,
    onWalkTracker: () -> Unit,
    onCalendar: () -> Unit,
    onHistory: () -> Unit,
    onTodo: () -> Unit,
    onForum: () -> Unit,
    onPrescriptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tiles = listOf(
        FeatureTile("🔬", "Skin Scanner", "AI visual diagnosis",
            listOf(Color(0xFF4C0519), Color(0xFF881337)), "TFLite", onSkinDisease),
        FeatureTile("❤️", "Diabetes Check", "Risk assessment",
            listOf(Color(0xFF1E1B4B), Color(0xFF312E81)), "AI", onDiabetes),
        FeatureTile("🗺️", "Walk Tracker", "GPS route & history",
            listOf(Color(0xFF064E3B), Color(0xFF065F46)), "GPS", onWalkTracker),
        FeatureTile("📅", "Appointments", "Schedule & reminders",
            listOf(Color(0xFF0C4A6E), Color(0xFF075985)), null, onCalendar),
        FeatureTile("📋", "Health History", "Past AI reports",
            listOf(Color(0xFF1C1917), Color(0xFF292524)), null, onHistory),
        FeatureTile("✅", "Today's Tasks", "Daily to-do list",
            listOf(Color(0xFF1A1306), Color(0xFF2D1E08)), null, onTodo),
        FeatureTile("💊", "Prescriptions", "From your doctor",
            listOf(Color(0xFF0F172A), Color(0xFF1E293B)), null, onPrescriptions),
        FeatureTile("💬", "Community", "Connect & share",
            listOf(Color(0xFF042F2E), Color(0xFF064E3B)), null, onForum)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile ->
                    FeatureTileItem(tile = tile, modifier = Modifier.weight(1f))
                }
                // Fill empty slot in last row if odd count
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureTileItem(tile: FeatureTile, modifier: Modifier = Modifier) {
    GradientCard(
        modifier       = modifier,
        gradientColors = tile.gradientColors,
        onClick        = tile.onClick,
        shape          = RoundedCornerShape(18.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            tile.badge?.let { badge ->
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape    = RoundedCornerShape(8.dp),
                    color    = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text     = badge,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column {
                Text(tile.emoji, fontSize = 26.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = tile.title,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = tile.subtitle,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun Int.formatK(): String =
    if (this >= 1_000) "${this / 1_000}.${(this % 1_000) / 100}k" else "$this"

private fun moodLabel(score: Int) = when (score) {
    1 -> "Rough"; 2 -> "Meh"; 3 -> "Good"; 4 -> "Great"; 5 -> "Amazing"; else -> "Not logged"
}

private fun moodEmoji(score: Int) = when (score) {
    1 -> "😞"; 2 -> "😐"; 3 -> "😊"; 4 -> "😄"; 5 -> "🤩"; else -> "--"
}

@Composable
private fun insightColor(key: InsightColor): Color = when (key) {
    InsightColor.BLUE  -> MaterialTheme.colorScheme.primary
    InsightColor.GREEN -> AccentGreen
    InsightColor.AMBER -> AccentAmber
    InsightColor.ROSE  -> AccentRose
}
