package com.kiro.arcade.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.4f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF8B5CF6)
            ),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                disabledContainerColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
