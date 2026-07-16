package com.kiro.arcade.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.arcade.data.model.Chat
import com.kiro.arcade.data.model.Message
import com.kiro.arcade.data.model.User
import com.kiro.arcade.data.repository.FirebaseRepository
import com.kiro.arcade.ui.components.GlassCard
import com.kiro.arcade.ui.components.GlassTextField
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun ChatsScreen(hazeState: HazeState) {
    val chats by FirebaseRepository.getChats().collectAsState(initial = emptyList())
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var chatUsers by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(chats) {
        val users = mutableMapOf<String, User>()
        chats.forEach { chat ->
            val u = FirebaseRepository.getUser(chat.otherUserUid)
            if (u != null) users[chat.otherUserUid] = u
        }
        chatUsers = users
    }

    if (selectedChat != null) {
        ChatDetailScreen(
            chat = selectedChat!!,
            otherUser = chatUsers[selectedChat!!.otherUserUid],
            hazeState = hazeState,
            onBack = { selectedChat = null }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 56.dp)
    ) {
        Text(
            "Чаты",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Нет чатов.\nПодпишись на кого-нибудь взаимно!",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chats) { chat ->
                    val otherUser = chatUsers[chat.otherUserUid]
                    GlassCard(
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxWidth().clickable { selectedChat = chat }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape)
                                    .border(2.dp, Color(0xFF8B5CF6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    otherUser?.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(otherUser?.username ?: chat.otherUserUid,
                                    color = Color.White, fontWeight = FontWeight.SemiBold)
                                if (chat.lastMessage.isNotEmpty()) {
                                    Text(chat.lastMessage,
                                        color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                                        maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    chat: Chat,
    otherUser: User?,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val messages by FirebaseRepository.getMessages(chat.id).collectAsState(initial = emptyList())
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUid = FirebaseRepository.currentUid

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(otherUser?.username ?: "Чат",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg = msg, isOwn = msg.senderUid == currentUid, hazeState = hazeState)
            }
        }

        // Input
        Row(
            modifier = Modifier.padding(12.dp).padding(bottom = 90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Сообщение...",
                hazeState = hazeState,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    scope.launch {
                        FirebaseRepository.sendMessage(chat.id, text)
                        text = ""
                    }
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF8B5CF6))
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message, isOwn: Boolean, hazeState: HazeState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        GlassCard(
            hazeState = hazeState,
            cornerRadius = 18.dp,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                msg.text,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
