package com.healthoracle.presentation.aisuggestion

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthoracle.core.util.PdfGenerator
import com.healthoracle.presentation.calendar.NotificationScheduler
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSuggestionScreen(
    conditionName: String,
    conditionSource: String,
    onNavigateBack: () -> Unit,
    viewModel: AiSuggestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // NEW: Initialize our custom Notification Scheduler
    val scheduler = remember { NotificationScheduler(context) }
    var isSyncing by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                if (uiState is AiSuggestionUiState.Success) {
                    val advice = (uiState as AiSuggestionUiState.Success).advice
                    PdfGenerator.generatePdf(context, it, conditionName, advice)
                    Toast.makeText(context, "PDF Saved Successfully!", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    LaunchedEffect(key1 = conditionName) {
        if (uiState is AiSuggestionUiState.Initial) {
            viewModel.fetchHealthTimetable(conditionName, conditionSource)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("AI Health Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is AiSuggestionUiState.Initial,
                is AiSuggestionUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing condition and generating personalized plan...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is AiSuggestionUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Plan for: $conditionName",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = state.advice,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 26.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // NEW: Single automated sync button replacing the 3 old buttons
                        Button(
                            onClick = {
                                isSyncing = true
                                val morningText = extractSection(state.advice, "🌅 MORNING", listOf("☀️ AFTERNOON", "🌙 EVENING", "🥗 DIETARY", "⚠️ PRECAUTION"))
                                val afternoonText = extractSection(state.advice, "☀️ AFTERNOON", listOf("🌙 EVENING", "🥗 DIETARY", "⚠️ PRECAUTION"))
                                val eveningText = extractSection(state.advice, "🌙 EVENING", listOf("🥗 DIETARY", "⚠️ PRECAUTION"))

                                viewModel.addPlanToLocalCalendar(
                                    conditionName = conditionName,
                                    morningAdvice = morningText,
                                    afternoonAdvice = afternoonText,
                                    eveningAdvice = eveningText,
                                    onScheduleAlarm = { id, title, time, date ->
                                        scheduler.schedule(id, title, time, date)
                                    },
                                    onComplete = {
                                        isSyncing = false
                                        Toast.makeText(context, "30-Day Plan securely synced to your appointments!", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Syncing...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.EventAvailable, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Sync 30-Day Plan to My Appointments", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val safeName = conditionName.replace(" ", "_")
                                pdfLauncher.launch("HealthOracle_${safeName}_Plan.pdf")
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Download as PDF Document",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                is AiSuggestionUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Oops! Something went wrong.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Button(
                            onClick = { viewModel.fetchHealthTimetable(conditionName, conditionSource) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

// Function stays exactly the same to parse the AI output
private fun extractSection(fullText: String, startHeader: String, nextHeaders: List<String>): String {
    val startIndex = fullText.indexOf(startHeader)
    if (startIndex == -1) return fullText

    var minEndIndex = fullText.length
    for (header in nextHeaders) {
        val index = fullText.indexOf(header, startIndex)
        if (index != -1 && index < minEndIndex) {
            minEndIndex = index
        }
    }
    return fullText.substring(startIndex, minEndIndex).trim()
}