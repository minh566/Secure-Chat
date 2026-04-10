package com.securechat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.securechat.domain.model.ChatRoom
import java.util.Calendar
import java.util.Date
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
    val myUid = user?.uid.orEmpty()
    var roomSearchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RoomFilter.ALL) }

    val sortedRooms = remember(uiState.rooms) {
        uiState.rooms.sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }
    }
    val unreadRoomCount = remember(sortedRooms, myUid) {
        sortedRooms.count { (it.unreadCount[myUid] ?: 0) > 0 }
    }
    val groupRoomCount = remember(sortedRooms) { sortedRooms.count { it.isGroup } }
    val filteredRooms = remember(sortedRooms, roomSearchQuery, selectedFilter, myUid) {
        sortedRooms.filter { room ->
            val matchesFilter = when (selectedFilter) {
                RoomFilter.ALL -> true
                RoomFilter.UNREAD -> (room.unreadCount[myUid] ?: 0) > 0
                RoomFilter.GROUPS -> room.isGroup
            }
            val matchesQuery = roomSearchQuery.isBlank() || roomMatchesQuery(room, roomSearchQuery, myUid)
            matchesFilter && matchesQuery
        }
    }
    val activityRooms = remember(sortedRooms) { sortedRooms.take(6) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SecureChat", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = user?.displayName?.takeIf { it.isNotBlank() }?.let { "Xin chào, $it" }
                                ?: "Cuộc trò chuyện của bạn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                        AvatarWithStatus(
                            imageUrl = user?.photoUrl,
                            name = user?.displayName ?: "Me",
                            isOnline = user?.isOnline ?: false,
                            size = 44.dp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Kết bạn")
                    }
                    IconButton(onClick = { viewModel.signOut(onSignedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Đăng xuất")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "Kết bạn")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarWithStatus(
                                imageUrl = user?.photoUrl,
                                name = user?.displayName ?: "Me",
                                isOnline = user?.isOnline ?: false,
                                size = 52.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user?.displayName ?: "Người dùng",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = user?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = viewModel::showCreateDialog) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Kết bạn",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatMiniCard(
                                label = "Tất cả",
                                value = sortedRooms.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                label = "Chưa đọc",
                                value = unreadRoomCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                label = "Nhóm",
                                value = groupRoomCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = roomSearchQuery,
                    onValueChange = { roomSearchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Tìm kiếm nhóm, tin nhắn...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChipItem(
                        text = "Tất cả",
                        selected = selectedFilter == RoomFilter.ALL,
                        onClick = { selectedFilter = RoomFilter.ALL }
                    )
                    FilterChipItem(
                        text = "Chưa đọc",
                        selected = selectedFilter == RoomFilter.UNREAD,
                        onClick = { selectedFilter = RoomFilter.UNREAD }
                    )
                    FilterChipItem(
                        text = "Nhóm",
                        selected = selectedFilter == RoomFilter.GROUPS,
                        onClick = { selectedFilter = RoomFilter.GROUPS }
                    )
                }
            }

            uiState.infoMessage?.let { message ->
                item {
                    BannerMessage(
                        message = message,
                        isError = false,
                        onDismiss = viewModel::clearInfoMessage
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    BannerMessage(
                        message = message,
                        isError = true,
                        onDismiss = { }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Đang hoạt động",
                    subtitle = "Truy cập nhanh những cuộc trò chuyện gần đây"
                )
            }

            item {
                if (activityRooms.isEmpty()) {
                    Text(
                        text = "Chưa có hoạt động gần đây",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            items = activityRooms,
                            key = { it.id }
                        ) { room ->
                            ActiveRoomCard(
                                room = room,
                                isHighlighted = (room.unreadCount[myUid] ?: 0) > 0,
                                onClick = { onOpenChat(room.id, room.name) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Tin nhắn gần đây",
                    subtitle = if (filteredRooms.isEmpty()) "Không có kết quả phù hợp" else "${filteredRooms.size} cuộc trò chuyện"
                )
            }

            when {
                uiState.isLoading && uiState.rooms.isEmpty() -> {
                    item { LoadingRoomPlaceholder() }
                }

                uiState.rooms.isEmpty() -> {
                    item {
                        EmptyRoomsState(
                            title = "Chưa có cuộc trò chuyện nào",
                            subtitle = "Nhấn nút + để tạo phòng hoặc gửi lời mời kết bạn đầu tiên của bạn."
                        )
                    }
                }

                filteredRooms.isEmpty() -> {
                    item {
                        EmptyRoomsState(
                            title = "Không tìm thấy phòng phù hợp",
                            subtitle = "Thử đổi từ khóa tìm kiếm hoặc chuyển sang bộ lọc khác."
                        )
                    }
                }

                else -> {
                    items(
                        items = filteredRooms,
                        key = { it.id }
                    ) { room ->
                        val displayRoomName = room.displayNameFor(myUid)
                        
                        RoomItem(
                            room = room,
                            roomDisplayName = displayRoomName,
                            unreadForMe = room.unreadCount[myUid] ?: 0,
                            myUid = myUid,
                            onClick = { onOpenChat(room.id, displayRoomName) },
                            onLongClick = { viewModel.showDeleteRoomDialog(room.id) }
                        )
                    }
                }
            }
        }

        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissCreateDialog,
                title = { Text("Kết bạn / Mời tham gia") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            label = { Text("Tên hoặc email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.searchQuery.isNotBlank()) {
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
                                                    AvatarWithStatus(
                                                        imageUrl = item.photoUrl,
                                                        name = item.displayName,
                                                        isOnline = item.isOnline,
                                                        size = 36.dp
                                                    )
                                                }
                                            )
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                        uiState.selectedUser?.let {
                            AssistChip(
                                onClick = { },
                                label = { Text("Đã chọn: ${it.displayName}") },
                                leadingIcon = {
                                    AvatarWithStatus(
                                        imageUrl = it.photoUrl,
                                        name = it.displayName,
                                        isOnline = it.isOnline,
                                        size = 24.dp
                                    )
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
                        onClick = viewModel::sendFriendRequest,
                        enabled = uiState.selectedUser != null && !uiState.isCreating
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Gửi")
                        }
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
    myUid: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val previewText = remember(room, myUid) { buildPreviewText(room, myUid) }
    val timeText = remember(room) { formatRoomTime(room.lastMessage?.createdAt ?: room.createdAt, timeFormat) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                url = room.photoUrl,
                name = roomDisplayName,
                size = 56.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = roomDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unreadForMe > 0) FontWeight.Bold else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (unreadForMe > 0) {
                Spacer(Modifier.width(10.dp))
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(unreadForMe.toString())
                }
            }
        }
    }
}

@Composable
private fun ActiveRoomCard(
    room: ChatRoom,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (isHighlighted) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                UserAvatar(
                    url = room.photoUrl,
                    name = room.name,
                    size = 52.dp
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isHighlighted) PrimaryGreen else MaterialTheme.colorScheme.outline)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = room.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BannerMessage(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isError) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Đóng")
            }
        }
    }
}

