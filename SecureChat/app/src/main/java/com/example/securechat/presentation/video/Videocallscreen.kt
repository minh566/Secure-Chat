package com.example.securechat.presentation.video

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.example.securechat.presentation.auth.PrimaryBlue
import com.example.securechat.presentation.home.GroupAvatar
import kotlinx.coroutines.delay

// ── Participant model ─────────────────────────────────────────────────────────
data class VideoParticipant(
    val id: String,
    val name: String,
    val initial: String,
    val avatarColor: Color,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeaking: Boolean = false
)

// ── VideoCallUiState ──────────────────────────────────────────────────────────
data class VideoCallUiState(
    val remoteParticipants: List<VideoParticipant> = emptyList(),
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = true
)

// ── VideoCallViewModel interface (stub dùng từ viewmodel/) ────────────────────
interface VideoCallViewModelInterface {
    val uiState: kotlinx.coroutines.flow.StateFlow<VideoCallUiState>
    fun joinRoom(roomId: String)
    fun toggleMicrophone()
    fun toggleCamera()
    fun switchCamera()
    fun toggleSpeaker()
    fun endCall()
}

// ── VideoCallScreen ───────────────────────────────────────────────────────────
@Composable
fun VideoCallScreen(
    roomId: String,
    groupName: String,
    onEndCall: () -> Unit,
    viewModel: com.example.connectnow.viewmodel.VideoCallViewModel
) {
    val uiState      by viewModel.uiState.collectAsState()
    var showControls  by remember { mutableStateOf(true) }
    var callDuration  by remember { mutableStateOf(0L) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callDuration++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        // ── Layout video theo số người ──────────────────────────────────────
        when (uiState.remoteParticipants.size) {
            0    -> WaitingScreen(groupName)
            1    -> OnePersonLayout(uiState.remoteParticipants.first())
            2    -> TwoPersonLayout(uiState.remoteParticipants)
            else -> GridLayout(uiState.remoteParticipants)
        }

        // ── Local preview góc phải trên ───────────────────────────────────
        AnimatedVisibility(
            visible = !uiState.isCameraOff,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 12.dp),
            enter = fadeIn() + scaleIn(),
            exit  = fadeOut() + scaleOut()
        ) {
            LocalVideoPreview(isMuted = uiState.isMuted)
        }

        // ── Top bar ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn() + slideInVertically(),
            exit  = fadeOut() + slideOutVertically()
        ) {
            VideoTopBar(groupName = groupName, duration = callDuration, onBack = onEndCall)
        }

        // ── Participants strip ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls && uiState.remoteParticipants.size > 3,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            enter = fadeIn() + slideInVertically { it },
            exit  = fadeOut() + slideOutVertically { it }
        ) {
            ParticipantsStrip(uiState.remoteParticipants)
        }

        // ── Controls bar ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit  = fadeOut() + slideOutVertically { it }
        ) {
            VideoControlBar(
                isMuted         = uiState.isMuted,
                isCameraOff     = uiState.isCameraOff,
                isSpeaker       = uiState.isSpeakerOn,
                onToggleMute    = viewModel::toggleMicrophone,
                onToggleCamera  = viewModel::toggleCamera,
                onSwitchCamera  = viewModel::switchCamera,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onEndCall       = { viewModel.endCall(); onEndCall() }
            )
        }
    }
}

// ── Waiting screen ────────────────────────────────────────────────────────────
@Composable
fun WaitingScreen(groupName: String) {
    val pulse by rememberInfiniteTransition(label = "wait").animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulse)
                .background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("📞", fontSize = 40.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(groupName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Đang chờ mọi người tham gia...",
            color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}

// ── 1 người: full screen ──────────────────────────────────────────────────────
@Composable
fun OnePersonLayout(participant: VideoParticipant) {
    Box(Modifier.fillMaxSize()) {
        // Demo: hiện avatar thay vì camera thật
        ParticipantAvatar(participant, Modifier.fillMaxSize())
        ParticipantBadge(
            participant,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}

// ── 2 người: chia đôi ─────────────────────────────────────────────────────────
@Composable
fun TwoPersonLayout(participants: List<VideoParticipant>) {
    Column(Modifier.fillMaxSize()) {
        participants.forEach { p ->
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ParticipantAvatar(p, Modifier.fillMaxSize())
                HorizontalDivider(color = Color(0xFF2A2A3E), thickness = 2.dp)
                ParticipantBadge(p, Modifier.align(Alignment.BottomStart).padding(10.dp))
            }
        }
    }
}

// ── 3+ người: grid 2 cột ─────────────────────────────────────────────────────
@Composable
fun GridLayout(participants: List<VideoParticipant>) {
    val rows = participants.chunked(2)
    Column(Modifier.fillMaxSize()) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { p ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, Color(0xFF2A2A3E))
                    ) {
                        ParticipantAvatar(p, Modifier.fillMaxSize())
                        if (p.isSpeaking) {
                            Box(Modifier.matchParentSize()
                                .border(2.dp, PrimaryBlue, RoundedCornerShape(0.dp)))
                        }
                        ParticipantBadge(p, Modifier.align(Alignment.BottomStart).padding(6.dp))
                    }
                }
                if (row.size == 1) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF111124)))
                }
            }
        }
    }
}

