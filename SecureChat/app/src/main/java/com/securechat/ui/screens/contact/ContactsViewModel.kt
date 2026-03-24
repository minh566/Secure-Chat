package com.securechat.ui.screens.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.data.repository.FakeRepository
import com.securechat.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val contacts: List<User> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: FakeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getContacts().collect { list ->
                _uiState.update { it.copy(contacts = list, isLoading = false) }
            }
        }
    }
}
