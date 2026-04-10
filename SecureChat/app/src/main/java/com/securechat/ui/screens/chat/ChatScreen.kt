<<<<<<< HEAD

package com.securechat.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Message
import com.securechat.domain.repository.AuthRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomName: String,
    onBack: () -> Unit,
    onStartVideoCall: (calleeId: String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    authRepository: AuthRepository
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val currentUserId = authRepository.currentUser?.uid ?: ""
    val displayRoomName = uiState.chatRoom?.displayNameFor(currentUserId) ?: roomName

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(displayRoomName, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = viewModel::showAddMembersDialog) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Thêm thành viên")
                    }
                    Spacer(Modifier.width(6.dp))
                    FilledTonalIconButton(onClick = {
                        viewModel.getCalleeId()?.let(onStartVideoCall)
                    }) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Gọi video")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputChange,
                        placeholder = { Text("Nhắn tin...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = viewModel::sendMessage,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(
                    items = uiState.messages,
                    key = { _, item -> item.id }
                ) { index, message ->
                    val previousMessage = uiState.messages.getOrNull(index - 1)
                    val shouldShowDate = previousMessage == null || !isSameDay(previousMessage.createdAt, message.createdAt)

                    if (shouldShowDate) {
                        DateSeparator(date = message.createdAt)
                    }

                    MessageBubble(
                        message = message,
                        isMine = message.senderId == currentUserId,
                        currentUserId = currentUserId
                    )
                }
            }
        }

        if (uiState.showAddMembersDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissAddMembersDialog,
                title = { Text("Thêm thành viên") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.memberSearchQuery,
                            onValueChange = viewModel::onMemberSearchQueryChange,
                            label = { Text("Tìm theo tên") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (uiState.memberSearchQuery.isNotBlank() && uiState.memberSearchResults.isEmpty()) {
                            Text(
                                text = "Không có người dùng phù hợp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.memberSearchResults,
                                key = { _, item -> item.uid }
                            ) { _, user ->
                                val isSelected = user.uid in uiState.selectedMemberIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.toggleMemberSelection(user.uid) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleMemberSelection(user.uid) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.displayName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            user.email,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::addSelectedMembers,
                        enabled = uiState.selectedMemberIds.isNotEmpty() && !uiState.isAddingMembers
                    ) {
                        if (uiState.isAddingMembers) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Thêm")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissAddMembersDialog) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

private fun ChatRoom.displayNameFor(currentUserId: String): String {
    if (isGroup) return name
    val otherMemberId = members.firstOrNull { it != currentUserId }
    val otherName = otherMemberId
        ?.let { memberNames[it] }
        ?.takeIf { it.isNotBlank() }
    return otherName ?: name
}

@Composable
private fun DateSeparator(date: Date) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = dateFormat.format(date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private fun isSameDay(first: Date, second: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = first }
    val cal2 = Calendar.getInstance().apply { time = second }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean, currentUserId: String) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val bubbleColor = if (isMine)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isMine) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMine) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 6.dp,
                        bottomEnd = if (isMine) 6.dp else 16.dp
                    )
                )
                .background(color = bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFormat.format(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        if (isMine) {
            val hasSeen = message.seenBy.any { it != currentUserId }
            val hasDelivered = message.deliveredTo.any { it != currentUserId }
            val statusText = when {
                hasSeen -> "Đã xem"
                hasDelivered -> "Đã nhận"
                else -> "Đã gửi"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 4.dp, top = 3.dp)
            )
        }
    }
}

