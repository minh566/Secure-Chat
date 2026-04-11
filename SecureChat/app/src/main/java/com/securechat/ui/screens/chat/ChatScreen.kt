package com.securechat.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Message
import com.securechat.domain.model.MessageType
import com.securechat.domain.repository.AuthRepository
import java.text.SimpleDateFormat
import java.io.File
import android.net.Uri
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    roomName: String,
    onBack: () -> Unit,
    onStartVideoCall: (calleeId: String, isGroupCall: Boolean) -> Unit,
    onOpenImageViewer: (imageSources: List<String>, startIndex: Int) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    authRepository: AuthRepository
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var reactingMessage by remember { mutableStateOf<Message?>(null) }
    var previewingMessage by remember { mutableStateOf<Message?>(null) }
    var poppingEmoji by remember { mutableStateOf<String?>(null) }
    val currentUserId = authRepository.currentUser?.uid ?: ""
    val displayRoomName = uiState.chatRoom?.displayNameFor(currentUserId) ?: roomName
    val activeCount = ((uiState.chatRoom?.members?.size ?: 2) - 1).coerceAtLeast(1)
    val isBusy = uiState.isSending || uiState.isUploadingAttachment

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.sendAttachment(uri, MessageType.FILE)
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.sendAttachment(uri, MessageType.IMAGE)
    }

    LaunchedEffect(uiState.messages.size, uiState.isUploadingAttachment) {
        val shouldStickToBottom = listState.shouldAutoScrollToBottom()
        if (!shouldStickToBottom) return@LaunchedEffect
        val listSize = uiState.messages.size + if (uiState.isUploadingAttachment) 1 else 0
        if (listSize > 0) listState.animateScrollToItem(listSize - 1)
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeErrorMessage()
    }

    LaunchedEffect(uiState.resendTargetType) {
        when (uiState.resendTargetType) {
            MessageType.IMAGE -> imagePickerLauncher.launch("image/*")
            MessageType.FILE -> filePickerLauncher.launch("*/*")
            else -> Unit
        }
        if (uiState.resendTargetType != null) {
            viewModel.consumeResendRequest()
        }
    }

    LaunchedEffect(uiState.pendingOpenAttachment?.id) {
        val pending = uiState.pendingOpenAttachment ?: return@LaunchedEffect
        if (pending.type == MessageType.IMAGE) {
            val images = uiState.messages
                .filter { it.type == MessageType.IMAGE }
                .mapNotNull { it.previewSource() }
            val startIndex = uiState.messages
                .filter { it.type == MessageType.IMAGE }
                .indexOfFirst { it.id == pending.id }
                .coerceAtLeast(0)
            if (images.isNotEmpty()) {
                onOpenImageViewer(images, startIndex)
            }
        } else {
            previewingMessage = pending
        }
        viewModel.consumePendingOpenAttachment()
    }

    Scaffold(
        containerColor = Color(0xFFF5F6FF),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = Color(0xFFF5F6FF)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp)
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
                                tint = Color(0xFF5360A9)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = Color(0xFFE5E9FF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayRoomName.take(2).uppercase(),
                                    color = Color(0xFF3C57C6),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayRoomName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1E2856),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$activeCount thành viên đang hoạt động",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A93BE)
                            )
                        }
                        IconButton(onClick = viewModel::showAddMembersDialog) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Thêm thành viên",
                                tint = Color(0xFF5360A9)
                            )
                        }
                        IconButton(onClick = {
                            val calleeId = viewModel.getCalleeId() ?: return@IconButton
                            onStartVideoCall(calleeId, uiState.chatRoom?.isGroup == true)
                        }) {
                            Icon(
                                Icons.Default.VideoCall,
                                contentDescription = "Gọi video",
                                tint = Color(0xFF5360A9)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFFF5F6FF),
                shadowElevation = 0.dp
            ) {
                Column {
                    AnimatedVisibility(visible = uiState.isUploadingAttachment) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Dang tai tep len... ${uiState.uploadProgressPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5360A9)
                            )
                            LinearProgressIndicator(
                                progress = { uiState.uploadProgressPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = Color(0xFFDFE4FD)
                    ) {
                        IconButton(onClick = { showAttachmentDialog = true }, enabled = !isBusy) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color(0xFF5868BB))
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    TextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputChange,
                        placeholder = { Text("Type a message", color = Color(0xFF99A1C9)) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp)),
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFE7EAFD),
                            unfocusedContainerColor = Color(0xFFE7EAFD),
                            focusedTextColor = Color(0xFF222A56),
                            unfocusedTextColor = Color(0xFF222A56)
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = {
                        filePickerLauncher.launch("*/*")
                    }, enabled = !isBusy) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Đính kèm", tint = Color(0xFF7C86B8))
                    }
                    IconButton(onClick = {
                        imagePickerLauncher.launch("image/*")
                    }, enabled = !isBusy) {
                        Icon(Icons.Default.Image, contentDescription = "Ảnh", tint = Color(0xFF7C86B8))
                    }

                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = Color(0xFF4E7EFF)
                    ) {
                        IconButton(onClick = {
                        if (uiState.inputText.isNotBlank()) viewModel.sendMessage() else viewModel.sendLike()
                    }) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.inputText.isBlank()) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Thích", tint = Color.White)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                        }
                    }
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
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
                        currentUserId = currentUserId,
                        onLongPress = { reactingMessage = message },
                        onOpenAttachment = viewModel::openAttachment,
                        isAttachmentMissing = message.id in uiState.brokenAttachmentMessageIds,
                        showResendAction = message.senderId == currentUserId && message.id in uiState.brokenAttachmentMessageIds,
                        onResendAttachment = { viewModel.requestResendAttachment(it.type) }
                    )
                }

                if (uiState.isUploadingAttachment) {
                    item(key = "uploading_indicator") {
                        UploadingAttachmentBubble(uiState.uploadProgressPercent)
                    }
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

        if (showAttachmentDialog) {
            AlertDialog(
                onDismissRequest = { showAttachmentDialog = false },
                title = { Text("Gửi tệp") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showAttachmentDialog = false
                                imagePickerLauncher.launch("image/*")
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chọn ảnh")
                        }
                        TextButton(
                            onClick = {
                                showAttachmentDialog = false
                                filePickerLauncher.launch("*/*")
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chọn tệp")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachmentDialog = false }) {
                        Text("Đóng")
                    }
                }
            )
        }

        reactingMessage?.let { target ->
            AlertDialog(
                onDismissRequest = { reactingMessage = null },
                title = { Text("Thả cảm xúc") },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChatViewModel.SUPPORTED_REACTIONS.forEach { emoji ->
                            val popScale by animateFloatAsState(
                                targetValue = if (poppingEmoji == emoji) 1.22f else 1f,
                                animationSpec = spring(dampingRatio = 0.38f, stiffness = 520f),
                                label = "reaction_pop"
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (target.reactions[currentUserId] == emoji) Color(0xFFD7E1FF) else Color(0xFFEAEFFD),
                                modifier = Modifier
                                    .scale(popScale)
                                    .clip(CircleShape)
                                    .clickable {
                                        poppingEmoji = emoji
                                        coroutineScope.launch {
                                            delay(120)
                                            viewModel.toggleReaction(target, emoji)
                                            reactingMessage = null
                                            poppingEmoji = null
                                        }
                                    }
                            ) {
                                Text(
                                    text = emoji,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { reactingMessage = null }) {
                        Text("Đóng")
                    }
                }
            )
        }

        previewingMessage?.let { message ->
            AttachmentViewerDialog(
                message = message,
                onDismiss = { previewingMessage = null }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListState.shouldAutoScrollToBottom(): Boolean {
    val info = layoutInfo
    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return true
    return lastVisible >= (info.totalItemsCount - 3)
}

private fun Message.previewSource(): String? {
    val local = localCachePath
    return if (!local.isNullOrBlank() && File(local).exists()) {
        Uri.fromFile(File(local)).toString()
    } else {
        fileUrl
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
    val todayFormat = remember { SimpleDateFormat("MMM dd", Locale.ENGLISH) }
    val today = remember { Calendar.getInstance() }
    val msgDay = remember(date) { Calendar.getInstance().apply { time = date } }

    val label = when {
        today.get(Calendar.YEAR) == msgDay.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == msgDay.get(Calendar.DAY_OF_YEAR) -> "TODAY, ${todayFormat.format(date).uppercase()}"
        else -> dateFormat.format(date)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8EBFC)
        ) {
        Text(
            text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7B84AE),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontWeight = FontWeight.SemiBold
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    currentUserId: String,
    onLongPress: () -> Unit,
    onOpenAttachment: (Message) -> Unit,
    isAttachmentMissing: Boolean,
    showResendAction: Boolean,
    onResendAttachment: (Message) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val bubbleColor = if (isMine) Color(0xFF4E7EFF) else Color(0xFFDCE1FB)
    val textColor = if (isMine) Color.White else Color(0xFF202A58)
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
                        .combinedClickable(
                            onClick = {
                                if (message.type == MessageType.IMAGE || message.type == MessageType.FILE) {
                                    onOpenAttachment(message)
                                }
                            },
                            onLongClick = onLongPress
                        )
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 6.dp
                            )
                        )
                        .background(bubbleColor)
                        .padding(horizontal = 13.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp)
                ) {
                    MessageBodyContent(message = message, textColor = textColor, onOpenAttachment = onOpenAttachment)
                }
                ReactionSummaryRow(message = message)
                if (isAttachmentMissing) {
                    Text(
                        text = "Tep da bi xoa tren server",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B93BD)
                    )
                }
                if (showResendAction && (message.type == MessageType.FILE || message.type == MessageType.IMAGE)) {
                    TextButton(onClick = { onResendAttachment(message) }) {
                        Text("Gui lai tep", color = Color(0xFF3E5FC9))
                    }
                }
                Row(modifier = Modifier.padding(top = 3.dp, end = 4.dp)) {
                    Text(
                        text = timeFormat.format(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B93BD)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hasSeen) "✓✓" else if (hasDelivered) "✓" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasSeen) Color(0xFF4E7EFF) else Color(0xFF97A0B2)
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = Color(0xFF313C6B)
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
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6D75A2),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                    )
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    if (message.type == MessageType.IMAGE || message.type == MessageType.FILE) {
                                        onOpenAttachment(message)
                                    }
                                },
                                onLongClick = onLongPress
                            )
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 6.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(bubbleColor)
                            .padding(horizontal = 13.dp, vertical = 10.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        MessageBodyContent(message = message, textColor = textColor, onOpenAttachment = onOpenAttachment)
                    }
                    ReactionSummaryRow(message = message)
                    if (isAttachmentMissing) {
                        Text(
                            text = "Tep da bi xoa tren server",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B93BD),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                    Text(
                        text = timeFormat.format(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B93BD),
                        modifier = Modifier.padding(top = 3.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBodyContent(message: Message, textColor: Color, onOpenAttachment: (Message) -> Unit) {
    when (message.type) {
        MessageType.IMAGE -> {
            SubcomposeAsyncImage(
                model = message.previewSource(),
                contentDescription = message.fileName ?: "Ảnh",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenAttachment(message) },
                loading = {
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFFA7AEC9), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Anh khong ton tai hoac da bi xoa",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2D355F)
                        )
                    }
                }
            )
            if (!message.fileName.isNullOrBlank()) {
                Text(
                    text = message.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        MessageType.FILE -> {
            Row(
                modifier = Modifier.clickable { onOpenAttachment(message) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "File",
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = message.fileName ?: message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Nhấn để mở tệp",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }

        else -> Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReactionSummaryRow(message: Message) {
    val grouped = message.reactions.values.groupingBy { it }.eachCount()
    if (grouped.isEmpty()) return

    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grouped.forEach { (emoji, count) ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEAEFFD)
            ) {
                Text(
                    text = "$emoji $count",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF41539F),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun UploadingAttachmentBubble(progressPercent: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFDDE5FF)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Dang gui tep... $progressPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF3E4F95)
                )
            }
        }
    }
}

