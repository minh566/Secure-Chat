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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val tokens = homeChatListTokens()
    val user = viewModel.currentUser
    val currentUserId = user?.uid.orEmpty()
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }

    val filters = remember {
        listOf(
            FilterOption("Tất cả") { _: ChatRoom, _: String -> true },
            FilterOption("Chưa đọc") { room, uid -> (room.unreadCount[uid] ?: 0) > 0 },
            FilterOption("Nhóm") { room, _ -> room.isGroup }
        )
    }

    val filteredRooms = remember(uiState.rooms, selectedFilter, currentUserId) {
        uiState.rooms
            .filter { filters[selectedFilter].predicate(it, currentUserId) }
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

    LaunchedEffect(uiState.pendingNavigation) {
        val pending = uiState.pendingNavigation ?: return@LaunchedEffect
        onOpenChat(pending.roomId, pending.roomName)
        viewModel.clearPendingNavigation()
    }

    Scaffold(
        containerColor = tokens.screenBackground,
        topBar = {
            Surface(color = tokens.screenBackground) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarWithStatus(
                            imageUrl = user?.photoUrl,
                            name = user?.displayName.orEmpty().ifBlank { "N" },
                            isOnline = true,
                            size = 36.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "securechat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = tokens.title,
                            modifier = Modifier.weight(1f)
                        )
                        HeaderIconButton(icon = Icons.Default.Search, onClick = onOpenSettings)
                        Spacer(Modifier.width(6.dp))
                        HeaderIconButton(icon = Icons.Default.Edit, onClick = onOpenSettings)
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.friendSearchQuery,
                        onValueChange = viewModel::onFriendSearchQueryChange,
                        placeholder = {
                            Text(
                                "Tim nguoi dung de ket ban...",
                                color = tokens.searchText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = tokens.searchText
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = tokens.nameText,
                            unfocusedTextColor = tokens.nameText,
                            focusedContainerColor = tokens.searchContainer,
                            unfocusedContainerColor = tokens.searchContainer
                        )
                    )

                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(filters) { index, option ->
                            FilterChip(
                                selected = selectedFilter == index,
                                onClick = { selectedFilter = index },
                                label = { Text(option.label) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.accent,
                                    selectedLabelColor = Color.White,
                                    containerColor = tokens.searchContainer,
                                    labelColor = tokens.bodyText
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
                containerColor = tokens.accent
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
            AnimatedVisibility(
                visible = uiState.infoMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.infoMessage?.let { message ->
                    Surface(
                        color = tokens.infoContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.infoText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
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

            AnimatedVisibility(
                visible = uiState.friendSearchQuery.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = tokens.listCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (uiState.friendSearchResults.isEmpty()) {
                            Text(
                                text = "Khong tim thay nguoi dung phu hop",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.metaText,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            uiState.friendSearchResults.take(6).forEach { friend ->
                                ListItem(
                                    headlineContent = { Text(friend.displayName) },
                                    supportingContent = { Text(friend.email) },
                                    leadingContent = {
                                        AvatarWithStatus(
                                            imageUrl = friend.photoUrl,
                                            name = friend.displayName,
                                            isOnline = friend.isOnline
                                        )
                                    },
                                    trailingContent = {
                                        val isSending = uiState.sendingFriendRequestUserId == friend.uid
                                        TextButton(
                                            onClick = { viewModel.sendFriendRequestTo(friend) },
                                            enabled = !isSending
                                        ) {
                                            if (isSending) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text("Ket ban")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (activeContacts.isNotEmpty()) {
                Text(
                    text = "LIVE NOW",
                    style = sectionLabelTextStyle(),
                    color = tokens.sectionLabel,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(activeContacts, key = { it.id }) { contact ->
                        ActiveContactItem(
                            contact = contact,
                            onClick = { viewModel.openOrCreateDirectChat(contact.id, contact.name) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = tokens.divider)
                Spacer(Modifier.height(12.dp))
            }

            if (filteredRooms.isNotEmpty()) {
                Text(
                    text = "FEATURED",
                    style = sectionLabelTextStyle(),
                    color = tokens.sectionLabel,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FeaturedRoomCard(
                    room = filteredRooms.first(),
                    currentUserId = user?.uid ?: "",
                    tokens = tokens,
                    onClick = {
                        val firstRoom = filteredRooms.first()
                        onOpenChat(firstRoom.id, firstRoom.displayNameFor(user?.uid.orEmpty()))
                    }
                )
                Spacer(Modifier.height(14.dp))
            }

            Text(
                text = "RECENT CHATS",
                style = sectionLabelTextStyle(),
                color = tokens.sectionLabel,
                fontWeight = FontWeight.SemiBold
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
                            tint = tokens.emptyIcon
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Chưa có cuộc trò chuyện nào",
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.bodyText
                        )
                        Text(
                            "Nhan + de ket ban va bat dau tro chuyen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.metaText
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(
                        items = filteredRooms.drop(1),
                        key   = { it.id }
                    ) { room ->
                        val displayRoomName = room.displayNameFor(user?.uid.orEmpty())
                        RoomItem(
                            room    = room,
                            roomDisplayName = displayRoomName,
                            currentUserId = user?.uid ?: "",
                            tokens = tokens,
                            onClick = { onOpenChat(room.id, displayRoomName) },
                            onLongClick = { viewModel.showDeleteRoomDialog(room.id) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        if (uiState.showCreateDialog) {
            val imagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { pickedUri ->
                viewModel.onCreateRoomImagePicked(pickedUri?.toString())
            }

            AlertDialog(
                onDismissRequest = viewModel::dismissCreateDialog,
                title   = { Text("Tao phong chat") },
                text    = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AvatarWithStatus(
                                imageUrl = uiState.createRoomImageUri,
                                name = uiState.createRoomName.ifBlank { "R" },
                                isOnline = false,
                                size = 52.dp
                            )
                            TextButton(onClick = { imagePicker.launch("image/*") }) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Dat anh phong")
                            }
                            if (uiState.createRoomImageUri != null) {
                                TextButton(onClick = { viewModel.onCreateRoomImagePicked(null) }) {
                                    Text("Xoa")
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.createRoomName,
                            onValueChange = viewModel::onCreateRoomNameChange,
                            label = { Text("Ten phong (tuy chon)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.createRoomSearchQuery,
                            onValueChange = viewModel::onCreateRoomSearchQueryChange,
                            label = { Text("Them thanh vien") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        if (uiState.selectedRoomMembers.isNotEmpty()) {
                            Text(
                                text = "Da chon (${uiState.selectedRoomMembers.size}):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            AnimatedSelectedMembersRow(
                                members = uiState.selectedRoomMembers,
                                onRemoveMember = viewModel::removeMemberFromCreateRoom
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Nhan vao chip de xoa thanh vien truoc khi tao phong",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.metaText
                            )
                        }

                        if (uiState.createRoomFriendCandidates.isEmpty()) {
                            Text(
                                text = "Ban can ket ban truoc khi them vao phong",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.metaText,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            uiState.createRoomSearchResults.take(8).forEach { friend ->
                                ListItem(
                                    headlineContent = { Text(friend.displayName) },
                                    supportingContent = { Text("Ban be") },
                                    leadingContent = {
                                        AvatarWithStatus(
                                            imageUrl = friend.photoUrl,
                                            name = friend.displayName,
                                            isOnline = friend.isOnline
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(onClick = { viewModel.addMemberToCreateRoom(friend) })
                                )
                            }
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
                        onClick  = viewModel::createGroupRoom,
                        enabled  = uiState.selectedRoomMembers.isNotEmpty() && !uiState.isCreating
                    ) {
                        if (uiState.isCreating) CircularProgressIndicator(Modifier.size(16.dp))
                        else Text("Tao phong")
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
@OptIn(ExperimentalFoundationApi::class)
private fun RoomItem(
    room: ChatRoom,
    roomDisplayName: String,
    currentUserId: String,
    tokens: HomeChatListTokens,
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
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.listCard)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            AvatarWithStatus(
                imageUrl = room.photoUrl,
                name = roomDisplayName,
                isOnline = isRecent(room.lastMessage?.createdAt),
                size = 46.dp
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roomDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.nameText,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val prefix = if (isMine) "Bạn: " else ""
                Text(
                    text = prefix + (room.lastMessage?.content ?: "Chưa có tin nhắn"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUnread && !isMine) tokens.nameText else tokens.bodyText,
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
                        color = tokens.metaText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (unread > 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = tokens.badge
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedRoomCard(
    room: ChatRoom,
    currentUserId: String,
    tokens: HomeChatListTokens,
    onClick: () -> Unit
) {
    val name = room.displayNameFor(currentUserId)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = tokens.featuredCard,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithStatus(
                imageUrl = room.photoUrl,
                name = name,
                isOnline = isRecent(room.lastMessage?.createdAt),
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.nameText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = room.lastMessage?.content ?: "Let's start chatting...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.bodyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(16.dp)
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

@Composable
private fun HeaderIconButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = null,
            tint = homeChatListTokens().headerIcon,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveContactItem(contact: HomeContact, onClick: () -> Unit) {
    val tokens = homeChatListTokens()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tokens.activeNowCard,
        modifier = Modifier.combinedClickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            AvatarWithStatus(
                imageUrl = contact.photoUrl,
                name = contact.name,
                isOnline = contact.isActive,
                size = 50.dp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.nameText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Typing...",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.metaText
            )
        }
    }
}

@Composable
private fun sectionLabelTextStyle(): TextStyle {
    return MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedSelectedMembersRow(
    members: List<com.securechat.domain.model.User>,
    onRemoveMember: (String) -> Unit
) {
    val animatedMembers = remember { mutableStateListOf<com.securechat.domain.model.User>() }
    val visibilityStates = remember { mutableStateMapOf<String, MutableTransitionState<Boolean>>() }

    LaunchedEffect(members) {
        val incomingIds = members.map { it.uid }.toSet()

        members.forEach { member ->
            val existingIndex = animatedMembers.indexOfFirst { it.uid == member.uid }
            if (existingIndex >= 0) {
                animatedMembers[existingIndex] = member
            } else {
                animatedMembers.add(member)
                visibilityStates[member.uid] = MutableTransitionState(false).apply { targetState = true }
            }
        }

        animatedMembers
            .filter { it.uid !in incomingIds }
            .forEach { removed ->
                visibilityStates[removed.uid]?.targetState = false
            }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(animatedMembers, key = { it.uid }) { member ->
            val visibleState = visibilityStates.getOrPut(member.uid) {
                MutableTransitionState(true).apply { targetState = true }
            }

            if (!visibleState.currentState && !visibleState.targetState) {
                LaunchedEffect(member.uid) {
                    animatedMembers.removeAll { it.uid == member.uid }
                    visibilityStates.remove(member.uid)
                }
            }

            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    scaleOut(targetScale = 0.82f, animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
            ) {
                AssistChip(
                    onClick = { onRemoveMember(member.uid) },
                    label = { Text(member.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        AvatarWithStatus(
                            imageUrl = member.photoUrl,
                            name = member.displayName,
                            isOnline = member.isOnline,
                            size = 28.dp
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xoa thanh vien",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
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

