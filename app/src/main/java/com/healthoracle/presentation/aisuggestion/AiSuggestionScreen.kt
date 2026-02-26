package com.healthoracle.presentation.aisuggestion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSuggestionScreen(
    conditionName: String,
    conditionSource: String,
    onNavigateBack: () -> Unit,
    viewModel: AiSuggestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger the API call once when the screen is first composed
    LaunchedEffect(key1 = conditionName) {
        if (uiState is AiSuggestionUiState.Initial) {
            viewModel.fetchHealthTimetable(conditionName, conditionSource)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Health Timetable") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is AiSuggestionUiState.Initial,
                is AiSuggestionUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating personalized health timetable...")
                    }
                }

                is AiSuggestionUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Condition: $conditionName",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = state.advice,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is AiSuggestionUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = {
                            viewModel.fetchHealthTimetable(conditionName, conditionSource)
                        }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}