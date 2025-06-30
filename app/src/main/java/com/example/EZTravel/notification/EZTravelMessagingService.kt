package com.example.EZTravel.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EZTravelMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var notificationModel: NotificationModel

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        processRemoteMessage(remoteMessage.data)
    }

    private fun processRemoteMessage(data: Map<String, String>) {
        NotificationData.fromRemoteMessage(data).onSuccess { notificationData ->
            Log.d("EZTravelMessagingService", "Received notification" +
                    "relatedId: ${notificationData.relatedId}")
            notificationHelper.showNotification(notificationData)
        }.onFailure { error ->
            Log.e("EZTravelMessagingService", "Error processing notification: ${error.message}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        serviceScope.launch {
            handleTokenUpdate(token)
        }
    }

    private suspend fun handleTokenUpdate(token: String) {
        notificationModel.registerToken(token).onSuccess {
            Log.d("EZTravelMessagingService", "Token saved in Firestore")
        }.onFailure { error ->
            Log.e("EZTravelMessagingService", "Error saving FCM token: ${error.message}")
        }
    }

}
