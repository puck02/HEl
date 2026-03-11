package com.heldairy.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.heldairy.core.network.agent.AgentServerNotification

class HElFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isNotBlank()) {
            FcmTokenRegistrar.registerTokenWithBackend(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "健康提醒"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "你有一条新的消息"

        val notification = AgentServerNotification(
            id = message.messageId ?: "fcm-${System.currentTimeMillis()}",
            title = title,
            body = body,
            type = message.data["type"] ?: "general",
            data = emptyMap(),
            createdAt = System.currentTimeMillis()
        )

        runCatching {
            ServerNotificationHelper.show(applicationContext, notification)
        }.onFailure {
            Log.w(TAG, "Show FCM notification failed: ${it.message}")
        }
    }

    companion object {
        private const val TAG = "HElFirebaseMsgService"
    }
}
