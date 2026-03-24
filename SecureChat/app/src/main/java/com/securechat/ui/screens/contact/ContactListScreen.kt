package com.securechat.ui.screens.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securechat.ui.screens.home.AvatarWithStatus
import com.securechat.ui.screens.home.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onBack: () -> Unit,
    onStartChat: (roomId: String, roomName: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                color = PrimaryGreen,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    TopAppBar(
                        title = { Text("Contacts", color = Color.White, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* Add contact logic */ }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // Search Bar inside Header
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search contacts...", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Mock Data
            val contacts = listOf(
                Contact("Alex Johnson", "Available", true),
                Contact("Maria Garcia", "Busy", true),
                Contact("James Smith", "Away", false),
                Contact("Linda Williams", "Available", true),
                Contact("Robert Brown", "At the gym", false)
            )

            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onStartChat("room_${contact.name}", contact.name) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}

data class Contact(val name: String, val status: String, val isOnline: Boolean)

@Composable
fun ContactItem(contact: Contact, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(contact.name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(contact.status, color = Color.Gray) },
        leadingContent = {
            AvatarWithStatus(imageUrl = "", name = contact.name, isOnline = contact.isOnline)
        }
    )
}
