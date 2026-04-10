

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

=======
package com.securechat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.ChatRoom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenChat: (roomId: String, roomName: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = viewModel.currentUser
    val currentUserId = user?.uid.orEmpty()
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }

    val filters = remember {
        listOf(
            FilterOption("Tất cả") { _: ChatRoom, _: String -> true },
            FilterOption("Chưa đọc") { room, uid -> (room.unreadCount[uid] ?: 0) > 0 },
            FilterOption("Nhóm") { room, _ -> room.isGroup }
        )
    }

    val filteredRooms = remember(uiState.rooms, searchText, selectedFilter, currentUserId) {
        val query = searchText.trim().lowercase(Locale.getDefault())
        uiState.rooms
            .filter { filters[selectedFilter].predicate(it, currentUserId) }
            .filter { room ->
                if (query.isBlank()) return@filter true
                val name = room.displayNameFor(currentUserId)
                val last = room.lastMessage?.content.orEmpty()
                name.lowercase(Locale.getDefault()).contains(query) ||
                    last.lowercase(Locale.getDefault()).contains(query)
            }
    }

    val activeContacts = remember(uiState.rooms, currentUserId) {
        val now = System.currentTimeMillis()
        uiState.rooms
            .mapNotNull { room ->
                val contactId = room.members.firstOrNull { it != currentUserId } ?: return@mapNotNull null
                val contactName = room.memberNames[contactId].orEmpty().ifBlank { room.displayNameFor(currentUserId) }
                HomeContact(
                    id = contactId,
                    name = contactName,
                    photoUrl = room.memberPhotos[contactId],
                    isActive = room.lastMessage?.createdAt?.let { now - it.time < 45 * 60 * 1000 } == true
                )
            }
            .distinctBy { it.id }
            .take(10)
    }

    LaunchedEffect(uiState.infoMessage) {
        if (uiState.infoMessage != null) {
            kotlinx.coroutines.delay(1800)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Surface(color = Color.White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarWithStatus(
                            imageUrl = user?.photoUrl,
                            name = user?.displayName.orEmpty().ifBlank { "N" },
                            isOnline = true,
                            size = 46.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "ConnectNow",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        HeaderIconButton(icon = Icons.Default.VideoCall, onClick = onOpenSettings)
                        Spacer(Modifier.width(6.dp))
                        HeaderIconButton(icon = Icons.Default.Edit, onClick = onOpenSettings)
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Tìm kiếm nhóm...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF1F3F8),
                            unfocusedContainerColor = Color(0xFFF1F3F8)
                        )
                    )

                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(filters) { index, option ->
                            FilterChip(
                                selected = selectedFilter == index,
                                onClick = { selectedFilter = index },
                                label = { Text(option.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1287FF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF0F2F7),
                                    labelColor = Color(0xFF5D6472)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color.Transparent,
                                    enabled = true,
                                    selected = selectedFilter == index
                                )
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateDialog,
                containerColor = Color(0xFF1287FF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Kết bạn", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            uiState.infoMessage?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
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

                Spacer(Modifier.height(8.dp))
            }

            if (activeContacts.isNotEmpty()) {
                Text(
                    text = "Đang hoạt động",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4A4F5A)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(activeContacts, key = { it.id }) { contact ->
                        ActiveContactItem(contact = contact)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE7EBF2))
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = "Tin nhắn gần đây",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4A4F5A)
            )
            Spacer(Modifier.height(8.dp))

            if (uiState.isLoading && filteredRooms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredRooms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
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
                        items = filteredRooms,
                        key   = { it.id }
                    ) { room ->
                        val displayRoomName = room.displayNameFor(user?.uid.orEmpty())
                        RoomItem(
                            room    = room,

                            currentUserId = user?.uid.orEmpty(),
                            onClick = { onOpenChat(room.id, room.name) }

                            roomDisplayName = displayRoomName,
                            currentUserId = user?.uid ?: "",
                            onClick = { onOpenChat(room.id, displayRoomName) },
                            onLongClick = { viewModel.showDeleteRoomDialog(room.id) }

                        )
                        HorizontalDivider(color = Color(0xFFF0F2F6))
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
                                    .combinedClickable(onClick = { viewModel.onUserSelected(user) })
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

@Composable

private fun RoomItem(room: ChatRoom, currentUserId: String, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val unreadCount = room.unreadCount[currentUserId] ?: 0

@OptIn(ExperimentalFoundationApi::class)
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


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            AvatarWithStatus(
                imageUrl = room.photoUrl,
                name = roomDisplayName,
                isOnline = isRecent(room.lastMessage?.createdAt),
                size = 52.dp
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roomDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val prefix = if (isMine) "Bạn: " else ""
                Text(
                    text = prefix + (room.lastMessage?.content ?: "Chưa có tin nhắn"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnread && !isMine) Color(0xFF131722) else Color(0xFF687082),
                    fontWeight = if (isUnread && !isMine) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                room.lastMessage?.createdAt?.let { date ->
                    Text(
                        text = formatHomeTimestamp(date, timeFormat),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2F95EC),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (unreadCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Badge { Text(unreadCount.toString()) }

                if (unread > 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1788FF)
                    ) {
                        Text(
                            text = unread.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
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

private data class FilterOption(
    val label: String,
    val predicate: (ChatRoom, String) -> Boolean
)

private data class HomeContact(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val isActive: Boolean
)

val PrimaryGreen = Color(0xFF2BC85D)

@Composable
private fun HeaderIconButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1287FF))
    }
}

@Composable
private fun ActiveContactItem(contact: HomeContact) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarWithStatus(
            imageUrl = contact.photoUrl,
            name = contact.name,
            isOnline = contact.isActive,
            size = 66.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5E6471),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AvatarWithStatus(imageUrl: String?, name: String, isOnline: Boolean, size: Dp = 40.dp) {
    Box {
        Surface(
            shape = CircleShape,
            color = Color(0xFFEAF0FF),
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
                        color = Color(0xFF3A82F7)
                    )

                }
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size * 0.28f).coerceAtLeast(10.dp))
                    .align(Alignment.BottomEnd)
                    .background(PrimaryGreen, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

private fun formatHomeTimestamp(date: Date, timeFormat: SimpleDateFormat): String {
    val now = Calendar.getInstance()
    val day = Calendar.getInstance().apply { time = date }

    return when {
        now.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) -> {
            timeFormat.format(date)
        }
        now.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - day.get(Calendar.DAY_OF_YEAR) == 1 -> "Hôm qua"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
}

private fun isRecent(date: Date?): Boolean {
    if (date == null) return false
    return System.currentTimeMillis() - date.time < 45 * 60 * 1000
}


