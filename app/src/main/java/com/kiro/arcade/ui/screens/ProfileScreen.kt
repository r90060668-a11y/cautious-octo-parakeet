package com.kiro.arcade.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
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
import com.kiro.arcade.data.model.User
import com.kiro.arcade.data.repository.FirebaseRepository
import com.kiro.arcade.ui.components.GlassButton
import com.kiro.arcade.ui.components.GlassCard
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    uid: String?,
    hazeState: HazeState,
    onBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val targetUid = uid ?: FirebaseRepository.currentUid ?: return
    val isOwnProfile = targetUid == FirebaseRepository.currentUid
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(targetUid) {
        user = FirebaseRepository.getUser(targetUid)
        if (!isOwnProfile) {
            isFollowing = FirebaseRepository.isFollowing(targetUid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Text(
                user?.username ?: "Профиль",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (isOwnProfile && onLogout != null) {
                IconButton(onClick = {
                    FirebaseRepository.logout()
                    onLogout()
                }) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF8B5CF6), CircleShape)
        ) {
            val avatarUrl = user?.avatarUrl ?: ""
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
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
                        user?.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "@${user?.username ?: ""}",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        if (user?.bio?.isNotEmpty() == true) {
            Text(
                user?.bio ?: "",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Stats
        GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Подписчики", value = user?.followersCount ?: 0)
                Divider(
                    modifier = Modifier.height(40.dp).width(1.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )
                StatItem(label = "Подписки", value = user?.followingCount ?: 0)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Follow / unfollow button
        if (!isOwnProfile) {
            GlassButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        if (isFollowing) {
                            FirebaseRepository.unfollowUser(targetUid)
                        } else {
                            FirebaseRepository.followUser(targetUid)
                        }
                        isFollowing = !isFollowing
                        user = FirebaseRepository.getUser(targetUid)
                        isLoading = false
                    }
                },
                hazeState = hazeState,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        if (isFollowing) "Отписаться" else "Подписаться",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Hosts placeholder
        Spacer(Modifier.height(40.dp))
        Text(
            "Хосты скоро появятся",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}
