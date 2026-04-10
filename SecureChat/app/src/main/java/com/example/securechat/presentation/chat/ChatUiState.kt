package com.example.securechat.presentation.chat

// Thêm vào cuối file GroupChatScreen.kt hoặc tạo file riêng
// UiState cho ChatViewModel
data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val typingUsers: Set<String> = emptySet(),
    val error: String? = null
)