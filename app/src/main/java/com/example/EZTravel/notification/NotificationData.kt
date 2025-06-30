package com.example.EZTravel.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class NotificationData(
    @get:Exclude
    var id: String = "",
    var type: Int = 5,
    @set:PropertyName("related_id")
    @get:PropertyName("related_id")
    var relatedId: String? = null,              // ID associato (tripId, applicationId, ecc.)
    @get:PropertyName("is_read")
    @set:PropertyName("is_read")
    var isRead: Boolean = false,
    var timestamp: Timestamp = Timestamp.now(),
    var title: String = "",
    var message: String = "",
    @set:Exclude
    @get:Exclude
    var hashId: Int = 0
    //val data: Map<String, Any> = emptyMap() // Dati specifici del tipo di notifica
) {
    companion object {
        fun fromRemoteMessage(remoteMessage: Map<String, String>): Result<NotificationData> {
            return runCatching {
                val timestampMillis = remoteMessage["timestamp"]?.toLongOrNull()
                    ?: throw IllegalArgumentException("Invalid or missing timestamp")

                NotificationData(
                    id = remoteMessage["id"] ?: throw IllegalArgumentException("Missing id"),
                    type = remoteMessage["type"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid type"),
                    relatedId = remoteMessage["related_id"],
                    isRead = remoteMessage["is_read"]?.toBoolean() ?: false,
                    timestamp = Timestamp(timestampMillis / 1000, (timestampMillis % 1000).toInt() * 1_000_000),
                    title = remoteMessage["title"] ?: "",
                    message = remoteMessage["message"] ?: "",
                    hashId = remoteMessage["id"].hashCode()
                )
            }
        }
    }

}

enum class NotificationType(val channelId: String, val channelName: String) {
    LAST_MINUTE_PROPOSAL("last_minute_channel", "Last Minute Proposals"),
    RECOMMENDED_TRIPS("recommended_trips_channel", "Recommended Trips"),
    NEW_APPLICATION("new_application_channel", "New Applications"),
    PENDING_APPLICATION_UPDATE("pending_application_channel", "Pending Applications"),
    REVIEW_RECEIVED("review_channel", "Reviews Received"),
    OTHER("default_channel", "Default Notifications")
}
