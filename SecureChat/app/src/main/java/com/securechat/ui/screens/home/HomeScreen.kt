package com.securechat.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.ChatRoom
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (roomId: String, roomName: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = viewModel.currentUser

    LaunchedEffect(uiState.infoMessage) {
        if (uiState.infoMessage != null) {
            kotlinx.coroutines.delay(1800)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureChat") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Cài đặt")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Ket ban")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            uiState.infoMessage?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            if (uiState.incomingRequests.isNotEmpty()) {
                Text(
                    text = "Loi moi ket ban",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                )

                uiState.incomingRequests.forEach { requestUser ->
                    ListItem(
                        headlineContent = { Text(requestUser.displayName) },
                        supportingContent = { Text(requestUser.email) },
                        leadingContent = {
                            AvatarWithStatus(
                                imageUrl = requestUser.photoUrl,
                                name = requestUser.displayName,
                                isOnline = requestUser.isOnline
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { viewModel.rejectFriendRequest(requestUser) }) {
                                    Text("Tu choi")
                                }
                                Button(onClick = { viewModel.acceptFriendRequest(requestUser) }) {
                                    Text("Dong y")
                                }
                            }
                        }
                    )
                }

                HorizontalDivider()
            }

            user?.let {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(it.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(it.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider()

            if (uiState.isLoading && uiState.rooms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.rooms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Chưa có cuộc trò chuyện nào",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Nhan + de ket ban va bat dau tro chuyen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(
                        items = uiState.rooms,
                        key   = { it.id }
                    ) { room ->
                        val displayRoomName = room.displayNameFor(user?.uid.orEmpty())
                        RoomItem(
                            room    = room,
                            roomDisplayName = displayRoomName,
                            currentUserId = user?.uid ?: "",
                            onClick = { onOpenChat(room.id, displayRoomName) },
                            onLongClick = { viewModel.showDeleteRoomDialog(room.id) } // Needs to be added to view model
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }

        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissCreateDialog,
                title   = { Text("Gui loi moi ket ban") },
                text    = {
                    Column {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            label = { Text("Tim kiem nguoi dung") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        uiState.selectedUser?.let { selected ->
                            Text(
                                text = "Da chon: ${selected.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        uiState.searchResults.take(6).forEach { user ->
                            ListItem(
                                headlineContent = { Text(user.displayName) },
                                supportingContent = { Text(user.email) },
                                leadingContent = {
                                    AvatarWithStatus(
                                        imageUrl = user.photoUrl,
                                        name = user.displayName,
                                        isOnline = user.isOnline
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(user.uid) {
                                        detectTapGestures(onTap = { viewModel.onUserSelected(user) })
                                    }
                            )
                        }

                        if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick  = viewModel::sendFriendRequest,
                        enabled  = uiState.selectedUser != null && !uiState.isCreating
                    ) {
                        if (uiState.isCreating) CircularProgressIndicator(Modifier.size(16.dp))
                        else Text("Gui loi moi")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissCreateDialog) { Text("Huy") }
                }
            )
        }

        if (uiState.roomToDeleteId != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteRoomDialog,
                title = { Text("Xóa phòng chat") },
                text = { Text("Bạn có chắc chắn muốn xóa phòng chat này không? Tất cả tin nhắn sẽ bị xóa vĩnh viễn.") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteRoom(uiState.roomToDeleteId!!) }
                    ) {
                        Text("Xóa", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteRoomDialog) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomItem(
    room: ChatRoom,
    roomDisplayName: String,
    currentUserId: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val unread = room.unreadCount[currentUserId] ?: 0
    val isUnread = unread > 0
    val isMine = room.lastMessage?.senderId == currentUserId

    ListItem(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick() }
            )
        },
        headlineContent = {
            Text(roomDisplayName, style = MaterialTheme.typography.titleMedium, fontWeight = if (isUnread) androidx.compose.ui.text.font.FontWeight.Bold else null)
        },
        supportingContent = {
            val prefix = if (isMine) "Bạn: " else ""
            Text(
                text = prefix + (room.lastMessage?.content ?: "Chưa có tin nhắn"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMine) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isUnread && !isMine) androidx.compose.ui.text.font.FontWeight.Bold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = roomDisplayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                room.lastMessage?.createdAt?.let {
                    Text(
                        timeFormat.format(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (unread > 0) {
                    Spacer(Modifier.height(4.dp))
                    Badge { Text(unread.toString()) }
                }
            }
        }
    )
}

private fun ChatRoom.displayNameFor(currentUserId: String): String {
    if (isGroup) return name
    val otherMemberId = members.firstOrNull { it != currentUserId }
    val otherName = otherMemberId
        ?.let { memberNames[it] }
        ?.takeIf { it.isNotBlank() }
    return otherName ?: name
}

val PrimaryGreen = Color(0xFF4CAF50)

@Composable
fun AvatarWithStatus(imageUrl: String?, name: String, isOnline: Boolean) {
    Box {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .background(PrimaryGreen, androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
