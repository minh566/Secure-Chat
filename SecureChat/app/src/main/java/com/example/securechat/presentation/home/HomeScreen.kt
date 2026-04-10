package com.example.securechat.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.example.securechat.presentation.auth.*

// ── Data class preview ───────────────────────────────────────────────────────
data class GroupPreview(
    val id: String,
    val name: String,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int = 0,
    val avatarColor: Color,
    val avatarInitial: String,
    val isOnline: Boolean = false,
    val memberCount: Int
)

// Demo data
val sampleGroups = listOf(
    GroupPreview("1","Nhóm Công Nghệ", "Minh: Họp lúc 3h chiều nha 👍","10:32", 3, Color(0xFF0084FF),"CN", true, 12),
    GroupPreview("2","Gia đình yêu thương ❤️","Mẹ: Con ăn cơm chưa?","09:15", 1, Color(0xFFE91E8C),"GĐ", true, 5),
    GroupPreview("3","Team Design Sprint","Linh đã gửi 1 ảnh","Hôm qua", 0, Color(0xFF9C27B0),"TD", false, 8),
    GroupPreview("4","Lớp K21 CNTT","An: Bài tập nộp chưa mọi người?","Hôm qua", 12, Color(0xFF00BCD4),"K2", false, 45),
    GroupPreview("5","Dự án App Mobile","Tuấn: Build lỗi rồi anh ơi 😅","T2", 0, Color(0xFF4CAF50),"DA", true, 6),
    GroupPreview("6","Nhóm Du lịch 2025","Hà: Đặt khách sạn xong rồi!","T2", 5, Color(0xFFFF5722),"DL", false, 9),
    GroupPreview("7","Marketing Team","Boss: Báo cáo Q2 chưa xong?","CN", 0, Color(0xFF795548),"MK", false, 11),
    GroupPreview("8","Bạn thân 4 người","Huy: Hôm nay nhậu không?🍺","T6", 2, Color(0xFF607D8B),"BT", true, 4),
)

// ── HomeScreen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGroupClick: (GroupPreview) -> Unit,
    onProfileClick: () -> Unit,
    onCreateGroup: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab  by remember { mutableStateOf(0) }

    val filtered = sampleGroups.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar người dùng
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                            .clickable(onClick = onProfileClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "ConnectNow",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )

                    // Action icons
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.VideoCall, null, tint = PrimaryBlue, modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = onCreateGroup) {
                        Icon(Icons.Outlined.EditNote, null, tint = PrimaryBlue, modifier = Modifier.size(26.dp))
                    }
                }

                // Search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(40.dp)
                        .background(SurfaceLight, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, null,
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                        BasicTextField_Placeholder(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Tìm kiếm nhóm..."
                        )
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tất cả", "Chưa đọc", "Nhóm").forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) PrimaryBlue else SurfaceLight)
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE4E6EB), thickness = 0.5.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGroup,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Filled.Add, "Tạo nhóm mới", modifier = Modifier.size(26.dp))
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Stories / Active now
            item {
                ActiveNowRow()
            }

            item {
                Text(
                    "  Tin nhắn gần đây",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(filtered, key = { it.id }) { group ->
                GroupListItem(group = group, onClick = { onGroupClick(group) })
            }
        }
    }
}

// ── Active Now horizontal row ────────────────────────────────────────────────
@Composable
fun ActiveNowRow() {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "  Đang hoạt động",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sampleGroups.filter { it.isOnline }.take(6)) { group ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    Box {
                        GroupAvatar(
                            initial = group.avatarInitial,
                            color = group.avatarColor,
                            size = 52.dp
                        )
                        // Online dot
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color.White, CircleShape)
                                .background(OnlineGreen, CircleShape)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        group.name.take(8),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Divider(
            color = Color(0xFFE4E6EB),
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

// ── Group list item ───────────────────────────────────────────────────────────
@Composable
fun GroupListItem(group: GroupPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box {
            GroupAvatar(initial = group.avatarInitial, color = group.avatarColor, size = 56.dp)
            if (group.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .border(2.dp, Color.White, CircleShape)
                        .background(OnlineGreen, CircleShape)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Text info
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    group.name,
                    fontWeight = if (group.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    group.lastTime,
                    fontSize = 12.sp,
                    color = if (group.unreadCount > 0) PrimaryBlue else TextSecondary
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    group.lastMessage,
                    fontSize = 13.sp,
                    color = if (group.unreadCount > 0) TextPrimary else TextSecondary,
                    fontWeight = if (group.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Unread badge
                if (group.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(PrimaryBlue, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (group.unreadCount > 99) "99+" else group.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable avatar ───────────────────────────────────────────────────────────
@Composable
fun GroupAvatar(initial: String, color: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(color, color.copy(alpha = 0.7f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.33f).sp
        )
    }
}

// ── Simple placeholder TextField ─────────────────────────────────────────────
@Composable
fun BasicTextField_Placeholder(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(Modifier.fillMaxWidth().padding(start = 8.dp)) {
        if (value.isEmpty()) {
            Text(placeholder, color = TextSecondary, fontSize = 14.sp)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 14.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}