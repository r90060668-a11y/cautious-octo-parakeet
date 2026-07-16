package com.kiro.arcade.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun AnimatedLiquidBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "t3"
    )

    val x1 = (sin(t1.toDouble()) * 80).dp
    val y1 = (cos(t1.toDouble() * 0.7) * 100).dp
    val x2 = (cos(t2.toDouble()) * 100).dp
    val y2 = (sin(t2.toDouble() * 0.8) * 80).dp
    val x3 = (sin(t3.toDouble() * 0.6) * 60).dp
    val y3 = (cos(t3.toDouble()) * 120).dp

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF120E1E), Color(0xFF05040A)))
        )
    ) {
        // Violet blob
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-40).dp + x1, y = 20.dp + y1)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.55f), CircleShape)
                .blur(130.dp)
        )
        // Pink blob
        Box(
            Modifier
                .size(340.dp)
                .offset(x = 60.dp + x2, y = 140.dp + y2)
                .background(Color(0xFFEC4899).copy(alpha = 0.45f), CircleShape)
                .blur(140.dp)
        )
        // Cyan blob
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (-20).dp + x3, y = 500.dp + y3)
                .background(Color(0xFF22D3EE).copy(alpha = 0.35f), CircleShape)
                .blur(120.dp)
        )
        // Blue accent
        Box(
            Modifier
                .size(200.dp)
                .offset(x = 140.dp + x2 * 0.5f, y = 350.dp + y1 * 0.5f)
                .background(Color(0xFF3B82F6).copy(alpha = 0.3f), CircleShape)
                .blur(100.dp)
        )
    }
}
