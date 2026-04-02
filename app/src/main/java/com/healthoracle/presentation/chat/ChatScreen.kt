package com.healthoracle.presentation.chat

// All business logic, swipe-to-reply, message bubble, delete dialog — preserved exactly.
// Changes: topbar uses primaryContainer gradient pill, input bar is cleaner, bubble shape
// slightly refined, timestamp/status row polished.

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.healthoracle.data.model.ChatMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactName: String,
    contactProfileUrl: String? = null,
    currentUserId: String,
    messages: List<ChatMessage>,
    onSendMessage: (String, Uri?) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var textInput                    by remember { mutableStateOf("") }
    var selectedImageUri             by remember { mutableStateOf<Uri?>(null) }
    val replyingTo                   by viewModel.replyingToMessage.collectAsState()
    var selectedMessageForOptions    by remember { mutableStateOf<ChatMessage?>(null) }
    var fullScreenImageUrl           by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    LaunchedEffect(messages) { viewModel.markMessagesAsSeen() }

    // Full-screen image viewer (logic unchanged)
    if (fullScreenImageUrl != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUrl = null },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                SubcomposeAsyncImage(
                    model              = fullScreenImageUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Fit,
                    loading            = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }}
                )
                IconButton(
                    onClick  = { fullScreenImageUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }

    // Delete message dialog (logic unchanged)
    if (selectedMessageForOptions != null) {
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            title            = { Text("Delete Message") },
            text             = { Text("Delete this message for everyone?") },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.deleteMessage(selectedMessageForOptions!!.messageId)
                    selectedMessageForOptions = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton    = {
                TextButton(onClick = { selectedMessageForOptions = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        // Contact avatar
                        if (!contactProfileUrl.isNullOrEmpty()) {
                            AsyncImage(model = contactProfileUrl, contentDescription = null,
                                modifier     = Modifier.size(36.dp).clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f), CircleShape),
                                contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center) {
                                Text(contactName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(contactName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor     = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor  = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {

                    // Reply preview
                    if (replyingTo != null) {
                        Row(
                            modifier          = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (replyingTo?.senderId == currentUserId) "Replying to yourself"
                                    else "Replying to $contactName",
                                    color      = MaterialTheme.colorScheme.primary,
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    when {
                                        replyingTo?.isDeleted == true                          -> "Deleted message"
                                        replyingTo?.messageText?.isNotBlank() == true          -> replyingTo!!.messageText
                                        else                                                   -> "Image"
                                    },
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style    = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.setReplyTo(null) }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }

                    // Selected image preview
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 10.dp)) {
                            SubcomposeAsyncImage(
                                model              = selectedImageUri,
                                contentDescription = null,
                                modifier           = Modifier.size(88.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale       = ContentScale.Crop,
                                loading            = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }}
                            )
                            IconButton(
                                onClick  = { selectedImageUri = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .size(22.dp).background(Color.Black.copy(0.55f), CircleShape)
                            ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                        }
                    }

                    // Input row
                    Row(
                        modifier          = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value         = textInput,
                            onValueChange = { textInput = it },
                            placeholder   = { Text("Type a message...") },
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(28.dp),
                            singleLine    = false,
                            maxLines      = 4,
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = Color.Transparent,
                                unfocusedBorderColor    = Color.Transparent,
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val canSend = textInput.isNotBlank() || selectedImageUri != null
                        IconButton(
                            onClick  = {
                                if (canSend) {
                                    onSendMessage(textInput, selectedImageUri)
                                    textInput        = ""
                                    selectedImageUri = null
                                }
                            },
                            enabled  = canSend,
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout  = true
        ) {
            items(messages) { message ->
                SwipeToReplyWrapper(onSwipe = { viewModel.setReplyTo(message) }) {
                    MessageBubble(
                        message           = message,
                        allMessages       = messages,
                        isFromCurrentUser = message.senderId == currentUserId,
                        contactProfileUrl = contactProfileUrl,
                        onLongPress       = { selectedMessageForOptions = message },
                        onImageClick      = { url -> fullScreenImageUrl = url }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SWIPE TO REPLY  (logic unchanged, just moved here)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SwipeToReplyWrapper(onSwipe: () -> Unit, content: @Composable () -> Unit) {
    val offsetX      = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic         = LocalHapticFeedback.current
    var triggered    by remember { mutableStateOf(false) }

    Box(
        modifier         = Modifier.fillMaxWidth().pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd    = { coroutineScope.launch { offsetX.animateTo(0f, tween(300)) }; triggered = false },
                onDragCancel = { coroutineScope.launch { offsetX.animateTo(0f, tween(300)) }; triggered = false },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        val newOffset = (offsetX.value + dragAmount).coerceIn(0f, 150f)
                        offsetX.snapTo(newOffset)
                        if (newOffset > 100f && !triggered) {
                            triggered = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwipe()
                        } else if (newOffset < 100f && triggered) {
                            triggered = false
                        }
                    }
                }
            )
        },
        contentAlignment = Alignment.CenterStart
    ) {
        if (offsetX.value > 10f) {
            val fraction = (offsetX.value / 100f).coerceIn(0f, 1f)
            Box(
                modifier         = Modifier.padding(start = 12.dp).size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = fraction)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Reply, null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = fraction),
                    modifier = Modifier.size(20.dp * fraction))
            }
        }
        Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) { content() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MESSAGE BUBBLE  (all logic identical, bubble shape slightly tightened)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    allMessages: List<ChatMessage>,
    isFromCurrentUser: Boolean,
    contactProfileUrl: String?,
    onLongPress: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val formatter  = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(message.timestamp))

    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment    = Alignment.Bottom
    ) {
        // Contact avatar for incoming messages
        if (!isFromCurrentUser) {
            if (!contactProfileUrl.isNullOrEmpty()) {
                AsyncImage(model = contactProfileUrl, contentDescription = null,
                    modifier     = Modifier.size(26.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier            = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart    = 18.dp, topEnd    = 18.dp,
                            bottomStart = if (isFromCurrentUser) 18.dp else 4.dp,
                            bottomEnd   = if (isFromCurrentUser) 4.dp  else 18.dp
                        )
                    )
                    .background(
                        if (isFromCurrentUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .combinedClickable(
                        onClick    = { },
                        onLongClick = { if (isFromCurrentUser && !message.isDeleted) onLongPress() }
                    )
                    .padding(2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                    if (message.isDeleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(14.dp),
                                tint = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("This message was deleted",
                                style     = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color     = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                    } else {
                        // Reply preview
                        val showReplyPreview = message.replyToMessageId != null || !message.replyToMessageText.isNullOrBlank()
                        if (showReplyPreview) {
                            val liveParent      = message.replyToMessageId?.let { id -> allMessages.find { it.messageId == id } }
                            val isParentDeleted = liveParent?.isDeleted == true || message.replyToMessageText == "Deleted Message"
                            val replyPreviewText = when {
                                isParentDeleted                                          -> "Deleted message"
                                liveParent?.messageText?.isNotBlank() == true           -> liveParent.messageText
                                message.replyToMessageText?.isNotBlank() == true        -> message.replyToMessageText
                                else                                                     -> "Image"
                            }

                            Surface(
                                modifier = Modifier.padding(bottom = 6.dp),
                                shape    = RoundedCornerShape(8.dp),
                                color    = Color.Black.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text("Replied message",
                                        style     = MaterialTheme.typography.labelSmall,
                                        color     = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(0.65f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f))
                                    Text(replyPreviewText ?: "",
                                        style     = MaterialTheme.typography.bodySmall,
                                        fontStyle = if (isParentDeleted) FontStyle.Italic else FontStyle.Normal,
                                        color     = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(if (isParentDeleted) 0.55f else 1f)
                                        else MaterialTheme.colorScheme.onSurface.copy(if (isParentDeleted) 0.55f else 1f),
                                        maxLines  = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        // Image
                        if (message.imageUrl != null) {
                            SubcomposeAsyncImage(
                                model              = message.imageUrl,
                                contentDescription = null,
                                modifier           = Modifier.fillMaxWidth(0.68f)
                                    .heightIn(min = 140.dp, max = 240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(message.imageUrl) },
                                contentScale       = ContentScale.Crop,
                                loading            = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp,
                                        color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                }}
                            )
                            if (message.messageText.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Text
                        if (message.messageText.isNotBlank()) {
                            Text(
                                text  = message.messageText,
                                color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Timestamp + status
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(timeString, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                if (isFromCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val statusText  = when (message.status) { "seen" -> "Seen"; "delivered" -> "Delivered"; else -> "Sent" }
                    val statusColor = if (message.status == "seen") Color(0xFF34B7F1)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    Text("· $statusText", style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = if (message.status == "seen") FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}