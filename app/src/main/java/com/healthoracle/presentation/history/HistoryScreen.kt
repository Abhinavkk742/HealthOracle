package com.healthoracle.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.healthoracle.data.model.HealthRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// --- VIEWMODEL ---
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _records = MutableStateFlow<List<HealthRecord>>(emptyList())
    val records: StateFlow<List<HealthRecord>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadHistory() }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId)
                    .collection("history")
                    .orderBy("dateMillis", Query.Direction.DESCENDING)
                    .get().await()

                val historyList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(HealthRecord::class.java)?.copy(id = doc.id)
                }
                _records.value = historyList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRecord(recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .collection("history").document(recordId).delete().await()
                _records.value = _records.value.filter { it.id != recordId }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// --- UI SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            } else if (records.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No history yet.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Generate an AI plan to see it here!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(records) { record ->
                        HistoryCard(record = record, onDelete = { viewModel.deleteRecord(record.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(record: HealthRecord, onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(record.dateMillis))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = record.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = dateString, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = record.description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )

            if (!isExpanded) {
                Text(
                    text = "Tap to read more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}