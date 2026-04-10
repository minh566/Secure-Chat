package com.example.securechat.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.example.securechat.presentation.auth.*
import com.example.securechat.presentation.home.GroupAvatar
import com.example.securechat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Message model (UI) ────────────────────────────────────────────────────────
data class UiMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderInitial: String,
    val senderColor: Color,
    val content: String,
    val timestamp: Long,
    val isSentByMe: Boolean,
    val status: MsgStatus = MsgStatus.SENT,
    val reactions: List<String> = emptyList()
)

enum class MsgStatus { SENDING, SENT, DELIVERED, READ }

// ── ChatScreen ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupName: String,
    memberCount: Int,
    onBack: () -> Unit,
    onStartVideoCall: () -> Unit,
    onViewMembers: () -> Unit,
    viewModel: ChatViewModel // inject bình thường
) {
    val uiState    by viewModel.uiState.collectAsState()
    val listState   = rememberLazyListState()
    var showOptions by remember { mutableStateOf(false) }

    // Scroll xuống cuối khi có tin mới
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty())
            listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Scaffold(
        topBar = {
            GroupChatTopBar(
                groupName    = groupName,
                memberCount  = memberCount,
                onBack       = onBack,
                onVideoCall  = onStartVideoCall,
                onViewMembers = onViewMembers,
                showOptions  = showOptions,
                onToggleOptions = { showOptions = !showOptions }
            )
        },
        containerColor = Color(0xFFF0F2F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Danh sách tin nhắn
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Nhóm tin nhắn theo ngày
                uiState.messages.groupBy { getDateLabel(it.timestamp) }
                    .forEach { (dateLabel, msgs) ->
                        item(key = "date_$dateLabel") {
                            DateDivider(dateLabel)
                        }
                        items(msgs, key = { it.id }) { msg ->
                            MessageBubble(
                                message = msg,
                                showAvatar = shouldShowAvatar(msgs, msg)
                            )
                        }
                    }
            }

            // Typing indicator
            AnimatedVisibility(
                visible = uiState.typingUsers.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit  = fadeOut() + slideOutVertically()
            ) {
                TypingIndicator(uiState.typingUsers)
            }

            // Input bar
            ChatInputBar(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onAttachment = { /* TODO */ },
                onEmoji = { /* TODO */ }
            )
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatTopBar(
    groupName: String,
    memberCount: Int,
    onBack: () -> Unit,
    onVideoCall: () -> Unit,
    onViewMembers: () -> Unit,
    showOptions: Boolean,
    onToggleOptions: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIos, null,
                    tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }

            // Group info clickable
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewMembers),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupAvatar(
                    initial = groupName.take(2).uppercase(),
                    color = PrimaryBlue,
                    size = 40.dp
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        groupName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(OnlineGreen, CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$memberCount thành viên đang hoạt động",
                            fontSize = 11.sp, color = TextSecondary
                        )
                    }
                }
            }

            // Action buttons
            IconButton(onClick = onVideoCall) {
                Icon(Icons.Filled.VideoCall, null,
                    tint = PrimaryBlue, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onVideoCall) {
                Icon(Icons.Filled.Call, null,
                    tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onToggleOptions) {
                Icon(Icons.Filled.MoreVert, null, tint = PrimaryBlue)
            }
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────────
@Composable
fun MessageBubble(message: UiMessage, showAvatar: Boolean) {
    val bubbleColor   = if (message.isSentByMe) BubbleSelf else BubbleOther
    val textColor     = if (message.isSentByMe) Color.White else TextPrimary
    val arrangement   = if (message.isSentByMe) Arrangement.End else Arrangement.Start
    val bubbleShape   = if (message.isSentByMe)
        RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    else
        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start  = if (message.isSentByMe) 60.dp else 8.dp,
                end    = if (message.isSentByMe) 8.dp  else 60.dp,
                top    = 2.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar (chỉ cho tin nhắn của người khác)
        if (!message.isSentByMe) {
            if (showAvatar) {
                GroupAvatar(
                    initial = message.senderInitial,
                    color = message.senderColor,
                    size = 30.dp
                )
            } else {
                Spacer(Modifier.width(30.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (message.isSentByMe) Alignment.End else Alignment.Start) {
            // Tên người gửi (chỉ hiện khi showAvatar)
            if (!message.isSentByMe && showAvatar) {
                Text(
                    message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = message.senderColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // Bong bóng tin nhắn
            Box(
                modifier = Modifier
                    .background(bubbleColor, bubbleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    message.content,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }

            // Reactions
            if (message.reactions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(y = (-8).dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE4E6EB), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(message.reactions.joinToString(""), fontSize = 12.sp)
                }
            }

            // Thời gian + status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = if (message.reactions.isEmpty()) 2.dp else 0.dp)
            ) {
                Text(
                    formatTime(message.timestamp),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                if (message.isSentByMe) {
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        when (message.status) {
                            MsgStatus.SENDING   -> Icons.Outlined.Schedule
                            MsgStatus.SENT      -> Icons.Outlined.Check
                            MsgStatus.DELIVERED -> Icons.Filled.DoneAll
                            MsgStatus.READ      -> Icons.Filled.DoneAll
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (message.status == MsgStatus.READ) PrimaryBlue else TextSecondary
                    )
                }
            }
        }
    }
}

// ── Date divider ──────────────────────────────────────────────────────────────
@Composable
fun DateDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(Modifier.weight(1f), color = Color(0xFFCDD1DA))
        Text(
            "  $label  ",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Divider(Modifier.weight(1f), color = Color(0xFFCDD1DA))
    }
}

// ── Typing indicator (3 dots animation) ──────────────────────────────────────
@Composable
fun TypingIndicator(users: Set<String>) {
    val names = users.take(2).joinToString(", ") +
            if (users.size > 2) " và ${users.size - 2} người khác" else ""

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated dots
        val infiniteTransition = rememberInfiniteTransition(label = "typing")
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (0..2).forEach { i ->
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(600, delayMillis = i * 150),
                        RepeatMode.Reverse
                    ), label = "dot$i"
                )
                Box(
                    Modifier
                        .size(7.dp)
                        .alpha(alpha)
                        .background(TextSecondary, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$names đang nhập...",
            fontSize = 12.sp, color = TextSecondary
        )
    }
}

// ── Chat Input Bar ────────────────────────────────────────────────────────────
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachment: () -> Unit,
    onEmoji: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment
            IconButton(onClick = onAttachment, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.AttachFile, null,
                    tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }

            // Image
            IconButton(onClick = onAttachment, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Image, null,
                    tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }

            // Text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .background(SurfaceLight, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text("Aa", color = TextSecondary, fontSize = 15.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary, fontSize = 15.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(6.dp))

            // Send / Like button
            AnimatedContent(
                targetState = text.isNotBlank(),
                transitionSpec = {
                    scaleIn(tween(150)) togetherWith scaleOut(tween(150))
                },
                label = "send_btn"
            ) { hasText ->
                if (hasText) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryBlue, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Send, "Gửi",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp).offset(x = 1.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onTextChange("👍") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("👍", fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getDateLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "Hôm qua"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun shouldShowAvatar(messages: List<UiMessage>, current: UiMessage): Boolean {
    val idx = messages.indexOf(current)
    if (idx == messages.size - 1) return true
    val next = messages[idx + 1]
    return next.senderId != current.senderId
}