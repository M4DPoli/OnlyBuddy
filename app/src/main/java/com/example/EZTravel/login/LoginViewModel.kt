package com.example.EZTravel.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.notification.NotificationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val notificationModel: NotificationModel,
    authUserManager: AuthUserManager
) : ViewModel() {

    val newUser: StateFlow<Boolean> = authUserManager.newUser
    val loggedIn: StateFlow<Boolean> = authUserManager.loggedIn

    fun saveNotificationToken() {

        viewModelScope.launch(Dispatchers.IO) {
            val token = notificationModel.getToken()

            if (token.isFailure) {
                Log.e(
                    "EZTravelMessagingService",
                    "Error getting FCM token: ${token.exceptionOrNull()?.message}"
                )
                return@launch
            }
            notificationModel.registerToken(token.getOrThrow()).onSuccess {
                Log.d("EZTravelMessagingService", "Token saved in Firestore")
            }.onFailure { error ->
                Log.e("EZTravelMessagingService", "Error saving FCM token: ${error.message}")
            }

        }
    }
}