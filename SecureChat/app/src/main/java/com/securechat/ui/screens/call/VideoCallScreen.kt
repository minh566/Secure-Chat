<<<<<<< HEAD

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
=======
package com.securechat.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
            .background(Color(0xFF0B1029))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                CallTile(
                    modifier = Modifier.weight(1f),
                    label = calleeName,
                    labelBg = Color(0xAA0A0C17)
                ) {
                    AndroidView(
                        factory = { context ->
                            SurfaceViewRenderer(context).apply {
                                init(eglContext, null)
                                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                setEnableHardwareScaler(true)
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
                }

                CallTile(
                    modifier = Modifier.weight(1f),
                    label = "Bạn",
                    labelBg = Color(0xAA0A0C17)
                ) {
                    if (uiState.isCameraOff) {
                        PlaceholderAvatar(initial = "B", color = Color(0xFF8A39D8))
                    } else {
                        AndroidView(
                            factory = { context ->
                                SurfaceViewRenderer(context).apply {
                                    init(eglContext, null)
                                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                    setEnableHardwareScaler(true)
                                    setMirror(true)
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
            }

            Row(modifier = Modifier.weight(1f)) {
                CallTile(
                    modifier = Modifier.weight(1f),
                    label = "Linh",
                    labelBg = Color(0xAA0A0C17)
                ) {
                    PlaceholderAvatar(initial = "L", color = Color(0xFFEF651D))
                }

                CallTile(
                    modifier = Modifier.weight(1f),
                    label = "",
                    labelBg = Color.Transparent
                ) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF070C23)))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::endCall) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = calleeName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (uiState.status) {
                            CallStatus.RINGING -> "Đang kết nối..."
                            CallStatus.ACCEPTED -> formatDuration(uiState.callDurationSeconds)
                            CallStatus.ENDED -> "Đã kết thúc"
                            CallStatus.DECLINED -> "Đã từ chối"
                            CallStatus.MISSED -> "Không trả lời"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF18314A).copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Surface(shape = CircleShape, color = Color(0xFF1D92FF)) {
                            Text(
                                text = calleeName.take(1).uppercase(),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                onClick = {},
                isActive = uiState.isSpeakerOn,
                label = "Loa ngoài"
            )
            CallControlButton(
                icon = if (uiState.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                onClick = viewModel::toggleCamera,
                isActive = !uiState.isCameraOff,
                label = "Tắt cam"
            )

            FloatingActionButton(
                onClick = viewModel::endCall,
                modifier = Modifier.size(70.dp),
                containerColor = Color(0xFFFF3B30),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "Kết thúc",
                    modifier = Modifier.size(30.dp),
                    tint = Color.White
                )
            }

            CallControlButton(
                icon = if (uiState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                onClick = viewModel::toggleMic,
                isActive = !uiState.isMicMuted,
                label = "Tắt mic"
            )
            CallControlButton(
                icon = Icons.Default.Cameraswitch,
                onClick = {},
                isActive = true,
                label = "Đổi cam"
            )
        }
    }
}

@Composable
private fun CallTile(
    modifier: Modifier,
    label: String,
    labelBg: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp)
            .border(width = 1.dp, color = Color(0x662E72F8))
            .background(Color(0xFF111633))
    ) {
        content()
        if (label.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = labelBg,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaceholderAvatar(initial: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isActive: Boolean,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(58.dp),
            containerColor = if (isActive) Color(0xFF4D5368) else Color(0xFF373E52),
            shape = CircleShape
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
>>>>>>> 22c3a84 (feat: redesign core screens and wire settings with biometric app lock)
