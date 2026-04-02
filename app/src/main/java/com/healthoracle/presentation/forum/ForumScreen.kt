package com.healthoracle.presentation.forum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.ForumPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val posts        by viewModel.posts.collectAsState()
    val selectedSort by viewModel.sortBy.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Forum", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onNavigateToCreatePost,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                icon           = { Icon(Icons.Default.Add, "Create Post") },
                text           = { Text("Create Post", fontWeight = FontWeight.SemiBold) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder   = { Text("Search discussions...") },
                leadingIcon   = {
                    Icon(
                        Icons.Default.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon  = {
                    AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape         = RoundedCornerShape(28.dp),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            // Sort chips
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Hot", Icons.Default.LocalFireDepartment, "Hot"),
                    Triple("New", Icons.Default.NewReleases,         "New"),
                    Triple("Top", Icons.Default.ThumbUp,             "Top")
                ).forEach { (label, icon, sort) ->
                    SortChip(
                        label      = label,
                        icon       = icon,
                        isSelected = selectedSort == sort,
                        onClick    = { viewModel.setSortMethod(sort) }
                    )
                }
            }

            if (posts.isEmpty() && searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        RedditPostCard(
                            post       = post,
                            onClick    = {
                                viewModel.incrementViewCount(post.id)
                                onNavigateToPostDetail(post.id)
                            },
                            onUpvote   = { viewModel.toggleUpvote(post.id) },
                            onDownvote = { viewModel.toggleDownvote(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected    = isSelected,
        onClick     = onClick,
        label       = { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors      = FilterChipDefaults.filterChipColors(
            selectedContainerColor   = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor       = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        shape = CircleShape
    )
}

@Composable
fun RedditPostCard(
    post: ForumPost,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""
    val hasUpvoted    = post.upvotedBy.contains(currentUserId)
    val hasDownvoted  = post.downvotedBy.contains(currentUserId)
    val upvoteColor   = if (hasUpvoted)   Color(0xFFFF4500) else MaterialTheme.colorScheme.onSurfaceVariant
    val downColor     = if (hasDownvoted) Color(0xFF7193FF) else MaterialTheme.colorScheme.onSurfaceVariant
    val scoreColor    = when {
        hasUpvoted   -> Color(0xFFFF4500)
        hasDownvoted -> Color(0xFF7193FF)
        else         -> MaterialTheme.colorScheme.onSurface
    }

    var showCardMenu     by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title            = { Text("Report Post") },
            text             = { Text("Report this post to moderators?") },
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

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color    = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            // Author row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!post.authorProfileUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model              = post.authorProfileUrl,
                        contentDescription = "Avatar",
                        modifier           = Modifier.size(28.dp).clip(CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier         = Modifier.size(28.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = post.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    post.authorName,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (post.authorRole == "doctor") {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.Default.Verified, contentDescription = null,
                        tint     = Color(0xFF1DA1F2),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    " · ${post.timeAgo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(onClick = { showCardMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.MoreVert, contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = showCardMenu, onDismissRequest = { showCardMenu = false }) {
                        DropdownMenuItem(
                            text        = { Text("Report Post") },
                            onClick     = { showCardMenu = false; showReportDialog = true },
                            leadingIcon = { Icon(Icons.Default.Flag, null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                post.title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                post.content,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    post.imageUrls.forEach { url ->
                        AsyncImage(
                            model              = url,
                            contentDescription = null,
                            modifier           = Modifier
                                .fillMaxWidth(if (post.imageUrls.size > 1) 0.85f else 1f)
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale       = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vote + comment row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onUpvote, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.ArrowUpward, null, tint = upvoteColor, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            post.upvotes.toString(),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = scoreColor,
                            modifier   = Modifier.padding(horizontal = 2.dp)
                        )
                        IconButton(onClick = onDownvote, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.ArrowDownward, null, tint = downColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape    = RoundedCornerShape(50),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            post.commentCount.toString(),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Visibility, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        post.viewCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier  = Modifier.fillMaxWidth(),
            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )
    }
}
