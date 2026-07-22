package com.kiro.arcade.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.arcade.data.repository.FirebaseRepository
import com.kiro.arcade.ui.components.AnimatedLiquidBackground
import com.kiro.arcade.ui.components.GlassButton
import com.kiro.arcade.ui.components.GlassCard
import com.kiro.arcade.ui.components.GlassTextField
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val hazeState = rememberHazeState()
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedLiquidBackground(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Kiro Arcade",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                if (isLogin) "Добро пожаловать!" else "Создай аккаунт",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 40.dp)
            )

            GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isLogin) {
                        GlassTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = "Никнейм",
                            hazeState = hazeState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        hazeState = hazeState,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Пароль",
                        hazeState = hazeState,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = Color(0xFFFF6B6B), fontSize = 13.sp)
                    }

                    GlassButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMsg = ""
                                val result = if (isLogin)
                                    FirebaseRepository.login(email, password)
                                else
                                    FirebaseRepository.register(email, password, username)
                                isLoading = false
                                result.fold(
                                    onSuccess = { onAuthSuccess() },
                                    onFailure = { errorMsg = it.message ?: "Ошибка" }
                                )
                            }
                        },
                        hazeState = hazeState,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else
                            Text(
                                if (isLogin) "Войти" else "Зарегистрироваться",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                    }
                }
            }

            TextButton(
                onClick = { isLogin = !isLogin; errorMsg = "" },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    if (isLogin) "Нет аккаунта? Зарегистрироваться"
                    else "Уже есть аккаунт? Войти",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
