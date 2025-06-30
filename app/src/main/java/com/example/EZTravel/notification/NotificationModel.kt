package com.example.EZTravel.notification

import android.util.Log
import com.example.EZTravel.AuthUserManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationModel @Inject constructor(
    private val authUserManager: AuthUserManager
) {

    fun getCurrentUserNotifications(): Flow<Result<List<NotificationData>>> = callbackFlow {
        Log.d("NotificationModel", "Fetching current user notifications.")
        val userRef = authUserManager.currentUserRef.value
        if (userRef == null) {
            trySend(Result.failure(IllegalStateException("User reference is null")))
            close()
            return@callbackFlow
        }


        val registration = userRef.collection("notifications")
            .orderBy("timestamp",Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationModel", "Error fetching notifications: ${error.message}")
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val notification = doc.toObject(NotificationData::class.java)
                        notification?.id = doc.id
                        notification
                    } catch (e: Exception) {
                        Log.e("NotificationModel", "Error parsing document: ${e.message}")
                        null
                    }
                } ?: emptyList()

                Log.d("NotificationModel", "Parsed ${notifications.size} notifications.")
                trySend(Result.success(notifications))
            }

        awaitClose { registration.remove() }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        val userRef = authUserManager.currentUserRef.value
        if (userRef == null) {
            Log.e("NotificationModel", "User reference is null, cannot update notification.")
            return Result.failure(IllegalStateException("User reference is null"))
        }

        return runCatching {
            userRef.collection("notifications")
                .document(notificationId)
                .update("is_read", true)
                .await()
            Log.i("NotificationModel", "Notification $notificationId marked as read.")
            Unit
        }.onFailure {
            Log.e("NotificationModel", "Failed to mark notification as read: ${it.message}")
        }
    }

    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        val userRef = authUserManager.currentUserRef.value
        if (userRef == null) {
            Log.e("NotificationModel", "User reference is null, cannot update notifications.")
            return Result.failure(IllegalStateException("User reference is null"))
        }

        return runCatching {
            val snapshot = userRef.collection("notifications")
                .whereEqualTo("is_read", false)
                .get()
                .await()

            val batch = userRef.firestore.batch()

            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "is_read", true)
            }

            batch.commit().await()
            Log.i("NotificationModel", "Marked ${snapshot.size()} notifications as read.")
            Unit
        }.onFailure {
            Log.e("NotificationModel", "Failed to mark all as read: ${it.message}")
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val userRef = authUserManager.currentUserRef.value
        if (userRef == null) {
            Log.e("NotificationModel", "User reference is null, cannot delete notification.")
            return Result.failure(IllegalStateException("User reference is null"))
        }

        return runCatching {
            userRef.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            Log.i("NotificationModel", "Deleted notification $notificationId.")
            Unit
        }.onFailure {
            Log.e("NotificationModel", "Failed to delete notification: ${it.message}")
        }
    }

    suspend fun registerToken(token: String): Result<Unit> {
        val currentUser = authUserManager.currentUserRef.value
            ?: return Result.failure(IllegalStateException("User is not logged in or user reference is null"))

        return runCatching {
            currentUser.set(mapOf("fcmToken" to token), SetOptions.merge()).await()
            Log.i("NotificationModel", "FCM token registered successfully.")
            Unit
        }.onFailure {
            Log.e("NotificationModel", "Failed to register FCM token: ${it.message}")
        }

    }

    suspend fun getToken(): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("FCM token is blank"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}