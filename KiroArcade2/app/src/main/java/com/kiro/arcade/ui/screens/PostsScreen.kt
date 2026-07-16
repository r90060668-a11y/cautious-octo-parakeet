package com.kiro.arcade.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.kiro.arcade.data.model.Post
import com.kiro.arcade.data.repository.FirebaseRepository
import com.kiro.arcade.ui.components.GlassButton
import com.kiro.arcade.ui.components.GlassCard
import com.kiro.arcade.ui.components.GlassTextField
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

@Composable
fun PostsScreen(
    hazeState: HazeState,
    onNavigateToProfile: (String) -> Unit
) {
    val posts by FirebaseRepository.getPostsFeed().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Post>>(emptyList()) }
    var showNewPost by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 60.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Search bar
                GlassTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        scope.launch {
                            if (it.isNotBlank()) {
                                val users = FirebaseRepository.searchUsers(it)
                                val postResults = FirebaseRepository.searchPosts(it)
                                searchResults = postResults
                            } else {
                                searchResults = emptyList()
                            }
                        }
                    },
                    placeholder = "Поиск постов и людей...",
                    hazeState = hazeState,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Empty state
            if (posts.isEmpty() && searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Посты отдыхают,",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "dabb скоро все решит",
                                    color = Color(0xFF8B5CF6),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            val displayPosts = if (searchResults.isNotEmpty()) searchResults else posts
            items(displayPosts) { post ->
                PostCard(
                    post = post,
                    hazeState = hazeState,
                    onAvatarClick = { onNavigateToProfile(post.authorUid) }
                )
            }
        }

        // Publish button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .hazeEffect(state = hazeState, style = HazeMaterials.thin())
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            ) {
                Button(
                    onClick = { showNewPost = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Опубликовать", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showNewPost) {
        NewPostDialog(
            hazeState = hazeState,
            onDismiss = { showNewPost = false },
            onPost = { caption, imageUrl ->
                scope.launch {
                    FirebaseRepository.createPost(caption, imageUrl)
                    showNewPost = false
                }
            }
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    hazeState: HazeState,
    onAvatarClick: () -> Unit
) {
    GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF8B5CF6), CircleShape)
                        .clickable { onAvatarClick() }
                ) {
                    if (post.authorAvatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                post.authorUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    post.authorUsername,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // Image
            if (post.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            }

            // Caption
            if (post.caption.isNotEmpty()) {
                Text(
                    post.caption,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun NewPostDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1428),
        title = { Text("Новый пост", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    placeholder = "Ссылка на изображение (URL)",
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = "Подпись к посту...",
                    hazeState = hazeState,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { onPost(caption, imageUrl) },
                hazeState = hazeState,
                modifier = Modifier.width(120.dp).height(44.dp)
            ) {
                Text("Опубликовать", color = Color.White, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
