package com.example.securechat.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*

// ── Màu sắc chủ đạo (Messenger/Zalo inspired) ──────────────────────────────
val PrimaryBlue   = Color(0xFF0084FF)
val PrimaryIndigo = Color(0xFF7B68EE)
val SurfaceLight  = Color(0xFFF0F2F5)
val BubbleSelf    = Color(0xFF0084FF)
val BubbleOther   = Color(0xFFE4E6EB)
val TextPrimary   = Color(0xFF050505)
val TextSecondary = Color(0xFF65676B)
val OnlineGreen   = Color(0xFF31A24C)

// ── LoginScreen ──────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var phone    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    val focusMgr = LocalFocusManager.current

    // Animated logo pulse
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1877F2), Color(0xFF0A4FA0), Color(0xFF05306B))
                )
            )
    ) {
        // Decorative circles background
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.04f), radius = 320f,
                center = Offset(size.width * 0.85f, size.height * 0.12f))
            drawCircle(Color.White.copy(alpha = 0.03f), radius = 220f,
                center = Offset(size.width * 0.1f,  size.height * 0.35f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = 180f,
                center = Offset(size.width * 0.7f,  size.height * 0.75f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(pulse)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Chat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "ConnectNow",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Nhắn tin & Gọi video nhóm",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(44.dp))

            // Card form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                    Text(
                        "Đăng nhập",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Chào mừng bạn trở lại!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Phone field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Phone, null,
                                tint = if (phone.isNotEmpty()) PrimaryBlue else TextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusMgr.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            focusedLabelColor  = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFDDE1E7)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(14.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, null,
                                tint = if (password.isNotEmpty()) PrimaryBlue else TextSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(
                                    if (passVisible) Icons.Outlined.VisibilityOff
                                    else Icons.Outlined.Visibility,
                                    null, tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusMgr.clearFocus() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            focusedLabelColor  = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFDDE1E7)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quên mật khẩu
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = {}) {
                            Text("Quên mật khẩu?", color = PrimaryBlue, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Login button
                    Button(
                        onClick = {
                            isLoading = true
                            // TODO: gọi ViewModel.login()
                            onLoginSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        enabled = phone.isNotEmpty() && password.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text("Đăng nhập", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Divider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(Modifier.weight(1f), color = Color(0xFFE4E6EB))
                        Text("  hoặc  ", color = TextSecondary, fontSize = 12.sp)
                        Divider(Modifier.weight(1f), color = Color(0xFFE4E6EB))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Google sign in
                    OutlinedButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDE1E7))
                    ) {
                        Icon(Icons.Filled.AccountCircle, null,
                            tint = Color(0xFFDB4437), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Tiếp tục với Google", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Register link
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản?", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Đăng ký ngay", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}