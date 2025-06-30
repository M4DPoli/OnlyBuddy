package com.example.EZTravel.notification.page

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.notification.NotificationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class NotificationPageViewModel @Inject constructor(
    private val notificationModel: NotificationModel
) : ViewModel() {

    val notificationList = notificationModel.getCurrentUserNotifications().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        Result.success(emptyList())
    )

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationModel.markNotificationAsRead(notificationId)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error marking notification as read: ${e.message}")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                notificationModel.markAllNotificationsAsRead()
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error marking all as read: ${e.message}")
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationModel.deleteNotification(notificationId)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error deleting notification: ${e.message}")
            }
        }
    }

}