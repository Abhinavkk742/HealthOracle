package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.Comment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val post         by viewModel.post.collectAsState()
    val comments     by viewModel.comments.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val isCommenting by viewModel.isCommenting.collectAsState()

    var commentText      by remember { mutableStateOf("") }
    var showMenu         by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var replyingTo       by remember { mutableStateOf<Comment?>(null) }
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title            = { Text("Report Post") },
            text             = { Text("Report this post for inappropriate content? Our moderators will review it.") },
            confirmButton    = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Report", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Full-screen image viewer
    if (selectedImageUrl != null) {
        Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties       = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                var scale  by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                AsyncImage(
                    model              = selectedImageUrl,
                    contentDescription = null,
                    modifier           = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                val maxX = (size.width  * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                        .graphicsLayer(
                            scaleX       = scale,
                            scaleY       = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale       = ContentScale.Fit
                )
                IconButton(
                    onClick  = { selectedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (post?.authorId == currentUserId) {
                            DropdownMenuItem(
                                text        = { Text("Edit Post") },
                                onClick     = { showMenu = false; showEditDialog = true },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text        = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
                                onClick     = {
                                    showMenu = false
                                    viewModel.deletePost { if (it) onNavigateBack() }
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        } else {
                            DropdownMenuItem(
                                text        = { Text("Report Post", color = MaterialTheme.colorScheme.error) },
                                onClick     = { showMenu = false; showReportDialog = true },
                                leadingIcon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {

                    // Reply preview
                    AnimatedVisibility(visible = replyingTo != null) {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Reply, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Replying to ${replyingTo?.authorName}",
                                color      = MaterialTheme.colorScheme.primary,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.weight(1f)
                            )
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value         = commentText,
                            onValueChange = { commentText = it },
                            placeholder   = {
                                Text(if (replyingTo != null) "Write a reply..." else "Add a comment...")
                            },
                            modifier  = Modifier.weight(1f),
                            shape     = RoundedCornerShape(24.dp),
                            singleLine = true,
                            enabled   = !isCommenting,
                            colors    = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick  = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(commentText, replyingTo?.id, replyingTo?.authorName)
                                    commentText = ""
                                    replyingTo  = null
                                }
                            },
                            enabled  = commentText.isNotBlank() && !isCommenting,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            if (isCommenting) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color       = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (showEditDialog && post != null) {
            EditPostDialog(
                initialTitle     = post!!.title,
                initialContent   = post!!.content,
                initialImageUrls = post!!.imageUrls,
                onDismiss        = { showEditDialog = false },
                onSave           = { t, c, retained, newUris ->
                    viewModel.editPost(t, c, retained, newUris) { showEditDialog = false }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                post?.let { currentPost ->
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!currentPost.authorProfileUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model              = currentPost.authorProfileUrl,
                                            contentDescription = null,
                                            modifier           = Modifier.size(32.dp).clip(CircleShape),
                                            contentScale       = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier         = Modifier.size(32.dp).clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                currentPost.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                style      = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        currentPost.authorName,
                                        fontWeight = FontWeight.Bold,
                                        style      = MaterialTheme.typography.bodyMedium
                                    )
                                    if (currentPost.authorRole == "doctor") {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Verified, null,
                                            tint     = Color(0xFF1DA1F2),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(currentPost.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(currentPost.content, style = MaterialTheme.typography.bodyLarge)

                                if (currentPost.imageUrls.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier              = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        currentPost.imageUrls.forEach { url ->
                                            AsyncImage(
                                                model              = url,
                                                contentDescription = null,
                                                modifier           = Modifier
                                                    .width(280.dp).height(180.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { selectedImageUrl = url },
                                                contentScale       = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Comments",
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val topLevelComments = comments.filter { it.replyToCommentId == null }
                        items(topLevelComments) { parentComment ->
                            CommentThreadItem(
                                comment      = parentComment,
                                isReply      = false,
                                onReplyClick = { replyingTo = parentComment }
                            )
                            comments.filter { it.replyToCommentId == parentComment.id }.forEach { reply ->
                                CommentThreadItem(
                                    comment      = reply,
                                    isReply      = true,
                                    onReplyClick = { replyingTo = parentComment }
                                )
                            }
                            HorizontalDivider(
                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentThreadItem(comment: Comment, isReply: Boolean, onReplyClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            start  = if (isReply) 44.dp else 16.dp,
            end    = 16.dp,
            top    = 12.dp,
            bottom = 12.dp
        )
    ) {
        val sz = if (isReply) 22.dp else 28.dp
        if (!comment.authorProfileUrl.isNullOrEmpty()) {
            AsyncImage(
                model              = comment.authorProfileUrl,
                contentDescription = null,
                modifier           = Modifier.size(sz).clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
        } else {
            Box(
                modifier         = Modifier.size(sz).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    comment.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                if (comment.authorRole == "doctor") {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(Icons.Default.Verified, null, tint = Color(0xFF1DA1F2), modifier = Modifier.size(13.dp))
                }
                if (comment.replyToAuthorName != null && isReply) {
                    Text(
                        " → ${comment.replyToAuthorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Reply",
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(top = 6.dp).clickable { onReplyClick() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostDialog(
    initialTitle: String,
    initialContent: String,
    initialImageUrls: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>, List<Uri>) -> Unit
) {
    var title        by remember { mutableStateOf(initialTitle) }
    var content      by remember { mutableStateOf(initialContent) }
    var retainedUrls by remember { mutableStateOf(initialImageUrls) }
    var newUris      by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSaving     by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> newUris = newUris + uris }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title          = { Text("Edit Post", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") } },
                    actions        = {
                        TextButton(
                            onClick  = { isSaving = true; onSave(title, content, retainedUrls, newUris) },
                            enabled  = title.isNotBlank() && content.isNotBlank() && !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Title") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value         = content,
                    onValueChange = { content = it },
                    label         = { Text("Content") },
                    modifier      = Modifier.fillMaxWidth().height(140.dp),
                    shape         = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Images", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier              = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    retainedUrls.forEach { url ->
                        Box {
                            AsyncImage(
                                model              = url,
                                contentDescription = null,
                                modifier           = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale       = ContentScale.Crop
                            )
                            IconButton(
                                onClick  = { retainedUrls = retainedUrls - url },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(0.55f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    newUris.forEach { uri ->
                        Box {
                            AsyncImage(
                                model              = uri,
                                contentDescription = null,
                                modifier           = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale       = ContentScale.Crop
                            )
                            IconButton(
                                onClick  = { newUris = newUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(0.55f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick  = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add More Images")
                }
            }
        }
    }
}
