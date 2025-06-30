package com.example.EZTravel.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.R
import com.example.EZTravel.getMonogram
import com.example.EZTravel.mergePadding
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatListView(
    vm: ChatListViewModel = hiltViewModel(),
    outerPadding: PaddingValues = PaddingValues(),
    onChatClick: (String) -> Unit
) {
    val chatList by vm.chatList.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val currentUser by vm.currentUser.collectAsState()

    Scaffold (
        topBar = {
            TopBar(
                onMarkAllAsRead = { vm.markAllChatsAsRead() }
            )
        }
    ){ padding ->
        val mergedPadding = mergePadding(padding, outerPadding)

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(mergedPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(mergedPadding)
            ) {
                items(chatList) { chat ->
                    ChatListItem(
                        chat = chat,
                        currentUserId = currentUser?.id.orEmpty(),
                        onClick = {
                            onChatClick(chat.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onMarkAllAsRead: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.chats_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            IconButton(onClick = onMarkAllAsRead) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_mark_chat_read),
                    contentDescription = "Mark all as read"
                )
            }
        },
        colors = AppBarColors.TopAppBarColor()
    )
}

@Composable
fun ChatListItem(
    chat: Chat,
    currentUserId: String,
    onClick: () -> Unit
) {
    val lastMsg = chat.messages.lastOrNull()
    val lastMsgTime = lastMsg?.timestamp?.toDate()
    val lastAccess = chat.lastUsersAccess[currentUserId]?.toDate()

    val hasUnread = lastMsgTime != null && (lastAccess == null || lastMsgTime.after(lastAccess))

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                ChatAvatar(photoUrl = chat.photo, fallbackText = chat.title.getMonogram())
            },
            supportingContent = {

                if (lastMsg != null) {
                    Column{
                        Text(
                            text = lastMsg.authorUsername?:"Unnamed",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = lastMsg.text.take(40),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                }
            },
            trailingContent = {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = lastMsg?.timestamp.toDisplayTime(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Light
                        )
                    )
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp)) // Keeps spacing consistent if no badge
                    }
                }
            }
        )
    }
}

fun Timestamp?.toDisplayTime(): String {
    if (this == null) return ""

    val dateTime = this.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    val today = LocalDate.now()

    return if (dateTime.toLocalDate() == today) {
        dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        dateTime.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
}

@Composable
fun ChatAvatar(photoUrl: String, fallbackText: String) {
    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}