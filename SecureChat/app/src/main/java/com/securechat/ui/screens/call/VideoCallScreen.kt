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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securechat.domain.model.CallStatus
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    calleeName: String,
    isGroupCall: Boolean,
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF070C1B), Color(0xFF0B122A), Color(0xFF0A0F21))
                )
            )
    ) {
        if (isGroupCall) {
            GroupCallLayout(
                status = uiState.status,
                isCameraOff = uiState.isCameraOff,
                eglContext = eglContext,
                onRemoteRendererReady = { renderer ->
                    viewModel.setRemoteSink(renderer)
                    remoteRendererRef = renderer
                },
                onRemoteRendererReleased = { renderer ->
                    viewModel.clearRemoteSink()
                    runCatching {
                        renderer.clearImage()
                        renderer.release()
                    }
                    remoteRendererRef = null
                    viewModel.onRemoteRendererReleased()
                },
                onLocalRendererReady = { renderer ->
                    viewModel.setLocalSink(renderer)
                    localRendererRef = renderer
                },
                onLocalRendererReleased = { renderer ->
                    viewModel.clearLocalSink()
                    runCatching {
                        renderer.clearImage()
                        renderer.release()
                    }
                    localRendererRef = null
                    viewModel.onLocalRendererReleased()
                }
            )
        } else {
            OneToOneCallLayout(
                status = uiState.status,
                isCameraOff = uiState.isCameraOff,
                eglContext = eglContext,
                onRemoteRendererReady = { renderer ->
                    viewModel.setRemoteSink(renderer)
                    remoteRendererRef = renderer
                },
                onRemoteRendererReleased = { renderer ->
                    viewModel.clearRemoteSink()
                    runCatching {
                        renderer.clearImage()
                        renderer.release()
                    }
                    remoteRendererRef = null
                    viewModel.onRemoteRendererReleased()
                },
                onLocalRendererReady = { renderer ->
                    viewModel.setLocalSink(renderer)
                    localRendererRef = renderer
                },
                onLocalRendererReleased = { renderer ->
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

        TopOverlay(
            calleeName = calleeName,
            statusText = statusLabel(uiState.status, uiState.callDurationSeconds),
            isGroupCall = isGroupCall,
            onBack = viewModel::endCall
        )

        BottomControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
            isSpeakerOn = uiState.isSpeakerOn,
            isCameraOff = uiState.isCameraOff,
            isMicMuted = uiState.isMicMuted,
            onToggleSpeaker = viewModel::toggleSpeaker,
            onToggleCamera = viewModel::toggleCamera,
            onToggleMic = viewModel::toggleMic,
            onSwitchCamera = viewModel::switchCamera,
            onEndCall = viewModel::endCall
        )
    }
}

@Composable
private fun OneToOneCallLayout(
    status: CallStatus,
    isCameraOff: Boolean,
    eglContext: EglBase.Context,
    onRemoteRendererReady: (SurfaceViewRenderer) -> Unit,
    onRemoteRendererReleased: (SurfaceViewRenderer) -> Unit,
    onLocalRendererReady: (SurfaceViewRenderer) -> Unit,
    onLocalRendererReleased: (SurfaceViewRenderer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 92.dp, bottom = 118.dp, start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CallTile(modifier = Modifier.weight(1f), label = "Camera doi phuong") {
            if (status == CallStatus.ACCEPTED) {
                VideoRendererView(
                    modifier = Modifier.fillMaxSize(),
                    eglContext = eglContext,
                    onRendererReady = onRemoteRendererReady,
                    onRendererReleased = onRemoteRendererReleased,
                    mirror = false
                )
            } else {
                WaitingVideoPanel(text = "Dang ket noi camera doi phuong...")
            }
        }

        CallTile(modifier = Modifier.weight(1f), label = "Camera cua ban") {
            if (isCameraOff) {
                CameraDisabledPanel(text = "Camera cua ban dang tat")
            } else {
                VideoRendererView(
                    modifier = Modifier.fillMaxSize(),
                    eglContext = eglContext,
                    onRendererReady = onLocalRendererReady,
                    onRendererReleased = onLocalRendererReleased,
                    mirror = true
                )
            }
        }
    }
}

@Composable
private fun GroupCallLayout(
    status: CallStatus,
    isCameraOff: Boolean,
    eglContext: EglBase.Context,
    onRemoteRendererReady: (SurfaceViewRenderer) -> Unit,
    onRemoteRendererReleased: (SurfaceViewRenderer) -> Unit,
    onLocalRendererReady: (SurfaceViewRenderer) -> Unit,
    onLocalRendererReleased: (SurfaceViewRenderer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 92.dp, bottom = 118.dp, start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CallTile(modifier = Modifier.weight(1f), label = "Doi phuong") {
                if (status == CallStatus.ACCEPTED) {
                    VideoRendererView(
                        modifier = Modifier.fillMaxSize(),
                        eglContext = eglContext,
                        onRendererReady = onRemoteRendererReady,
                        onRendererReleased = onRemoteRendererReleased,
                        mirror = false
                    )
                } else {
                    WaitingVideoPanel(text = "Dang ket noi...")
                }
            }
            CallTile(modifier = Modifier.weight(1f), label = "Ban") {
                if (isCameraOff) {
                    CameraDisabledPanel(text = "Camera tat")
                } else {
                    VideoRendererView(
                        modifier = Modifier.fillMaxSize(),
                        eglContext = eglContext,
                        onRendererReady = onLocalRendererReady,
                        onRendererReleased = onLocalRendererReleased,
                        mirror = true
                    )
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CallTile(modifier = Modifier.weight(1f), label = "Thanh vien") {
                WaitingVideoPanel(text = "Dang cho nguoi tham gia")
            }
            CallTile(modifier = Modifier.weight(1f), label = "Thanh vien") {
                WaitingVideoPanel(text = "Dang cho nguoi tham gia")
            }
        }
    }
}

@Composable
private fun CallTile(
    modifier: Modifier,
    label: String,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF253365), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0D1329)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xAA0A1023),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = label,
                    color = Color(0xFFD5E0FF),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoRendererView(
    modifier: Modifier,
    eglContext: EglBase.Context,
    onRendererReady: (SurfaceViewRenderer) -> Unit,
    onRendererReleased: (SurfaceViewRenderer) -> Unit,
    mirror: Boolean
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                setMirror(mirror)
                onRendererReady(this)
            }
        },
        modifier = modifier,
        onRelease = onRendererReleased
    )
}

