package com.healthoracle.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthoracle.core.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// GLASS CARD  — translucent surface with a subtle top-border glow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    showTopGlow: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val border = if (showTopGlow) glowColor.copy(alpha = 0.3f)
                 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Surface(
        modifier  = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape     = shape,
        color     = base,
        border    = androidx.compose.foundation.BorderStroke(0.5.dp, border),
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GRADIENT CARD  — dark gradient background, colored top accent strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    borderColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Subtle top-left glow overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = 400f
                    )
                )
        )
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CIRCULAR PROGRESS RING  — animated arc for health score / progress
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CircularProgressRing(
    progress: Float,            // 0f–1f
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    progressColors: List<Color> = listOf(Primary, Teal),
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "ringProgress"
    )

    Box(
        modifier         = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val sweep = animatedProgress * 360f
        val stroke = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = this.size.minDimension - stroke
            val topLeft  = Offset(stroke / 2, stroke / 2)
            val arcSize  = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Track
            drawArc(
                color       = trackColor,
                startAngle  = 0f,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                brush      = Brush.sweepGradient(progressColors),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER  — consistent section label + optional action link
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.CenterVertically
    ) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text      = actionLabel,
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.primary,
                modifier  = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// METRIC CHIP  — compact icon + value tile used in health score breakdown
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MetricChip(
    icon: String,               // emoji
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ICON BADGE  — rounded-square icon container
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier         = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(size * 0.5f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS BAR  — labeled horizontal bar with animated fill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedProgressBar(
    progress: Float,            // 0f–1f
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
    height: Dp = 5.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "progressBar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.8f))))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WEEKLY BAR CHART  — 7-day bar visualisation
// ─────────────────────────────────────────────────────────────────────────────
data class DayBarData(
    val label: String,
    val value: Float,   // 0f–1f normalised
    val isToday: Boolean = false
)

@Composable
fun WeeklyBarChart(
    data: List<DayBarData>,
    barColor: List<Color> = listOf(Primary, PrimaryLight),
    todayColor: List<Color> = listOf(Teal, Primary),
    modifier: Modifier = Modifier,
    barHeightMax: Dp = 72.dp
) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.Bottom
    ) {
        data.forEach { day ->
            val animatedHeight by animateFloatAsState(
                targetValue   = day.value,
                animationSpec = tween(700, easing = FastOutSlowInEasing),
                label         = "bar_${day.label}"
            )
            val colors = if (day.isToday) todayColor else barColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier            = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(barHeightMax * animatedHeight.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brush.verticalGradient(colors))
                        .then(
                            if (day.isToday) Modifier.drawBehind {
                                // ring around today bar
                                drawRoundRect(
                                    color       = colors.first().copy(alpha = 0.4f),
                                    style       = Stroke(width = 2.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )
                            } else Modifier
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INSIGHT CHIP  — horizontal swipeable AI insight card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InsightChip(
    emoji: String,
    message: String,
    highlight: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column {
            // Top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f))))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = buildAnnotatedString(message, highlight, accentColor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun buildAnnotatedString(
    message: String,
    highlight: String,
    color: Color
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val start = message.indexOf(highlight)
        if (start == -1) {
            append(message)
        } else {
            append(message.substring(0, start))
            pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color      = color,
                    fontWeight = FontWeight.SemiBold
                )
            )
            append(highlight)
            pop()
            append(message.substring(start + highlight.length))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAV BAR
// ─────────────────────────────────────────────────────────────────────────────
data class NavItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@Composable
fun HealthOracleBottomBar(
    items: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(item.route) },
                icon     = {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.label,
                        modifier           = Modifier.size(22.dp)
                    )
                },
                label    = {
                    Text(
                        text  = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
