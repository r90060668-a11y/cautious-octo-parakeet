package com.kiro.arcade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.arcade.ui.theme.KiroArcadeTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KiroArcadeTheme {
                KiroArcadeApp()
            }
        }
    }
}

data class TabInfo(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabInfo("Посты", Icons.Filled.Article),
    TabInfo("Чаты", Icons.Filled.ChatBubble),
    TabInfo("Профиль", Icons.Filled.Person),
    TabInfo("Хосты", Icons.Filled.Groups)
)

@Composable
fun KiroArcadeApp() {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val hazeState = rememberHazeState()

    Box(modifier = Modifier.fillMaxSize()) {

        // Яркий подвижный фон — именно он "просвечивает" сквозь стеклянную панель
        LiquidBackground(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            TabContentScreen(tab = tabs[page])
        }

        GlassBottomBar(
            pagerState = pagerState,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp, vertical = 22.dp)
        )
    }
}

@Composable
private fun LiquidBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xFF120E1E), Color(0xFF05040A))
            )
        )
    ) {
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = 40.dp)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.55f), CircleShape)
                .blur(120.dp)
        )
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = 140.dp)
                .background(Color(0xFFEC4899).copy(alpha = 0.45f), CircleShape)
                .blur(130.dp)
        )
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 60.dp)
                .background(Color(0xFF22D3EE).copy(alpha = 0.35f), CircleShape)
                .blur(140.dp)
        )
    }
}

@Composable
private fun TabContentScreen(tab: TabInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tab.label,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "На данный момент происходит тест, приложение не работает",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun GlassBottomBar(
    pagerState: PagerState,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val tabCount = tabs.size
    val tabWidthPx = if (tabCount > 0) barWidthPx / tabCount else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val selectedIndex = pagerState.currentPage

    val indicatorXPx by animateFloatAsState(
        targetValue = selectedIndex * tabWidthPx + (if (isDragging) dragOffsetPx else 0f),
        animationSpec = if (isDragging) snap() else spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "indicatorOffset"
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
        // Ползунок ("thumb"), который нужно тянуть, чтобы переключить вкладку
        if (tabWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = indicatorXPx.roundToInt(), y = 0) }
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { tabWidthPx.toDp() })
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(30.dp))
                    .pointerInput(tabWidthPx, tabCount, selectedIndex) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffsetPx = 0f
                            },
                            onDragEnd = {
                                val rawPos = selectedIndex * tabWidthPx + dragOffsetPx
                                val newIndex = (rawPos / tabWidthPx)
                                    .roundToInt()
                                    .coerceIn(0, tabCount - 1)
                                isDragging = false
                                dragOffsetPx = 0f
                                scope.launch { pagerState.animateScrollToPage(newIndex) }
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetPx = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val minOffset = -selectedIndex * tabWidthPx
                                val maxOffset = (tabCount - 1 - selectedIndex) * tabWidthPx
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(minOffset, maxOffset)
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
                        ) {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
