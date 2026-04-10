
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.CallStatus
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    calleeName: String,
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eglContext = viewModel.getEglContext()
    var remoteRendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var localRendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearLocalSink()
            viewModel.clearRemoteSink()

            localRendererRef?.let { renderer ->
                runCatching {
                    renderer.clearImage()
                    renderer.release()
                }
                localRendererRef = null
                viewModel.onLocalRendererReleased()
            }

            remoteRendererRef?.let { renderer ->
                runCatching {
                    renderer.clearImage()
                    renderer.release()
                }
                remoteRendererRef = null
                viewModel.onRemoteRendererReleased()
            }
        }
    }

    LaunchedEffect(uiState.status) {
        if (uiState.status == CallStatus.ENDED ||
            uiState.status == CallStatus.DECLINED ||
            uiState.status == CallStatus.MISSED
        ) {
            onCallEnded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // ── REMOTE VIDEO (Màn hình lớn) ──
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    init(eglContext, null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                    // Đăng ký sink với WebRTCManager
                    viewModel.setRemoteSink(this)
                    remoteRendererRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { renderer ->
                viewModel.clearRemoteSink()
                runCatching {
                    renderer.clearImage()
                    renderer.release()
                }
                remoteRendererRef = null
                viewModel.onRemoteRendererReleased()
            }
        )

        // ── LOCAL VIDEO (Màn hình nhỏ góc trên) ──
        if (!uiState.isCameraOff) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 120.dp, height = 180.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(eglContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setEnableHardwareScaler(true)
                            setMirror(true)
                            // Đăng ký sink với WebRTCManager
                            viewModel.setLocalSink(this)
                            localRendererRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { renderer ->
                        viewModel.clearLocalSink()
                        runCatching {
                            renderer.clearImage()
                            renderer.release()
                        }
                        localRendererRef = null
                        viewModel.onLocalRendererReleased()
                    }
                )
            }
        }

        // ── THÔNG TIN CUỘC GỌI ──
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

        // ── ĐIỀU KHIỂN ──
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
