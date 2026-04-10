package com.securechat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

val PrimaryGreen = Color(0xFF25D366)

@Composable
fun UserAvatar(
    url: String?,
    name: String,
    size: Dp = 48.dp
) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().take(1).ifEmpty { "?" }.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AvatarWithStatus(
    imageUrl: String?,
    name: String,
    isOnline: Boolean,
    size: Dp = 48.dp
) {
    Box {
        UserAvatar(url = imageUrl, name = name, size = size)

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size((size / 4))
                .clip(CircleShape)
                .background(if (isOnline) PrimaryGreen else Color.Gray)
        )
    }
}

