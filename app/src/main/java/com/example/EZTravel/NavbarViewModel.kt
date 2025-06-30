package com.example.EZTravel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.chat.Chat
import com.example.EZTravel.chat.ChatModel
import com.example.EZTravel.notification.NotificationData
import com.example.EZTravel.notification.NotificationModel
import com.example.EZTravel.userProfile.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NavbarViewModel @Inject constructor(
    //private val userProfileModel: UserProfileModel,
    private val notificationModel: NotificationModel,
    private val firestore: FirebaseFirestore,
    private val chatModel: ChatModel,
    userManager: AuthUserManager
) : ViewModel() {

    val currentUser: StateFlow<User?> = userManager.currentUser

    private val _unreadChats = MutableStateFlow<Int>(0)
    val unreadChats: StateFlow<Int> = _unreadChats

    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications

    private val _nNotifications = MutableStateFlow(0)
    val nNotifications: StateFlow<Int> = _nNotifications

    val _chats = MutableStateFlow<List<Chat>>(emptyList())


    init {
        viewModelScope.launch {
            currentUser
                .flatMapLatest {user ->
                    if (user != null) {
                        notificationModel.getCurrentUserNotifications()
                    } else {
                        flowOf(Result.success(emptyList())) // Nessuna notifica se l'utente è vuoto o nullo
                    }}
                .collectLatest { result ->
                    result.onSuccess { notifications ->
                        _notifications.value = notifications
                        _nNotifications.value = notifications.count { !it.isRead }
                    }.onFailure {
                        _notifications.value = emptyList()
                        _nNotifications.value = 0
                    }
                }
        }

        viewModelScope.launch {
            currentUser.filterNotNull()
                .flatMapLatest { user ->

                    val chatFlows = user.chatRefs.map { ref ->
                        chatModel.getChatById(ref.id)
                    }

                    if (chatFlows.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(chatFlows) { chatArray ->
                            chatArray.toList().sortedByDescending { chat ->
                                val lastMessage = chat.messages.lastOrNull()?.timestamp
                                val lastAccess = chat.lastUsersAccess[user.id]
                                when {
                                    lastMessage == null -> Timestamp(0, 0)
                                    lastAccess == null -> lastMessage
                                    else -> maxOf(lastMessage, lastAccess)
                                }
                            }
                        }
                    }
                }
                .collectLatest { chatList ->
                    _chats.value = chatList
                    _unreadChats.value = 0
                    _chats.value.forEach{
                        Log.d("navbar", "Received ${it.title}")
                        if(it.lastUsersAccess[currentUser.value!!.id] == null){
                            _unreadChats.value += 1
                        }
                        else if(it.messages.lastOrNull()?.timestamp != null && it.lastUsersAccess[currentUser.value!!.id]!! < it.messages.lastOrNull()?.timestamp!!){
                            _unreadChats.value += 1
                        }
                    }
                }
        }


    }
}