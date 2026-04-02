package com.healthoracle.presentation.doctor

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.healthoracle.data.model.Prescription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScreen(
    patientId: String,
    doctorId: String,
    patientName: String = "",
    onNavigateBack: () -> Unit,
    viewModel: PrescriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDoctor = Firebase.auth.currentUser?.uid == doctorId
    val prescriptions by viewModel.prescriptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // ✅ NEW: State for Full Screen Image
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showUploadDialog = true
        }
    }

    LaunchedEffect(patientId) {
        viewModel.loadPrescriptions(patientId)
    }

    LaunchedEffect(uploadStatus) {
        if (uploadStatus == "Success") {
            showUploadDialog = false
            selectedImageUri = null
            viewModel.clearUploadStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        isDoctor && patientName.isNotBlank() -> "$patientName's Prescriptions"
                        isDoctor -> "Patient Prescriptions"
                        else -> "My Prescriptions"
                    }
                    Text(title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (isDoctor) {
                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Prescription")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && prescriptions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (prescriptions.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Prescriptions Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(prescriptions) { prescription ->
                        PrescriptionCard(
                            prescription = prescription,
                            isDoctor = isDoctor,
                            onImageClick = { url -> fullScreenImageUrl = url },
                            onDeleteClick = { id -> viewModel.deletePrescription(id) }
                        )
                    }
                }
            }

            // Upload Dialog
            if (showUploadDialog) {
                var notes by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = {
                        if (uploadStatus == null || uploadStatus == "Error uploading image" || uploadStatus == "Error saving record") {
                            showUploadDialog = false
                            viewModel.clearUploadStatus()
                        }
                    },
                    title = { Text("Upload Prescription") },
                    text = {
                        Column {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selected Prescription",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Doctor's Notes (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                            if (uploadStatus != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uploadStatus!!,
                                    color = if (uploadStatus!!.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                selectedImageUri?.let { uri ->
                                    viewModel.uploadPrescription(patientId, doctorId, uri, notes)
                                }
                            },
                            enabled = uploadStatus == null || uploadStatus!!.contains("Error")
                        ) {
                            Text("Upload")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showUploadDialog = false
                                viewModel.clearUploadStatus()
                            },
                            enabled = uploadStatus == null || uploadStatus!!.contains("Error")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // ✅ NEW: Full Screen Image Viewer Dialog
        if (fullScreenImageUrl != null) {
            Dialog(
                onDismissRequest = { fullScreenImageUrl = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fullScreenImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full Screen Prescription",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Top Bar for Close and Download
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { fullScreenImageUrl = null },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                downloadImageToGallery(context, fullScreenImageUrl!!)
                                fullScreenImageUrl = null
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Save to Gallery", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrescriptionCard(
    prescription: Prescription,
    isDoctor: Boolean,
    onImageClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(prescription.timestamp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(prescription.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Prescription Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clickable { onImageClick(prescription.imageUrl) }, // ✅ CLICK TO OPEN FULL SCREEN
                contentScale = ContentScale.Crop
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (prescription.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes: ${prescription.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ✅ NEW: Delete Button for Doctors Only
                if (isDoctor) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Prescription",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Deletion
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Prescription?") },
            text = { Text("Are you sure you want to permanently delete this prescription record?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick(prescription.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ NEW: Helper function to securely download and save to the Gallery
fun downloadImageToGallery(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("HealthOracle Prescription")
            .setDescription("Downloading prescription image...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Prescription_${System.currentTimeMillis()}.jpg")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading to Gallery...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
    }
}