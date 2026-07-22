package com.kiro.arcade.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kiro.arcade.data.model.User
import com.kiro.arcade.data.repository.FirebaseRepository
import com.kiro.arcade.data.repository.ImgBBRepository
import com.kiro.arcade.ui.components.GlassButton
import com.kiro.arcade.ui.components.GlassCard
import com.kiro.arcade.ui.components.GlassTextField
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    hazeState: HazeState,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            scope.launch {
                isUploadingPhoto = true
                errorMsg = ""
                val result = ImgBBRepository.uploadImage(context, it)
                result.fold(
                    onSuccess = { url ->
                        avatarUrl = url
                        isUploadingPhoto = false
                    },
                    onFailure = { e ->
                        errorMsg = "Ошибка загрузки фото: ${e.message}"
                        isUploadingPhoto = false
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        user = FirebaseRepository.getCurrentUser()
        user?.let {
            username = it.username
            bio = it.bio
            avatarUrl = it.avatarUrl
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                "Редактировать профиль",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Avatar picker
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF8B5CF6), CircleShape)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploadingPhoto) {
                CircularProgressIndicator(
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.size(32.dp)
                )
            } else if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Фото",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Text(
            "Нажми чтобы изменить фото",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        "Никнейм",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                    GlassTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().replace(" ", "_") },
                        placeholder = "username",
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text(
                        "О себе",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                    GlassTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 150) bio = it },
                        placeholder = "Расскажи о себе...",
                        hazeState = hazeState,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    )
                    Text(
                        "${bio.length}/150",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }

        if (errorMsg.isNotEmpty()) {
            Text(
                errorMsg,
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (successMsg.isNotEmpty()) {
            Text(
                successMsg,
                color = Color(0xFF22C55E),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        GlassButton(
            onClick = {
                if (username.isBlank()) { errorMsg = "Никнейм не может быть пустым"; return@GlassButton }
                scope.launch {
                    isLoading = true
                    errorMsg = ""
                    val result = FirebaseRepository.updateProfile(username, bio, avatarUrl)
                    isLoading = false
                    result.fold(
                        onSuccess = {
                            successMsg = "Профиль сохранён!"
                            kotlinx.coroutines.delay(1000)
                            onSaved()
                        },
                        onFailure = { errorMsg = it.message ?: "Ошибка" }
                    )
                }
            },
            hazeState = hazeState,
            enabled = !isLoading && !isUploadingPhoto,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Сохранить", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
