package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll // NEW: Added missing import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCommenting by viewModel.isCommenting.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Tracks who the user is replying to
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (post?.authorId == currentUserId) {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Options") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit Post") },
                                onClick = { showMenu = false; showEditDialog = true },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deletePost { success -> if (success) onNavigateBack() }
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 16.dp) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp)) {

                    // Reply Indicator Banner
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Replying to ${replyingTo!!.authorName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Cancel Reply", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Input Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isCommenting
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(commentText, replyingTo?.id, replyingTo?.authorName)
                                    commentText = ""
                                    replyingTo = null // Reset after sending
                                }
                            },
                            enabled = commentText.isNotBlank() && !isCommenting,
                            modifier = Modifier.clip(CircleShape).background(if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.LightGray)
                        ) {
                            if (isCommenting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            else Icon(Icons.Default.Send, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        // Full Screen Edit Dialog for Images
        if (showEditDialog && post != null) {
            EditPostDialog(
                initialTitle = post!!.title,
                initialContent = post!!.content,
                initialImageUrls = post!!.imageUrls,
                onDismiss = { showEditDialog = false },
                onSave = { newTitle, newContent, retainedUrls, newUris ->
                    viewModel.editPost(newTitle, newContent, retainedUrls, newUris) { showEditDialog = false }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                post?.let { currentPost ->
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

                        // Main Post
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentPost.authorName, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(currentPost.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(currentPost.content, style = MaterialTheme.typography.bodyLarge)

                                if (currentPost.imageUrls.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        currentPost.imageUrls.forEach { url ->
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier.width(300.dp).height(200.dp).clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                            }
                        }

                        // Threaded Comments Rendering
                        val topLevelComments = comments.filter { it.replyToCommentId == null }

                        items(topLevelComments) { parentComment ->
                            CommentThreadItem(
                                comment = parentComment,
                                isReply = false,
                                onReplyClick = { replyingTo = parentComment }
                            )

                            // Find and render replies underneath this specific parent
                            val replies = comments.filter { it.replyToCommentId == parentComment.id }
                            replies.forEach { reply ->
                                CommentThreadItem(
                                    comment = reply,
                                    isReply = true,
                                    onReplyClick = { replyingTo = parentComment } // Tapping reply on a reply threads it under the parent
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 48.dp else 16.dp, // Indents replies!
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
    ) {
        Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (isReply) 24.dp else 32.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                if (comment.replyToAuthorName != null && isReply) {
                    Text(" replied to ${comment.replyToAuthorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)

            // Inline Reply Button
            Text(
                text = "Reply",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).clickable { onReplyClick() }
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
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var retainedUrls by remember { mutableStateOf(initialImageUrls) }
    var newUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> newUris = newUris + uris }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false) // Makes it full screen
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Post") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") } },
                    actions = {
                        TextButton(
                            onClick = {
                                isSaving = true
                                onSave(title, content, retainedUrls, newUris)
                            },
                            enabled = title.isNotBlank() && content.isNotBlank() && !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth().height(150.dp))

                Spacer(modifier = Modifier.height(16.dp))
                Text("Images", fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Show old images (can be deleted)
                    retainedUrls.forEach { url ->
                        Box {
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            IconButton(
                                onClick = { retainedUrls = retainedUrls - url },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)
                            ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    }
                    // Show new URIs (can be deleted)
                    newUris.forEach { uri ->
                        Box {
                            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            IconButton(
                                onClick = { newUris = newUris - uri },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)
                            ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, null); Spacer(modifier = Modifier.width(8.dp)); Text("Add More Images")
                }
            }
        }
    }
}