=======
package com.securechat.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Message
import com.securechat.domain.repository.AuthRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomName: String,
    onBack: () -> Unit,
    onStartVideoCall: (calleeId: String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    authRepository: AuthRepository
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val currentUserId = authRepository.currentUser?.uid ?: ""
    val displayRoomName = uiState.chatRoom?.displayNameFor(currentUserId) ?: roomName
    val activeCount = ((uiState.chatRoom?.members?.size ?: 2) - 1).coerceAtLeast(1)

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF1F2F7),
        topBar = {
            Surface(color = Color.White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color(0xFF0F80F8)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFFE7F0FF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayRoomName.take(2).uppercase(),
                                    color = Color(0xFF2388F9),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayRoomName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$activeCount thành viên đang hoạt động",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5C6475)
                            )
                        }
                        IconButton(onClick = viewModel::showAddMembersDialog) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Thêm thành viên",
                                tint = Color(0xFF0F80F8)
                            )
                        }
                        IconButton(onClick = { viewModel.getCalleeId()?.let(onStartVideoCall) }) {
                            Icon(
                                Icons.Default.VideoCall,
                                contentDescription = "Gọi video",
                                tint = Color(0xFF0F80F8)
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Gọi thoại",
                                tint = Color(0xFF0F80F8)
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Tuỳ chọn",
                                tint = Color(0xFF0F80F8)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE9ECF3))
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Đính kèm", tint = Color(0xFF0F80F8))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Image, contentDescription = "Ảnh", tint = Color(0xFF0F80F8))
                    }

                    TextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputChange,
                        placeholder = { Text("Aa") },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF1F2F6),
                            unfocusedContainerColor = Color(0xFFF1F2F6)
                        )
                    )

                    IconButton(onClick = {
                        if (uiState.inputText.isNotBlank()) viewModel.sendMessage()
                    }) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF0F80F8),
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.inputText.isBlank()) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Thích", tint = Color(0xFFF0B90B))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color(0xFF0F80F8))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(
                    items = uiState.messages,
                    key = { _, item -> item.id }
                ) { index, message ->
                    val previousMessage = uiState.messages.getOrNull(index - 1)
                    val shouldShowDate = previousMessage == null ||
                        !isSameDay(previousMessage.createdAt, message.createdAt)

                    if (shouldShowDate) {
                        DateSeparator(date = message.createdAt)
                    }

                    MessageBubble(
                        message = message,
                        isMine = message.senderId == currentUserId,
                        currentUserId = currentUserId
                    )
                }
            }
        }

        if (uiState.showAddMembersDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissAddMembersDialog,
                title = { Text("Thêm thành viên") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.memberSearchQuery,
                            onValueChange = viewModel::onMemberSearchQueryChange,
                            label = { Text("Tìm theo tên") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (uiState.memberSearchQuery.isNotBlank() && uiState.memberSearchResults.isEmpty()) {
                            Text(
                                text = "Không có người dùng phù hợp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.memberSearchResults,
                                key = { _, item -> item.uid }
                            ) { _, user ->
                                val isSelected = user.uid in uiState.selectedMemberIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.toggleMemberSelection(user.uid) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleMemberSelection(user.uid) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.displayName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            user.email,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::addSelectedMembers,
                        enabled = uiState.selectedMemberIds.isNotEmpty() && !uiState.isAddingMembers
                    ) {
                        if (uiState.isAddingMembers) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Thêm")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissAddMembersDialog) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

private fun ChatRoom.displayNameFor(currentUserId: String): String {
    if (isGroup) return name
    val otherMemberId = members.firstOrNull { it != currentUserId }
    val otherName = otherMemberId
        ?.let { memberNames[it] }
        ?.takeIf { it.isNotBlank() }
    return otherName ?: name
}

@Composable
private fun DateSeparator(date: Date) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val today = remember { Calendar.getInstance() }
    val msgDay = remember(date) { Calendar.getInstance().apply { time = date } }

    val label = when {
        today.get(Calendar.YEAR) == msgDay.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == msgDay.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
        else -> dateFormat.format(date)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD9DEE8))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF707786),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD9DEE8))
    }
}

private fun isSameDay(first: Date, second: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = first }
    val cal2 = Calendar.getInstance().apply { time = second }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean, currentUserId: String) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val bubbleColor = if (isMine) Color(0xFF1B8CFF) else Color(0xFFE8EAF1)
    val textColor = if (isMine) Color.White else Color(0xFF202531)
    val hasSeen = message.seenBy.any { it != currentUserId }
    val hasDelivered = message.deliveredTo.any { it != currentUserId }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isMine) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .background(bubbleColor)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .widthIn(max = 300.dp)
                ) {
                    Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyLarge)
                }
                Row(modifier = Modifier.padding(top = 3.dp, end = 4.dp)) {
                    Text(
                        text = timeFormat.format(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6E7584)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hasSeen) "✓✓" else if (hasDelivered) "✓" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasSeen) Color(0xFF1B8CFF) else Color(0xFF97A0B2)
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFFDF4CA8)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = message.senderName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF9E3E8A),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 8.dp,
                                    bottomEnd = 18.dp
                                )
                            )
                            .background(Color(0xFFEDEFF3))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .widthIn(max = 300.dp)
                    ) {
                        Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        text = timeFormat.format(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6E7584),
                        modifier = Modifier.padding(top = 3.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}
>>>>>>> 22c3a84 (feat: redesign core screens and wire settings with biometric app lock)
