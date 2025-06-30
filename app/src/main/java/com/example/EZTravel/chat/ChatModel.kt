package com.example.EZTravel.chat

import android.util.Log
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.di.ChatsCollection
import com.example.EZTravel.di.UsersCollection
import com.example.EZTravel.travelPage.Travel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Query
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class Message(
    @get:Exclude
    var id : String = "",
    var text : String = "",
    var author : DocumentReference? = null,
    @get:Exclude
    var authorUsername : String? = null,
    @get:Exclude
    var firstRender : Boolean = true,
    @set:PropertyName("timestamp")
    @get:PropertyName("timestamp")
    var timestamp : Timestamp? = null,
)

@IgnoreExtraProperties
data class Chat(
    @get:Exclude
    var id: String = "",
    var title: String = "",
    var photo: String = "",
    @set:PropertyName("last_users_access")
    @get:PropertyName("last_users_access")
    var lastUsersAccess: Map<String,Timestamp?> = emptyMap(),
    var travel: DocumentReference? = null,
    @get:Exclude
    var messages: List<Message> = emptyList()
)

data class PaginatedResult(
    val messages: List<Message>,
    val lastVisible: DocumentSnapshot?
)

class ChatModel @Inject constructor(
    @ChatsCollection private val chatsCollection: CollectionReference,
    @UsersCollection private val usersCollection: CollectionReference,
    private val firestore: FirebaseFirestore,
    private val userManager: AuthUserManager
    ) {

    private var messageListeners = mutableMapOf<String, ListenerRegistration>()

    suspend fun getPaginatedMessages(
        chatId: String,
        lastVisible: DocumentSnapshot? = null,
        limit: Long = 50L
    ): PaginatedResult {
        var query = chatsCollection
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        if(lastVisible != null){
            query = query.startAfter(lastVisible)
        }

        val snapshot = query.get().await()

        val messages = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Message::class.java)?.apply { id = doc.id }
        }

        return PaginatedResult(messages, snapshot.documents.lastOrNull())
    }

    fun getAllChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listenerRegistration = chatsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val chatDocs = snapshot.documents
                val chatList = mutableMapOf<String,Chat>()

                // Track asynchronous fetches
                var remaining = chatDocs.size
                if (remaining == 0) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                chatDocs.forEach { doc ->
                    val chat = try {
                        doc.toObject(Chat::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        remaining--
                        if (remaining == 0) trySend(chatList.values.toList().sortedByUnread(userId)).isSuccess
                        return@forEach
                    }

                    if (chat == null) {
                        remaining--
                        if (remaining == 0) trySend(chatList.values.toList().sortedByUnread(userId)).isSuccess
                        return@forEach
                    }

                    chatList[chat.id] = chat

                    if (remaining == 0) {
                        trySend(chatList.values.toList().sortedByUnread(userId)).isSuccess
                    }

                    // Fetch the latest message for this chat
                    val messageListener = chatsCollection.document(chat.id)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { messageSnap,error ->
                            if (error != null) {
                                close(error)
                            }
                            else{
                                Log.d("MESSAGES","Retrieved last message for ${chat.id}")
                                val lastMessage = messageSnap?.documents?.firstOrNull()?.toObject(Message::class.java)
                                if (lastMessage != null) {
                                    val authorRef = lastMessage.author
                                    if(authorRef!=null){
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val authorSnap = authorRef.get().await()
                                                val username = authorSnap.getString("username")
                                                lastMessage.authorUsername = username

                                                val updatedChat = chatList[chat.id]?.copy(messages = listOf(lastMessage))
                                                if (updatedChat != null) {
                                                    chatList[chat.id] = updatedChat
                                                    trySend(chatList.values.toList().sortedByUnread(userId)).isSuccess
                                                }
                                            } catch (e: Exception) {
                                                Log.e("MESSAGES", "Failed to get author", e)
                                            }
                                        }
                                    }
                                }
                            }
                            Log.d("MESSAGES","Remaining: $remaining")
                        }
                    messageListeners[chat.id] = messageListener
                }
            }
        }

        awaitClose {
            listenerRegistration.remove()
            messageListeners.forEach { (_, listener) -> listener.remove() }}
    }

    private fun List<Chat>.sortedByUnread(userId: String): List<Chat> {
        return this.sortedByDescending { chat ->
            val lastMessageTime = chat.messages.lastOrNull()?.timestamp
            val lastUserAccess = chat.lastUsersAccess[userId]
            when {
                lastMessageTime == null -> Timestamp(0, 0)
                lastUserAccess == null -> lastMessageTime
                else -> maxOf(lastMessageTime, lastUserAccess)
            }
        }
    }

    fun getChatById(id: String): Flow<Chat> = callbackFlow {
        var currentChat: Chat? = null
        var currentLastMessage: Message? = null

        var hasChat = false
        var hasLastMessage = false
        var initialized = false

        fun emitIfReady(force: Boolean = false) {
            if ((hasChat && hasLastMessage) || force) {
                currentChat?.let { chat ->
                    trySend(
                        chat.copy(messages = currentLastMessage?.let { listOf(it) } ?: emptyList())
                    ).isSuccess
                }
            }
        }

        val chatListener = chatsCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    currentChat = snapshot.toObject(Chat::class.java)?.apply { this.id = snapshot.id }
                    hasChat = currentChat != null
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasChat && hasLastMessage
                    } else {
                        emitIfReady(force = true)
                    }
                } else if (error != null) {
                    close(error)
                }
            }

        val lastMessageListener = chatsCollection.document(id)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val message = snapshot.documents.firstOrNull()?.toObject(Message::class.java)
                    if (message != null) {
                        val authorRef = message.author
                        if (authorRef != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val authorSnap = authorRef.get().await()
                                    message.authorUsername = authorSnap.getString("username")
                                    currentLastMessage = message
                                    hasLastMessage = true
                                    if (!initialized) {
                                        emitIfReady()
                                        initialized = hasChat && hasLastMessage
                                    } else {
                                        emitIfReady(force = true)
                                    }
                                } catch (e: Exception) {
                                    Log.e("getChatById", "Failed to fetch author", e)
                                }
                            }
                        } else {
                            currentLastMessage = message
                            hasLastMessage = true
                            if (!initialized) {
                                emitIfReady()
                                initialized = hasChat && hasLastMessage
                            } else {
                                emitIfReady(force = true)
                            }
                        }
                    } else {
                        // Non ci sono messaggi, ma consideriamo hasLastMessage true per emettere la chat senza messaggi
                        currentLastMessage = null
                        hasLastMessage = true
                        if (!initialized) {
                            emitIfReady()
                            initialized = hasChat && hasLastMessage
                        } else {
                            emitIfReady(force = true)
                        }
                    }
                } else if (error != null) {
                    close(error)
                }
            }

        awaitClose {
            chatListener.remove()
            lastMessageListener.remove()
        }
    }



    fun getLastMessage(chatId: String): Flow<Pair<String,List<Message>>> = callbackFlow {
        val listenerRegistration = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.mapNotNull { doc ->
                        doc.toObject(Message::class.java).apply{
                            this.id = doc.id
                        }
                    }
                    Log.d("MESSAGES","Retrieved last message for $chatId  -> ${messages}")
                    trySend(Pair(chatId,messages)).isSuccess
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.mapNotNull { doc ->
                        doc.toObject(Message::class.java).apply{
                            this.id = doc.id
                        }
                    }
                    Log.d("MESSAGES","Retrieved last message for $chatId  -> ${messages}")
                    trySend(messages).isSuccess
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }


    suspend fun updateChat(chat : Chat) : Boolean{
        val docRef = chatsCollection.document(chat.id)
        Log.d("CHATS", chat.toString())
        return try{
            firestore.runTransaction { c ->
                c.set(docRef, chat)
            }.await()
            Log.d("TravelModel", "Updated chat ${chat.id}")
            return true
        }
        catch (e: Exception) {
            Log.e("TravelModel", "Error updating chat ${chat.id}", e)
            false
        }
    }

    suspend fun updateLastUserAccess(chatId: String, userId: String,timestamp : Timestamp) : Boolean{
        Log.d("UPDATEACCESS",chatId)
        val docRef = chatsCollection.document(chatId)

        //val timestamp = Timestamp.now()

        return try{
            firestore.runTransaction { c ->
                c.update(docRef, "last_users_access.$userId", timestamp)
            }.await()
            return true
        }
        catch (e: Exception) {
            false
        }
    }

    suspend fun addMessage(chatId: String, message: Message): Boolean {
        val currentUser = userManager.currentUser.value ?: return false
        val userRef = usersCollection.document(currentUser.id)
        val chatRef = chatsCollection.document(chatId)
        val messagesRef = chatRef.collection("messages")
        val timestamp = Timestamp.now()


        return try {
            firestore.runTransaction { t ->
                val newMessageRef = messagesRef.document()
                t.set(newMessageRef, message.copy(author = userRef))
                t.update(chatRef, "last_users_access.${currentUser.id}", timestamp)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("TravelModel","Error posting message : ${e.message}")
            false
        }


    }

    suspend fun postMessage(chat : Chat, message : Message) : Boolean {
         try{
             chatsCollection.document(chat.id)
                 .collection("messages")
                 .add(message)
                 .await()
             return true
        }
        catch(e : Exception){
            Log.e("TravelModel","Error posting message : ${e.message}")
            return false
        }
    }


}
