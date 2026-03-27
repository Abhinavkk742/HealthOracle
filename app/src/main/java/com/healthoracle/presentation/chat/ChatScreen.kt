package com.healthoracle.presentation.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.healthoracle.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactName: String,
    currentUserId: String,
    messages: List<ChatMessage>,
    onSendMessage: (String, Uri?) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var textInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val replyingTo by viewModel.replyingToMessage.collectAsState()

    // Manage Selected Message for Long Press Menu
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(messages) {
        viewModel.markMessagesAsSeen()
    }

    // Long Press Action Dialog
    if (selectedMessageForOptions != null) {
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title = { Text("Message Options") },
            text = { Text("What would you like to do with this message?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReplyTo(selectedMessageForOptions)
                    selectedMessageForOptions = null
                }) {
                    Text("Reply")
                }
            },
            dismissButton = {
                // Only allow deletion if the current user sent it and it isn't already deleted
                if (selectedMessageForOptions?.senderId == currentUserId && selectedMessageForOptions?.isDeleted == false) {
                    TextButton(onClick = {
                        viewModel.deleteMessage(selectedMessageForOptions!!.messageId)
                        selectedMessageForOptions = null
                    }) {
                        Text("Delete for Everyone", color = Color.Red)
                    }
                } else {
                    TextButton(onClick = { selectedMessageForOptions = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Reply Preview
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (replyingTo?.senderId == currentUserId) "Replying to yourself" else "Replying to $contactName",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (replyingTo?.isDeleted == true) "Deleted Message" else (replyingTo?.messageText ?: "Image"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.setReplyTo(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Reply")
                            }
                        }
                    }

                    // Image Preview
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Text Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach Photo", tint = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank() || selectedImageUri != null) {
                                    onSendMessage(textInput, selectedImageUri)
                                    textInput = ""
                                    selectedImageUri = null
                                }
                            },
                            enabled = textInput.isNotBlank() || selectedImageUri != null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (textInput.isNotBlank() || selectedImageUri != null) MaterialTheme.colorScheme.primary else Color.LightGray)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = true
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isFromCurrentUser = message.senderId == currentUserId,
                    onLongPress = { selectedMessageForOptions = message }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    onLongPress: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromCurrentUser) 16.dp else 0.dp,
                        bottomEnd = if (isFromCurrentUser) 0.dp else 16.dp
                    )
                )
                .background(
                    if (isFromCurrentUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .combinedClickable(
                    onClick = { /* Do nothing on tap */ },
                    onLongClick = onLongPress // Triggers Dialog
                )
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                // --- Deleted State ---
                if (message.isDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // --- Quoted Reply Section ---
                    if (message.replyToMessageText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.1f))
                                .padding(8.dp)
                                .padding(bottom = 4.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Replied Message",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = message.replyToMessageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // --- Attached Image Section ---
                    if (message.imageUrl != null) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Shared Image",
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .heightIn(max = 250.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // --- Main Text Section ---
                    if (message.messageText.isNotBlank()) {
                        Text(
                            text = message.messageText,
                            color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- TEXT-BASED Read Receipts ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFromCurrentUser) {
                Spacer(modifier = Modifier.width(6.dp))

                val statusText = when (message.status) {
                    "seen" -> "Seen"
                    "delivered" -> "Delivered"
                    else -> "Sent"
                }

                // Blue color if seen, otherwise gray
                val statusColor = if (message.status == "seen") Color(0xFF34B7F1) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                Text(
                    text = "• $statusText",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = if (message.status == "seen") FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}