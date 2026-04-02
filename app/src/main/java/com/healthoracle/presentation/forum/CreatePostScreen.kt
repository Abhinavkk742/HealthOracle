package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    var title       by remember { mutableStateOf("") }
    var content     by remember { mutableStateOf("") }
    var imageUris   by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
        onResult = { uris -> imageUris = uris }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick  = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                isUploading = true
                                viewModel.createPost(title, content, imageUris) { success, message ->
                                    isUploading = false
                                    if (success) {
                                        onNavigateBack()
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please add a title and content.")
                                }
                            }
                        },
                        enabled  = !isUploading && title.isNotBlank() && content.isNotBlank(),
                        shape    = RoundedCornerShape(50),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Post", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Title field
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                placeholder   = {
                    Text(
                        "An interesting title...",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor          = MaterialTheme.colorScheme.primary
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )

            // Body field
            OutlinedTextField(
                value         = content,
                onValueChange = { content = it },
                placeholder   = {
                    Text(
                        "What are your thoughts?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 160.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors   = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor          = MaterialTheme.colorScheme.primary
                )
            )

            // Image previews
            if (imageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imageUris.forEach { uri ->
                        Box(modifier = Modifier.width(180.dp)) {
                            AsyncImage(
                                model              = uri,
                                contentDescription = null,
                                modifier           = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale       = ContentScale.Crop
                            )
                            IconButton(
                                onClick  = { imageUris = imageUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Icon(
                                    Icons.Default.Close, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick  = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (imageUris.isEmpty()) "Attach Images" else "Replace Images",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