@Composable
private fun EmptyRoomsState(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingRoomPlaceholder() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Đang tải cuộc trò chuyện...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun roomMatchesQuery(room: ChatRoom, query: String, myUid: String): Boolean {
    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    if (normalizedQuery.isBlank()) return true

    val preview = buildPreviewText(room, myUid)
    val roomText = buildString {
        append(room.name)
        append(' ')
        append(preview)
        append(' ')
        append(room.memberNames.values.joinToString(" "))
    }.lowercase(Locale.getDefault())

    return normalizedQuery in roomText
}

private fun buildPreviewText(room: ChatRoom, myUid: String): String {
    val lastMessage = room.lastMessage ?: return "Chưa có tin nhắn"
    val prefix = when {
        lastMessage.senderId == myUid -> "Bạn: "
        lastMessage.senderName.isNotBlank() -> "${lastMessage.senderName}: "
        else -> ""
    }
    return prefix + lastMessage.content
}

private fun formatRoomTime(date: Date, formatter: SimpleDateFormat): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }
    return if (
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    ) {
        formatter.format(date)
    } else {
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
}

private enum class RoomFilter { ALL, UNREAD, GROUPS }

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
fun AvatarWithStatus(
    imageUrl: String?, 
    name: String, 
    isOnline: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(size)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
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
                    .background(PrimaryGreen, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}