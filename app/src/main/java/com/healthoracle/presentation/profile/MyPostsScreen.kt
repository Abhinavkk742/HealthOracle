package com.healthoracle.presentation.profile // Adjust package if needed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthoracle.presentation.forum.ForumViewModel
import com.healthoracle.presentation.forum.RedditPostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    myPostsViewModel: MyPostsViewModel = hiltViewModel(),
    forumViewModel: ForumViewModel = hiltViewModel() // Used purely for the upvote/downvote triggers
) {
    val myPosts by myPostsViewModel.myPosts.collectAsState()
    val isLoading by myPostsViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Posts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (myPosts.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You haven't posted anything yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myPosts) { post ->
                        // Re-using the exact same card from your main feed!
                        RedditPostCard(
                            post = post,
                            onClick = { onNavigateToPostDetail(post.id) },
                            onUpvote = { forumViewModel.toggleUpvote(post.id) },
                            onDownvote = { forumViewModel.toggleDownvote(post.id) }
                        )
                    }
                }
            }
        }
    }
}