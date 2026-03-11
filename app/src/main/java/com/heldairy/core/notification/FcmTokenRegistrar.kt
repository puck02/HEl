package com.heldairy.core.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.heldairy.HElDairyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"

    fun registerCurrentToken(context: Context) {
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNullOrBlank()) return@addOnSuccessListener
                    registerTokenWithBackend(context, token)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "FCM token fetch failed: ${e.message}")
                }
        }.onFailure {
            Log.w(TAG, "FirebaseMessaging not ready: ${it.message}")
        }
    }

    fun registerTokenWithBackend(context: Context, token: String) {
        val app = context.applicationContext as? HElDairyApplication ?: return
        val agentClient = app.appContainer.agentClient ?: return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                agentClient.upsertPushToken(token)
            }.onFailure {
                Log.w(TAG, "Push token register failed: ${it.message}")
            }
        }
    }
}
