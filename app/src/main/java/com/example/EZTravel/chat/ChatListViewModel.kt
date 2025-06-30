package com.example.EZTravel.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.userProfile.User
import com.example.EZTravel.userProfile.UserProfileModel
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatModel: ChatModel,
    private val userModel : UserProfileModel,
    userManager: AuthUserManager
): ViewModel() {
    val currentUser: StateFlow<User?> = userManager.currentUser
    val loggedIn: StateFlow<Boolean> = userManager.loggedIn

    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList: StateFlow<List<Chat>> = _chatList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            currentUser.filterNotNull()
                .flatMapLatest { user ->
                    _isLoading.value = true

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
                    _chatList.value = chatList
                    _isLoading.value = false
                }
        }
    }


    /*
    suspend fun markChatAsRead(chatId : String){
        if(currentUser.value != null){
            chatModel.updateLastUserAccess(chatId, currentUser.value!!.id)
        }
    }

     */

    fun markAllChatsAsRead() {
        val chatsToUpdate = _chatList.value.filter { chat ->
            val lastMessageTimestamp = chat.messages.lastOrNull()?.timestamp
            val lastAccess = chat.lastUsersAccess[currentUser.value?.id]
            lastMessageTimestamp != null && (lastAccess == null || lastMessageTimestamp > lastAccess)
        }

        viewModelScope.launch {
            val updatedChats = _chatList.value.toMutableList()
            chatsToUpdate.forEach { chat ->
                try {
                    if (currentUser.value?.id?.let { chatModel.updateLastUserAccess(chat.id, it, Timestamp.now()) } == true) {
                        val updatedChat = chat.copy(
                            lastUsersAccess = chat.lastUsersAccess.toMutableMap().apply {
                                put(currentUser.value!!.id, Timestamp.now())
                            }
                        )
                        val pos = updatedChats.indexOfFirst { it.id == chat.id }
                        if (pos != -1) updatedChats[pos] = updatedChat
                    }
                } catch (e: Exception) {
                    Log.e("ChatListViewModel", "Failed to update last access for chat ${chat.id}: ${e.message}")
                }
            }
            _chatList.value = updatedChats
        }
    }
}