package com.securechat.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.CallStatus
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    calleeName: String,
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.status) {
        if (uiState.status == CallStatus.ENDED) onCallEnded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    init(null, null)
                    setMirror(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!uiState.isCameraOff) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        init(null, null)
                        setMirror(true)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 110.dp, height = 160.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
        ) {
            Text(
                text  = calleeName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (uiState.status) {
                    CallStatus.RINGING  -> "Đang kết nối..."
                    CallStatus.ACCEPTED -> formatDuration(uiState.callDurationSeconds)
                    else                -> "Đã kết thúc"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (uiState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                onClick = viewModel::toggleMic,
                isActive = !uiState.isMicMuted,
                label = if (uiState.isMicMuted) "Bật mic" else "Tắt mic"
            )

            FloatingActionButton(
                onClick = viewModel::endCall,
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.error,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "Kết thúc",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            CallControlButton(
                icon = if (uiState.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                onClick = viewModel::toggleCamera,
                isActive = !uiState.isCameraOff,
                label = if (uiState.isCameraOff) "Bật cam" else "Tắt cam"
            )
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isActive: Boolean,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = if (isActive)
                Color.White.copy(alpha = 0.2f)
            else
                Color.White.copy(alpha = 0.08f),
            shape = CircleShape
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
