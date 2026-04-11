package com.securechat.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.ChatRepository
import com.securechat.domain.repository.UserRepository
import com.securechat.domain.usecase.auth.SignOutUseCase
import com.securechat.domain.usecase.chat.CreateRoomUseCase
import com.securechat.domain.usecase.chat.GetChatRoomsUseCase
import com.securechat.ui.common.toSearchResourceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class HomeUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val selectedUser: User? = null,
    val incomingRequests: List<User> = emptyList(),
    val deletingRoomId: String? = null,
    val roomToDeleteId: String? = null,
    val pendingNavigation: PendingHomeNavigation? = null,
    val friendSearchQuery: String = "",
    val friendSearchResults: List<User> = emptyList(),
    val sendingFriendRequestUserId: String? = null,
    val createRoomName: String = "",
    val createRoomSearchQuery: String = "",
    val createRoomImageUri: String? = null,
    val createRoomFriendCandidates: List<User> = emptyList(),
    val createRoomSearchResults: List<User> = emptyList(),
    val selectedRoomMembers: List<User> = emptyList()
)

data class PendingHomeNavigation(
    val roomId: String,
    val roomName: String
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getChatRoomsUseCase: GetChatRoomsUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val chatRepository: ChatRepository,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val friendSearchResultsResource = _uiState
        .map { it.friendSearchQuery }
        .toSearchResourceState(viewModelScope) { query ->
            userRepository.searchUsers(query)
        }

    val currentUser get() = authRepository.currentUser

    init {
        loadRooms()
        setupFriendSearch()
        observeIncomingFriendRequests()
    }

    private fun loadRooms() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            getChatRoomsUseCase(user.uid).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update { state ->
                        val friendCandidates = buildFriendCandidates(result.data, user.uid)
                        val next = state.copy(
                            isLoading = false,
                            rooms = result.data,
                            errorMessage = null,
                            createRoomFriendCandidates = friendCandidates
                        )
                        next.copy(createRoomSearchResults = filterCreateRoomCandidates(next))
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun observeIncomingFriendRequests() {
        viewModelScope.launch {
            userRepository.observeIncomingFriendRequests().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(incomingRequests = result.data) }
                    is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                    else -> Unit
                }
            }
        }
    }

    private fun setupFriendSearch() {
        viewModelScope.launch {
            friendSearchResultsResource.collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update {
                        it.copy(friendSearchResults = result.data)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(friendSearchResults = emptyList(), errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }
            }
        }
    }

    fun onFriendSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                friendSearchQuery = query,
                friendSearchResults = if (query.isBlank()) emptyList() else it.friendSearchResults,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun sendFriendRequestTo(user: User) {
        val current = authRepository.currentUser ?: return
        if (user.uid == current.uid) {
            _uiState.update { it.copy(errorMessage = "Khong the ket ban voi chinh minh") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sendingFriendRequestUserId = user.uid, errorMessage = null, infoMessage = null) }
            when (val result = userRepository.sendFriendRequest(user.uid)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        sendingFriendRequestUserId = null,
                        infoMessage = "Da gui loi moi ket ban toi ${user.displayName}",
                        friendSearchQuery = "",
                        friendSearchResults = emptyList()
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(sendingFriendRequestUserId = null, errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun onSearchQueryChange(query: String) = _uiState.update {
        it.copy(searchQuery = query, selectedUser = null, infoMessage = null)
    }

    fun onUserSelected(user: User) {
        _uiState.update {
            it.copy(
                selectedUser = user,
                searchQuery = user.displayName,
                searchResults = emptyList(),
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun showCreateDialog() = _uiState.update {
        val next = it.copy(
            showCreateDialog = true,
            createRoomName = "",
            createRoomSearchQuery = "",
            createRoomImageUri = null,
            createRoomSearchResults = emptyList(),
            selectedRoomMembers = emptyList(),
            errorMessage = null
        )
        next.copy(createRoomSearchResults = filterCreateRoomCandidates(next))
    }

    fun dismissCreateDialog() = _uiState.update {
        it.copy(
            showCreateDialog = false,
            searchQuery = "",
            searchResults = emptyList(),
            selectedUser = null,
            createRoomName = "",
            createRoomSearchQuery = "",
            createRoomImageUri = null,
            createRoomSearchResults = emptyList(),
            selectedRoomMembers = emptyList(),
            errorMessage = null,
            infoMessage = null
        )
    }

    fun onCreateRoomNameChange(value: String) {
        _uiState.update { it.copy(createRoomName = value, errorMessage = null) }
    }

    fun onCreateRoomSearchQueryChange(value: String) {
        _uiState.update {
            val next = it.copy(
                createRoomSearchQuery = value,
                errorMessage = null
            )
            next.copy(createRoomSearchResults = filterCreateRoomCandidates(next))
        }
    }

    fun onCreateRoomImagePicked(uri: String?) {
        _uiState.update { it.copy(createRoomImageUri = uri) }
    }

    fun addMemberToCreateRoom(user: User) {
        _uiState.update {
            val updatedMembers = (it.selectedRoomMembers + user).distinctBy(User::uid)
            val next = it.copy(
                selectedRoomMembers = updatedMembers,
                createRoomSearchQuery = ""
            )
            next.copy(createRoomSearchResults = filterCreateRoomCandidates(next))
        }
    }

    fun removeMemberFromCreateRoom(userId: String) {
        _uiState.update {
            val next = it.copy(selectedRoomMembers = it.selectedRoomMembers.filterNot { user -> user.uid == userId })
            next.copy(createRoomSearchResults = filterCreateRoomCandidates(next))
        }
    }

    fun createGroupRoom() {
        val current = authRepository.currentUser ?: return
        val selectedMembers = _uiState.value.selectedRoomMembers
        if (selectedMembers.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Vui long chon it nhat 1 thanh vien") }
            return
        }

        val roomName = _uiState.value.createRoomName.trim().ifBlank {
            selectedMembers.joinToString(", ") { it.displayName }.take(40)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            val memberIds = buildList {
                add(current.uid)
                addAll(selectedMembers.map(User::uid))
            }.distinct()

            when (val result = createRoomUseCase(
                name = roomName,
                memberIds = memberIds,
                isGroup = true,
                roomImageUri = _uiState.value.createRoomImageUri
            )) {
                is Resource.Success -> {
                    val room = result.data
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            showCreateDialog = false,
                            createRoomName = "",
                            createRoomSearchQuery = "",
                            createRoomImageUri = null,
                            createRoomSearchResults = emptyList(),
                            selectedRoomMembers = emptyList(),
                            pendingNavigation = PendingHomeNavigation(
                                roomId = room.id,
                                roomName = room.name.ifBlank { roomName }
                            )
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(isCreating = false, errorMessage = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun sendFriendRequest() {
        val selectedUser = _uiState.value.selectedUser
        if (selectedUser == null) {
            _uiState.update { it.copy(errorMessage = "Vui long chon nguoi ban muon ket ban") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null, infoMessage = null) }
            val result = userRepository.sendFriendRequest(selectedUser.uid)
            _uiState.update { it.copy(isCreating = false) }

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            infoMessage = "Da gui loi moi ket ban, cho doi phuong xac nhan",
                            searchQuery = "",
                            searchResults = emptyList(),
                            selectedUser = null,
                            showCreateDialog = false
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun acceptFriendRequest(user: User) {
        val current = authRepository.currentUser ?: return
        viewModelScope.launch {
            when (val result = userRepository.acceptFriendRequest(user.uid)) {
                is Resource.Success -> {
                    createRoomUseCase(user.displayName, listOf(current.uid, user.uid), isGroup = false)
                    _uiState.update { it.copy(infoMessage = "Da chap nhan loi moi ket ban") }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun rejectFriendRequest(user: User) {
        viewModelScope.launch {
            when (val result = userRepository.rejectFriendRequest(user.uid)) {
                is Resource.Success -> _uiState.update { it.copy(infoMessage = "Da tu choi loi moi ket ban") }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun clearInfoMessage() = _uiState.update { it.copy(infoMessage = null) }

    fun clearPendingNavigation() = _uiState.update { it.copy(pendingNavigation = null) }

    fun openOrCreateDirectChat(peerUserId: String, peerDisplayName: String) {
        val currentUserId = authRepository.currentUser?.uid ?: return

        val existingRoom = _uiState.value.rooms.firstOrNull { room ->
            !room.isGroup && room.members.size == 2 && room.members.containsAll(listOf(currentUserId, peerUserId))
        }
        if (existingRoom != null) {
            _uiState.update {
                it.copy(
                    pendingNavigation = PendingHomeNavigation(
                        roomId = existingRoom.id,
                        roomName = existingRoom.displayNameFor(currentUserId)
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            when (val result = createRoomUseCase(peerDisplayName, listOf(currentUserId, peerUserId), isGroup = false)) {
                is Resource.Success -> {
                    val room = result.data
                    _uiState.update {
                        it.copy(
                            pendingNavigation = PendingHomeNavigation(
                                roomId = room.id,
                                roomName = room.displayNameFor(currentUserId)
                            )
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun showDeleteRoomDialog(roomId: String) {
        _uiState.update { it.copy(roomToDeleteId = roomId) }
    }

    fun dismissDeleteRoomDialog() {
        _uiState.update { it.copy(roomToDeleteId = null) }
    }

    fun deleteRoom(roomId: String) {
        if (_uiState.value.deletingRoomId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(deletingRoomId = roomId, errorMessage = null, roomToDeleteId = null) }
            when (val result = chatRepository.deleteChatRoom(roomId)) {
                is Resource.Error -> _uiState.update {
                    it.copy(deletingRoomId = null, errorMessage = result.message)
                }
                else -> _uiState.update { it.copy(deletingRoomId = null) }
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { signOutUseCase() }
            onDone()
        }
    }
}

private fun buildFriendCandidates(rooms: List<ChatRoom>, currentUserId: String): List<User> {
    return rooms
        .asSequence()
        .filter { !it.isGroup && it.members.size == 2 }
        .mapNotNull { room ->
            val friendId = room.members.firstOrNull { memberId -> memberId != currentUserId } ?: return@mapNotNull null
            User(
                uid = friendId,
                displayName = room.memberNames[friendId].orEmpty().ifBlank { room.displayNameFor(currentUserId) },
                email = "",
                photoUrl = room.memberPhotos[friendId].orEmpty().ifBlank { null },
                isOnline = isRecent(room.lastMessage?.createdAt)
            )
        }
        .distinctBy(User::uid)
        .toList()
}

private fun filterCreateRoomCandidates(state: HomeUiState): List<User> {
    val selectedIds = state.selectedRoomMembers.map(User::uid).toSet()
    val query = state.createRoomSearchQuery.trim().lowercase()
    return state.createRoomFriendCandidates.filter { user ->
        user.uid !in selectedIds &&
            (query.isBlank() || user.displayName.lowercase().contains(query))
    }
}

private fun isRecent(date: Date?): Boolean {
    if (date == null) return false
    return System.currentTimeMillis() - date.time < 45 * 60 * 1000
}

private fun ChatRoom.displayNameFor(currentUserId: String): String {
    if (isGroup) return name
    val otherMemberId = members.firstOrNull { it != currentUserId }
    val otherName = otherMemberId
        ?.let { memberNames[it] }
        ?.takeIf { it.isNotBlank() }
    return otherName ?: name
}

