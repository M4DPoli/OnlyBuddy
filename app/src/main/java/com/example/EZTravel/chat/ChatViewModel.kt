package com.example.EZTravel.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.userProfile.User
import com.example.EZTravel.userProfile.UserProfileModel
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatModel: ChatModel,
    private val userModel: UserProfileModel,
    userManager: AuthUserManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val chatId: String = checkNotNull(savedStateHandle[EZTravelDestinationsArgs.CHAT_ID])

    val currentUser: StateFlow<User?> = userManager.currentUser

    private val _chat: MutableStateFlow<Chat> = MutableStateFlow(Chat())
    val chat: StateFlow<Chat> = _chat

    val currentUserAccess: StateFlow<Timestamp?> = combine(
        currentUser,
        _chat
    ) { user, chatData ->
        user?.let { chatData.lastUsersAccess[it.id] }
    }.stateIn(
        viewModelScope, // scope del viewmodel
        SharingStarted.WhileSubscribed(5000), // modalità di condivisione
        null // valore iniziale
    )

    private val _users: MutableStateFlow<Map<String, User>> = MutableStateFlow(emptyMap())
    val users: StateFlow<Map<String, User>> = _users

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages


    init {
        viewModelScope.launch(Dispatchers.IO) {
            chatModel.getChatById(chatId).collect {
                _chat.value = it
                val userFlows = it.lastUsersAccess.keys.map { userId ->
                    userModel.getUserById(userId).map { user ->
                        userId to user
                    }
                }

                combine(userFlows) { userPairs ->
                    userPairs.toMap()
                }.collect { usersMap ->
                    _users.value = usersMap
                }
            }
        }


        //TODO: Inserire pagination?
        viewModelScope.launch(Dispatchers.IO) {
            chatModel.getMessages(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    suspend fun updateLastUserAccess(message: Message) {
        Log.d("UPDATING LAST", chatId)
        if (message.timestamp != null) {
            val lastAccess = chat.value.lastUsersAccess[currentUser.value!!.id]
            lastAccess?.let {
                if (it < message.timestamp!!)
                    chatModel.updateLastUserAccess(
                        chatId,
                        currentUser.value!!.id,
                        message.timestamp!!
                    )
            }

        }
    }


    suspend fun sendMessage(messageText: String): Boolean {
        return withContext(Dispatchers.IO) {
            val message = Message(
                text = messageText,
                timestamp = Timestamp.now()
            )
            chatModel.addMessage(_chat.value.id, message)
        }
    }


}


//    val _messages = MutableStateFlow<List<Message>>(emptyList())
//    val messages: StateFlow<List<Message>> = _messages
//
//    private var lastDocument: DocumentSnapshot? = null
//    private var endReached = false
//
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading: StateFlow<Boolean> = _isLoading
//
//
//    //TODO: Questo non serve una bega. Chat da caricare quando scarico user?
//    @OptIn(ExperimentalCoroutinesApi::class)
//    private suspend fun loadUserChat() {
//        currentUser
//            .flatMapLatest { u ->
//                if (u == null) {
//                    flowOf(emptyList())
//                } else {
//                    val chatIds = u.chatRefs
//                    if (chatIds.isEmpty()) {
//                        flowOf(emptyList<Chat>())
//                    } else {
//                        val chatFlows = chatIds.map { id ->
//                            chatModel.getChatById(id.id) // assuming this returns Flow<Chat>
//                        }
//                        combine(chatFlows) { chatsArray ->
//                            chatsArray.toList()
//                        }
//                    }
//                }
//            }
//            .collect { chats ->
//                val tempMap = mutableMapOf<String, Chat>()
//                chats.forEach {
//                    tempMap[it.id] = it
//                }
//                val _chats
//                _chats.value = tempMap
//            }
//    }
//
//    //TODO: Mi servono info solo sulla mia chat, non su tutte
//    suspend fun updateChat(chat: Chat): Boolean {
//        if (currentUser.value != null) {
//            val tempMap = chat.lastUsersAccess.toMutableMap()
//            tempMap[currentUser.value!!.id] = Timestamp.now()
//            chat.lastUsersAccess = tempMap
//            return chatModel.updateChat(chat)
//        }
//        return false
//    }
//
//    //TODO: Gestire caso success o failure
//    fun postMessage(chat: Chat, message: Message) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val isSuccess = if (currentUser.value != null)
//                chatModel.postMessage(chat, message)
//            else
//                false
//
//        }
//    }
//
//    //TODO: Integrare pagination ma solo quando il resto funziona
//    fun loadMoreMessages(chatId: String) {
//        if (_isLoading.value || endReached) return
//
//        _isLoading.value = true
//
//        viewModelScope.launch {
//            val result = chatModel.getPaginatedMessages(chatId)
//
//
//            if (result.messages.isEmpty()) {
//                endReached = true
//            } else {
//                _messages.update { old ->
//                    (old + result.messages).distinctBy { it.id } }
//                lastDocument = result.lastVisible
//            }
//        }
//        _isLoading.value = false
//    }
//}