package com.securechat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.securechat.MainActivity
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.CallRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SecureChatFCMService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var callRepository: CallRepository

    // Khi Firebase cấp token mới (app cài lần đầu hoặc token hết hạn)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Lưu token mới lên Firestore để server gửi notification
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.updateFcmToken(token)
        }
    }

    // Khi nhận notification (dù app đang chạy hay bị kill)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        when (data["type"]) {
            "INCOMING_CALL" -> handleIncomingCall(message, data)
            "NEW_MESSAGE"   -> handleNewMessage(data)
        }
    }

    private fun handleIncomingCall(message: RemoteMessage, data: Map<String, String>) {
        val callerName = data["callerName"] ?: "Người dùng"
        val sessionId  = data["sessionId"] ?: return
        val peerId     = data["callerId"] ?: return
        val callType   = data["callType"] ?: "VIDEO"

        val now = System.currentTimeMillis()
        val sentAt = message.sentTime
        val timeoutMs = 3000L
        val isStale = sentAt > 0L && (now - sentAt) > timeoutMs
        if (isStale) {
            CoroutineScope(Dispatchers.IO).launch {
                callRepository.declineCall(sessionId)
            }
            return
        }

        showCallNotification(callerName, sessionId, peerId, callType)
    }

    private fun handleNewMessage(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "Tin nhắn mới"
        val content    = data["content"] ?: ""
        val roomId     = data["roomId"] ?: return

        showMessageNotification(senderName, content, roomId)
    }

    private fun showCallNotification(callerName: String, sessionId: String, peerId: String, callType: String) {
        val channelId = "call_channel"
        createNotificationChannel(channelId, "Cuộc gọi đến", NotificationManager.IMPORTANCE_HIGH)

        // Intent mở màn hình nhận cuộc gọi
        val acceptIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("action", "ACCEPT_CALL")
                putExtra("sessionId", sessionId)
                putExtra("callerName", callerName)
                putExtra("peerId", peerId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Cuộc gọi ${if (callType == "VIDEO") "video" else "thoại"} đến")
            .setContentText("$callerName đang gọi cho bạn")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(acceptIntent)
            .setFullScreenIntent(acceptIntent, true)  // Hiện full screen khi điện thoại khóa
            .addAction(android.R.drawable.ic_menu_call, "Nhận", acceptIntent)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(sessionId.hashCode(), notification)
    }

    private fun showMessageNotification(senderName: String, content: String, roomId: String) {
        val channelId = "message_channel"
        createNotificationChannel(channelId, "Tin nhắn mới", NotificationManager.IMPORTANCE_DEFAULT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(senderName)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(roomId.hashCode(), notification)
    }

    private fun createNotificationChannel(id: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
