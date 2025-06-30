package com.example.EZTravel.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.EZTravel.EZTravelActivity
import com.example.EZTravel.EZTravelScreens
import com.example.EZTravel.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
    notificationChannelManager: NotificationChannelManager
) {

    init {
        notificationChannelManager.createChannels()
    }

    /**
     * Mostra una notifica basandosi sui dati forniti.
     */
    fun showNotification(notificationData: NotificationData) {
        val notificationChannelType = NotificationType.entries[notificationData.type]
        val destinationRoute = createDestinationRoute(notificationData)
        Log.d("NotificationHelper", "Destination route: ${destinationRoute}")
        val intent = Intent(context, EZTravelActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationId", notificationData.id)
            putExtra("destination", destinationRoute)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationData.hashId, // Request code unico
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, notificationChannelType.channelId)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationData.hashId, notification)
    }

    private fun createDestinationRoute(notificationData: NotificationData): String {
        return when (NotificationType.entries[notificationData.type]) {
            NotificationType.LAST_MINUTE_PROPOSAL -> "${EZTravelScreens.TRAVEL_SCREEN}/${notificationData.relatedId}"
            NotificationType.RECOMMENDED_TRIPS -> "${EZTravelScreens.TRAVEL_SCREEN}/${notificationData.relatedId}"
            NotificationType.NEW_APPLICATION -> "${EZTravelScreens.APPLICATIONS_SCREEN}/${notificationData.relatedId}"
            NotificationType.PENDING_APPLICATION_UPDATE -> "${EZTravelScreens.TRAVEL_SCREEN}/${notificationData.relatedId}"
            NotificationType.REVIEW_RECEIVED -> "${EZTravelScreens.TRAVEL_SCREEN}/${notificationData.relatedId}"
            NotificationType.OTHER -> EZTravelScreens.HOME_SCREEN
        }
    }
}