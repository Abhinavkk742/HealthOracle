package com.healthoracle.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthoracle.core.util.OnboardingUtils
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODEL (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color
)

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinishOnboarding: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            title          = "AI Visual Diagnostics",
            description    = "Snap a photo of a skin concern and let our AI instantly identify potential conditions and assess your risk.",
            icon           = Icons.Default.CameraAlt,
            gradientStart  = Color(0xFF0B877D),
            gradientEnd    = Color(0xFF1B8FC5)
        ),
        OnboardingPage(
            title          = "Smart Health Planning",
            description    = "Calculate your diabetes risk and generate personalised 30-day health routines that sync to your calendar.",
            icon           = Icons.Default.MonitorHeart,
            gradientStart  = Color(0xFF4B5FD6),
            gradientEnd    = Color(0xFF7C4DCC)
        ),
        OnboardingPage(
            title          = "Community Support",
            description    = "Connect with others on similar health journeys. Ask questions, share your progress, and lift each other up.",
            icon           = Icons.Default.Forum,
            gradientStart  = Color(0xFF29845A),
            gradientEnd    = Color(0xFF0B877D)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // ── Bottom controls ───────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue   = if (selected) 24.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label         = "indicatorWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            // CTA button
            Button(
                onClick = {
                    if (isLastPage) {
                        scope.launch {
                            OnboardingUtils.setOnboardingComplete(context)
                            onFinishOnboarding()
                        }
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                AnimatedContent(
                    targetState   = isLastPage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label         = "btnLabel"
                ) { last ->
                    Text(
                        if (last) "Get Started" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }

            // Skip
            AnimatedVisibility(visible = !isLastPage) {
                TextButton(
                    onClick  = {
                        scope.launch {
                            OnboardingUtils.setOnboardingComplete(context)
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INDIVIDUAL PAGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon illustration
        Box(
            modifier         = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            page.gradientStart.copy(alpha = 0.18f),
                            page.gradientEnd.copy(alpha = 0.06f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(page.gradientStart, page.gradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = page.icon,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text       = page.title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text      = page.description,
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )

        // Extra bottom space so text isn't hidden under controls
        Spacer(modifier = Modifier.height(200.dp))
    }
}