@Composable
private fun WaitingVideoPanel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1F)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF9EABD8),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CameraDisabledPanel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = null,
                tint = Color(0xFF8A97C5),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                color = Color(0xFF9EABD8),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun TopOverlay(
    calleeName: String,
    statusText: String,
    isGroupCall: Boolean,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xAA0B1225)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = calleeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isGroupCall) "Nhom | $statusText" else statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBDD8FF)
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    modifier: Modifier,
    isSpeakerOn: Boolean,
    isCameraOff: Boolean,
    isMicMuted: Boolean,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x8A141B34))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallControlButton(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = onToggleSpeaker,
            isActive = isSpeakerOn,
            label = "Loa"
        )
        CallControlButton(
            icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
            onClick = onToggleCamera,
            isActive = !isCameraOff,
            label = "Camera"
        )

        FloatingActionButton(
            onClick = onEndCall,
            modifier = Modifier.size(62.dp),
            containerColor = Color(0xFFFF3B30),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Kết thúc",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

        CallControlButton(
            icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
            onClick = onToggleMic,
            isActive = !isMicMuted,
            label = "Micro"
        )
        CallControlButton(
            icon = Icons.Default.Cameraswitch,
            onClick = onSwitchCamera,
            isActive = true,
            label = "Đổi"
        )
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
            modifier = Modifier.size(56.dp),
            containerColor = if (isActive) Color(0xFF3E67FF) else Color(0xFF31354A),
            shape = CircleShape
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

private fun statusLabel(status: CallStatus, seconds: Int): String {
    return when (status) {
        CallStatus.RINGING -> "Dang ket noi..."
        CallStatus.ACCEPTED -> formatDuration(seconds)
        CallStatus.ENDED -> "Da ket thuc"
        CallStatus.DECLINED -> "Da tu choi"
        CallStatus.MISSED -> "Khong tra loi"
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
