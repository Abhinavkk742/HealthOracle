package com.healthoracle.presentation.skin

// ─── SkinDiseaseScreen ────────────────────────────────────────────────────────
// All logic (camera, gallery, TFLite classification, AI suggestions) preserved.
// UI: premium image preview box, refined result cards, better loading state.

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinDiseaseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiSuggestion: (String) -> Unit,
    viewModel: SkinDiseaseViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File.createTempFile("skin_", ".jpg", context.cacheDir)
            cameraImageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { uri ->
            uriToBitmap(context, uri)?.let { viewModel.onImageSelected(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uriToBitmap(context, it)?.let { bitmap -> viewModel.onImageSelected(bitmap) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skin Disease Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Image preview ─────────────────────────────────────────────────
            ImagePreviewBox(bitmap = uiState.selectedBitmap)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Camera + Gallery buttons ──────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick  = {
                        val file = File.createTempFile("skin_", ".jpg", context.cacheDir)
                        val uri  = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        cameraImageUri = uri
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick  = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Photo, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Loading ───────────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyzing image...", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            uiState.errorMessage?.let { error ->
                Surface(shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("⚠ $error", modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Results ───────────────────────────────────────────────────────
            AnimatedVisibility(
                visible        = uiState.hasResult && uiState.predictionResult != null,
                enter          = fadeIn() + slideInVertically { it },
                exit           = fadeOut()
            ) {
                uiState.predictionResult?.let { result ->
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Primary result card
                        ElevatedCard(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.elevatedCardElevation(2.dp),
                            colors    = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier            = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Detected Condition",
                                    style    = MaterialTheme.typography.labelLarge,
                                    color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(result.label,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign  = TextAlign.Center,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier          = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Confidence",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                                    Text("${(result.confidence * 100).roundToInt()}%",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { result.confidence },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color    = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }

                        // Other possibilities
                        if (result.allResults.size > 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Other Possibilities",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier   = Modifier.padding(start = 4.dp, bottom = 8.dp))
                            ElevatedCard(
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.elevatedCardElevation(1.dp),
                                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    result.allResults.drop(1).forEach { (label, confidence) ->
                                        OtherPredictionRow(label = label, confidence = confidence)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Disclaimer
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.Info, null,
                                    tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI prediction for informational purposes only. Consult a licensed dermatologist for an accurate diagnosis.",
                                    style  = MaterialTheme.typography.bodySmall,
                                    color  = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick  = { onNavigateToAiSuggestion(result.label) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Psychology, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get AI Health Suggestions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick  = { viewModel.clearResult() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Text("Try Another Image", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IMAGE PREVIEW BOX
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImagePreviewBox(bitmap: Bitmap?) {
    Box(
        modifier         = Modifier.fillMaxWidth().height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap       = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier     = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CameraAlt, null,
                        modifier = Modifier.size(36.dp),
                        tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("No image selected",
                    color  = MaterialTheme.colorScheme.onSurfaceVariant,
                    style  = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Use Camera or Gallery below",
                    color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style  = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OTHER PREDICTION ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OtherPredictionRow(label: String, confidence: Float) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(148.dp), color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress  = { confidence },
            modifier  = Modifier.weight(1f).height(6.dp).clip(CircleShape),
            color     = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("${(confidence * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// URI → BITMAP  (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
} catch (e: Exception) { null }


// ═════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ═════════════════════════════════════════════════════════════════════════════
