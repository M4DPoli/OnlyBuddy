package com.example.EZTravel.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    private val notificationManager: NotificationManager
) {
    fun createChannels() {
        NotificationType.entries.forEach { type ->
            val channelId = type.channelId
            val channelName = type.channelName

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for $channelName"
            }

            try {
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e("NotificationChannel", "Error creating notification channel: ${e.message}")
            }
        }
    }
}
