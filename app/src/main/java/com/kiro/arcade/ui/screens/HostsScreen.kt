package com.kiro.arcade.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.arcade.data.repository.FirebaseRepository
import com.kiro.arcade.data.repository.ZeroTierRepository
import com.kiro.arcade.ui.components.GlassButton
import com.kiro.arcade.ui.components.GlassCard
import com.kiro.arcade.ui.components.GlassTextField
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

data class HostInfo(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val networkId: String = "",
    val ownerUid: String = "",
    val ownerUsername: String = "",
    val playerCount: Int = 0,
    val maxPlayers: Int = 25,
    val online: Boolean = true,
    val timestamp: Long = 0L
)

val MINECRAFT_VERSIONS = listOf(
    "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21.0",
    "1.20.80", "1.20.73", "1.20.50", "1.20.40", "1.20.32",
    "1.20.15", "1.20.10", "1.20.0",
    "1.19.83", "1.19.73", "1.19.63", "1.19.51", "1.19.41",
    "1.19.31", "1.19.22", "1.19.11", "1.19.2", "1.19.0"
)

@Composable
fun HostsScreen(hazeState: HazeState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var hosts by remember { mutableStateOf<List<HostInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf<HostInfo?>(null) }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loadHosts { hosts = it; isLoading = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 60.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Хосты", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Minecraft Bedrock серверы", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                    GlassButton(
                        onClick = { showCreateDialog = true },
                        hazeState = hazeState,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Создать хост", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            if (errorMsg.isNotEmpty()) {
                item {
                    GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                        Text(errorMsg, color = Color(0xFFFF6B6B), fontSize = 13.sp,
                            modifier = Modifier.padding(4.dp))
                    }
                }
            }

            if (successMsg.isNotEmpty()) {
                item {
                    GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                        Text(successMsg, color = Color(0xFF22C55E), fontSize = 13.sp,
                            modifier = Modifier.padding(4.dp))
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                    }
                }
            } else if (hosts.isEmpty()) {
                item {
                    GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎮", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Нет активных хостов", color = Color.White,
                                fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Создай первый Minecraft сервер!", color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            } else {
                items(hosts) { host ->
                    HostCard(
                        host = host,
                        hazeState = hazeState,
                        onJoin = { showJoinDialog = host },
                        onDelete = {
                            scope.launch {
                                ZeroTierRepository.deleteNetwork(host.networkId)
                                FirebaseRepository.deleteHost(host.id)
                                loadHosts { hosts = it }
                            }
                        },
                        isOwn = host.ownerUid == FirebaseRepository.currentUid
                    )
                }
            }
        }
    }

    // Create host dialog
    if (showCreateDialog) {
        CreateHostDialog(
            hazeState = hazeState,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, version ->
                scope.launch {
                    showCreateDialog = false
                    isLoading = true
                    errorMsg = ""
                    try {
                        val networkId = ZeroTierRepository.createNetwork(name)
                        if (networkId != null) {
                            FirebaseRepository.createHost(name, version, networkId)
                            successMsg = "✅ Хост создан! Network ID: $networkId"
                            loadHosts { hosts = it }
                        } else {
                            errorMsg = "Ошибка создания сети ZeroTier"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Ошибка: ${e.message}"
                    }
                    isLoading = false
                }
            }
        )
    }

    // Join dialog
    showJoinDialog?.let { host ->
        JoinHostDialog(
            host = host,
            hazeState = hazeState,
            onDismiss = { showJoinDialog = null },
            onJoin = {
                scope.launch {
                    showJoinDialog = null
                    successMsg = "Подключение к сети ${host.networkId}...\nОткрой ZeroTier и введи Network ID, затем зайди в Minecraft → Играть → Серверы → Добавить сервер"
                }
            }
        )
    }
}

@Composable
fun HostCard(
    host: HostInfo,
    hazeState: HazeState,
    onJoin: () -> Unit,
    onDelete: () -> Unit,
    isOwn: Boolean
) {
    GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(0.dp, Color.Transparent, RoundedCornerShape(4.dp))
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 1f, targetValue = 0.3f,
                                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .then(Modifier.clip(RoundedCornerShape(4.dp)))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .then(
                                    if (host.online)
                                        Modifier.border(0.dp, Color(0xFF22C55E), RoundedCornerShape(5.dp))
                                    else
                                        Modifier.border(0.dp, Color(0xFF6B7280), RoundedCornerShape(5.dp))
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            host.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isOwn) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Мой", color = Color(0xFF8B5CF6), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoChip("🎮 Bedrock ${host.version}")
                        InfoChip("👥 ${host.playerCount}/${host.maxPlayers}")
                        InfoChip("@${host.ownerUsername}")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    onClick = onJoin,
                    hazeState = hazeState,
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Подключиться", color = Color.White, fontSize = 13.sp)
                }
                if (isOwn) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                    ) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
fun CreateHostDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var hostName by remember { mutableStateOf("") }
    var selectedVersion by remember { mutableStateOf("1.21.4") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1428),
        title = {
            Text("🎮 Создать хост", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassTextField(
                    value = hostName,
                    onValueChange = { hostName = it },
                    placeholder = "Название сервера...",
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth()
                )

                // Version picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    GlassButton(
                        onClick = { expanded = true },
                        hazeState = hazeState,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Версия: $selectedVersion", color = Color.White)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        MINECRAFT_VERSIONS.forEach { version ->
                            DropdownMenuItem(
                                text = { Text("Bedrock $version", color = Color.White) },
                                onClick = { selectedVersion = version; expanded = false },
                                modifier = Modifier.background(
                                    if (version == selectedVersion) Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                    else Color(0xFF1A1428)
                                )
                            )
                        }
                    }
                }

                GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text("ℹ️ Как это работает", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("1. Создастся виртуальная сеть ZeroTier", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("2. Игроки подключатся через ZeroTier", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("3. Реальный IP скрыт — все видят только виртуальный адрес", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("4. Запусти Minecraft и создай мир с LAN", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    if (hostName.isNotBlank()) onCreate(hostName, selectedVersion)
                },
                hazeState = hazeState,
                modifier = Modifier.height(44.dp)
            ) {
                Text("Создать", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun JoinHostDialog(
    host: HostInfo,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1428),
        title = {
            Text("Подключиться к ${host.name}", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("🎮 Версия", "Bedrock ${host.version}")
                        InfoRow("👤 Хост", "@${host.ownerUsername}")
                        InfoRow("👥 Игроков", "${host.playerCount}/${host.maxPlayers}")
                        InfoRow("🔗 Network ID", host.networkId)
                    }
                }

                GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text("📋 Инструкция", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("1. Установи приложение ZeroTier из Play Market", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("2. Введи Network ID в ZeroTier:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(host.networkId, color = Color(0xFF8B5CF6),
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("3. Открой Minecraft → Играть → Друзья", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text("4. Сервер хоста появится автоматически!", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = onJoin,
                hazeState = hazeState,
                modifier = Modifier.height(44.dp)
            ) {
                Text("Понятно!", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

suspend fun loadHosts(onResult: (List<HostInfo>) -> Unit) {
    val hosts = FirebaseRepository.getHosts()
    onResult(hosts)
}