// ── Local preview (góc trên phải) ─────────────────────────────────────────────
@Composable
fun LocalVideoPreview(isMuted: Boolean) {
    Card(
        modifier = Modifier.size(100.dp, 140.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF223344)),
            contentAlignment = Alignment.Center
        ) {
            GroupAvatar(initial = "T", color = PrimaryBlue, size = 44.dp)
            if (isMuted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MicOff, null,
                        tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

// ── Avatar khi camera tắt ─────────────────────────────────────────────────────
@Composable
fun ParticipantAvatar(participant: VideoParticipant, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF1E1E35)),
        contentAlignment = Alignment.Center
    ) {
        GroupAvatar(initial = participant.initial, color = participant.avatarColor, size = 80.dp)
    }
}

// ── Name badge ────────────────────────────────────────────────────────────────
@Composable
fun ParticipantBadge(participant: VideoParticipant, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (participant.isMuted) {
            Icon(Icons.Filled.MicOff, null,
                tint = Color(0xFFFF4444), modifier = Modifier.size(12.dp))
        }
        Text(participant.name, color = Color.White,
            fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ── Participants strip ────────────────────────────────────────────────────────
@Composable
fun ParticipantsStrip(participants: List<VideoParticipant>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(participants) { p ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A3E))
                    .border(
                        width = if (p.isSpeaking) 2.dp else 0.dp,
                        color = if (p.isSpeaking) PrimaryBlue else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                GroupAvatar(p.initial, p.avatarColor, 40.dp)
                if (p.isMuted) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MicOff, null,
                            tint = Color.White, modifier = Modifier.size(9.dp))
                    }
                }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
fun VideoTopBar(groupName: String, duration: Long, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
            ))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, null,
                tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(groupName, color = Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(formatDuration(duration),
                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.PersonAdd, null,
                tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// ── Control bar ───────────────────────────────────────────────────────────────
@Composable
fun VideoControlBar(
    isMuted: Boolean,
    isCameraOff: Boolean,
    isSpeaker: Boolean,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
            ))
            .navigationBarsPadding()
            .padding(vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = if (isSpeaker) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                label = if (isSpeaker) "Loa ngoài" else "Tai nghe",
                isActive = isSpeaker,
                activeColor = Color.White,
                onClick = onToggleSpeaker
            )
            ControlButton(
                icon = if (isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                label = if (isCameraOff) "Bật cam" else "Tắt cam",
                isActive = !isCameraOff,
                activeColor = Color.White,
                activeBackground = Color.White.copy(alpha = 0.2f),
                disabledBackground = Color(0xFF444466),
                onClick = onToggleCamera
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFFF3B30), CircleShape)
                    .clickable(onClick = onEndCall),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CallEnd, "Kết thúc",
                    tint = Color.White, modifier = Modifier.size(28.dp))
            }
            ControlButton(
                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = if (isMuted) "Bật mic" else "Tắt mic",
                isActive = !isMuted,
                activeColor = Color.White,
                activeBackground = Color.White.copy(alpha = 0.2f),
                disabledBackground = Color(0xFF444466),
                onClick = onToggleMute
            )
            ControlButton(
                icon = Icons.Filled.FlipCameraAndroid,
                label = "Đổi cam",
                isActive = true,
                activeColor = Color.White,
                onClick = onSwitchCamera
            )
        }
    }
}

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    activeBackground: Color = Color.White.copy(alpha = 0.2f),
    disabledBackground: Color = Color(0xFF3A3A5C),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(if (isActive) activeBackground else disabledBackground, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────
private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}