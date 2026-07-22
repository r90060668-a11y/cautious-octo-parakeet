package com.kiro.arcade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.kiro.arcade.ui.components.AnimatedLiquidBackground
import com.kiro.arcade.ui.screens.*
import com.kiro.arcade.ui.theme.KiroArcadeTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KiroArcadeTheme {
                KiroArcadeRoot()
            }
        }
    }
}

@Composable
fun KiroArcadeRoot() {
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    if (!isLoggedIn) {
        AuthScreen(onAuthSuccess = { isLoggedIn = true })
    } else {
        MainApp(onLogout = { isLoggedIn = false })
    }
}

data class TabInfo(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabInfo("Посты", Icons.Filled.Article),
    TabInfo("Чаты", Icons.Filled.ChatBubble),
    TabInfo("Профиль", Icons.Filled.Person),
    TabInfo("Хосты", Icons.Filled.Groups)
)

@Composable
fun MainApp(onLogout: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val hazeState = rememberHazeState()
    var profileUid by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedLiquidBackground(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        )

        if (profileUid != null) {
            // Navigate to another user's profile
            ProfileScreen(
                uid = profileUid,
                hazeState = hazeState,
                onBack = { profileUid = null }
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> PostsScreen(
                        hazeState = hazeState,
                        onNavigateToProfile = { uid -> profileUid = uid }
                    )
                    1 -> ChatsScreen(hazeState = hazeState)
                    2 -> ProfileScreen(
                        uid = null,
                        hazeState = hazeState,
                        onLogout = onLogout
                    )
                    3 -> HostsScreen(hazeState = hazeState)
                }
            }

            GlassTabBar(
                pagerState = pagerState,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            )
        }
    }
}

@Composable
fun HostsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Хосты\nскоро появятся",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GlassTabBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val tabCount = tabs.size
    val tabWidthPx = if (tabCount > 0) barWidthPx / tabCount else 0f
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val selectedIndex = pagerState.currentPage
    val density = LocalDensity.current

    val indicatorXPx by animateFloatAsState(
        targetValue = selectedIndex * tabWidthPx + (if (isDragging) dragOffsetPx else 0f),
        animationSpec = if (isDragging) snap() else spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "indicator"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(38.dp))
            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(38.dp))
            .onSizeChanged { barWidthPx = it.width.toFloat() }
    ) {
        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = indicatorXPx.roundToInt(), y = 0) }
                    .width(tabWidthDp)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(30.dp))
                    .pointerInput(tabWidthPx, tabCount, selectedIndex) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true; dragOffsetPx = 0f },
                            onDragEnd = {
                                val rawPos = selectedIndex * tabWidthPx + dragOffsetPx
                                val newIndex = (rawPos / tabWidthPx).roundToInt().coerceIn(0, tabCount - 1)
                                isDragging = false; dragOffsetPx = 0f
                                scope.launch { pagerState.animateScrollToPage(newIndex) }
                            },
                            onDragCancel = { isDragging = false; dragOffsetPx = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val minOff = -selectedIndex * tabWidthPx
                                val maxOff = (tabCount - 1 - selectedIndex) * tabWidthPx
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(minOff, maxOff)
                            }
                        )
                    }
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { scope.launch { pagerState.animateScrollToPage(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
