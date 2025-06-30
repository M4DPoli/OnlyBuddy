package com.example.EZTravel.notification


object NotificationFactory {

    /*
    fun createNotificationData(remoteMessage: Map<String, String>): NotificationData {
        //This method throws exceptions if something fails during the parsing

        return NotificationData(
            id = remoteMessage["id"]!!,
            type = remoteMessage["type"]!!.toInt(),
            relatedId = remoteMessage["relatedId"],
            isRead = remoteMessage["isRead"].toBoolean(),
            timestamp = remoteMessage["timestamp"]?.toLong() ?: 0L,
            title = remoteMessage["title"]!!,
            message = remoteMessage["message"]!!
        )
    }

     */


    //DA PORTARE A BE
    /*
    fun updateNotificationContent(data: NotificationData): NotificationContent {
        return when (NotificationType.entries[data.type]) {
            NotificationType.LAST_MINUTE_PROPOSAL -> NotificationContent(
                title = "Last Minute Proposal",
                message = "A last-minute trip is available for booking!"
            )
            NotificationType.RECOMMENDED_TRIPS -> NotificationContent(
                title = "Recommended Trip",
                message = "We found a trip recommendation for you!"
            )
            NotificationType.NEW_APPLICATION -> NotificationContent(
                title = "New Application",
                message = "A new application requires your attention."
            )
            NotificationType.PENDING_APPLICATION_UPDATE -> NotificationContent(
                title = "Application Update",
                message = "Your application status has been updated."
            )
            NotificationType.REVIEW_RECEIVED -> NotificationContent(
                title = "New Review",
                message = "You received a new review."
            )
            NotificationType.OTHER -> NotificationContent(
                title = "Notification",
                message = "You have a new notification."
            )
        }
    }

     */

}
