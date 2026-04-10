
package com.securechat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.securechat.domain.model.ChatRoom
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (roomId: String, roomName: String) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = viewModel.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureChat") },
                actions = {
                    IconButton(onClick = { viewModel.signOut(onSignedOut) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "Kết bạn")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                            "Nhấn + để tạo phòng mới",
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
                        val myUid = user?.uid ?: "me"
                        val displayRoomName = room.displayNameFor(user?.uid.orEmpty())
                        
                        RoomItem(
                            room = room,
                            roomDisplayName = displayRoomName,
                            unreadForMe = room.unreadCount[myUid] ?: 0,
                            onClick = { onOpenChat(room.id, displayRoomName) },
                            onLongClick = { viewModel.showDeleteRoomDialog(room.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }

        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissCreateDialog,
                title   = { Text("Gửi lời mời kết bạn") },
                text    = {
                    Column {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            label = { Text("Tên hoặc email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.searchQuery.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            if (uiState.searchResults.isEmpty() && uiState.selectedUser == null) {
                                Text(
                                    text = "Không tìm thấy người dùng phù hợp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        uiState.searchResults.take(5).forEach { item ->
                                            ListItem(
                                                modifier = Modifier.clickable { viewModel.onUserSelected(item) },
                                                headlineContent = { Text(item.displayName) },
                                                supportingContent = { Text(item.email) },
                                                leadingContent = {
                                                    Icon(Icons.Default.Person, contentDescription = null)
                                                }
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                        uiState.selectedUser?.let {
                            Text(
                                text = "Đã chọn: ${it.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
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
                        else Text("Gửi") 
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissCreateDialog) { Text("Hủy") }
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RoomItem(
    room: ChatRoom,
    roomDisplayName: String,
    unreadForMe: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        headlineContent = {
            Text(
                text = roomDisplayName, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = if (unreadForMe > 0) androidx.compose.ui.text.font.FontWeight.Bold else null
            )
        },
        supportingContent = {
            Text(
                text = room.lastMessage?.content ?: "Chưa có tin nhắn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                if (unreadForMe > 0) {
                    Spacer(Modifier.height(4.dp))
                    Badge { Text(unreadForMe.toString()) }
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

