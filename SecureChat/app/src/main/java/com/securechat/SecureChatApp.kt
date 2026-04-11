package com.securechat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureChatApp : Application() {

    companion object {
        const val MESSAGE_CHANNEL_ID = "message_channel"
        const val CALL_CHANNEL_ID = "call_channel"
    }

    override fun onCreate() {
        super.onCreate()
        // Không cần gọi FirebaseApp.initializeApp thủ công nữa
        // Plugin google-services sẽ tự động làm việc này khi có file .json
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Tin nhan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thong bao tin nhan SecureChat"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }

            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Cuoc goi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thong bao cuoc goi den"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(messageChannel)
            manager.createNotificationChannel(callChannel)
        }
    }
}
