package com.example.EZTravel.chat


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.ProfilePicture
import com.example.EZTravel.R
import com.example.EZTravel.userProfile.User
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Preview(showBackground = true)
@Composable
fun ChatMessagePreview() {
    val exampleMessage = Message(
        id = "1",
        text = "Hello, how are you?",
        timestamp = Timestamp(Date())
    )
    val exampleUser = User(
        id = "1",
        fullName = "John Doe",
        profilePicture = null
    )

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        ChatMessage(
            message = exampleMessage,
            author = exampleUser,
            isCurrentUser = false,
            modifier = Modifier.fillMaxWidth(),
            updateLastUserAccess = {},
            scope = scope
        )
        Spacer(modifier = Modifier.height(16.dp))
        ChatMessage(
            message = exampleMessage.copy(text = "I'm doing well, thank you!"),
            author = exampleUser,
            isCurrentUser = true,
            modifier = Modifier.fillMaxWidth(),
            updateLastUserAccess = {},
            scope = scope
        )
    }
}

@Preview
@Composable
fun UserInputPreview() {
    UserInput(onMessageSent = {})
}

@Preview
@Composable
fun DayHeaderPreview() {
    DayHeader("Today")
}

@Preview(showBackground = true)
@Composable
fun UnreadMessagesBannerPreview() {
    UnreadMessagesBanner(modifier = Modifier.padding(16.dp))
}

@Preview
@Composable
fun ChatAppBarPreview() {
    ChatAppBar(
        title = "Chat title",
        memberCount = 3
    ) { }
}

@Composable
fun DayHeader(dayString: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(16.dp)
    ) {
        DayHeaderLine()
        Text(
            text = dayString,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DayHeaderLine()
    }
}


@Composable
private fun RowScope.DayHeaderLine() {
    HorizontalDivider(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

fun Date.toLocalDateString(): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    onBack: () -> Unit,
    onSnack: (String) -> Job,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val chat by viewModel.chat.collectAsState()
    val messages by viewModel.messages.collectAsState()

    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserAccess by viewModel.currentUserAccess.collectAsState()

    var shouldScroll by remember { mutableStateOf(false) }
    var shouldShowUnreadBanner by remember { mutableStateOf(true) }

    val indexUnreadMessages by remember(messages, currentUserAccess) {
        derivedStateOf {
            currentUserAccess?.let { lastAccess ->

                messages.indexOfFirst {
                    val messageTimestamp = it.timestamp
                    messageTimestamp != null && messageTimestamp > lastAccess
                }
                    .takeIf {
                        it <= (messages.size - 1)
                    } ?: (messages.size - 1)

            } ?: (messages.size - 1)
        }
    }


    // Raggruppa i messaggi per data
    val groupedMessages = messages.groupBy { message ->
        message.timestamp?.toDate()?.toLocalDateString() ?: "Unknown Date"
    }
    val globalIndexMap by remember(messages) {
        derivedStateOf {
            messages.mapIndexed { index, message -> message to index }.toMap()
        }
    }

    LaunchedEffect(messages) {
        if (shouldScroll)
            scrollState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(indexUnreadMessages) {
        indexUnreadMessages.let {
            if (it != -1) {
                scrollState.animateScrollToItem(it)
                shouldScroll = true
            } else {
                val size = messages.size
                if (size > 0) {
                    scrollState.animateScrollToItem(size - 1)
                    shouldScroll = true
                    shouldShowUnreadBanner = false
                }

            }
        }
    }

    Scaffold(
        topBar = {
            ChatAppBar(
                title = chat.title,
                memberCount = chat.lastUsersAccess.keys.size,
                onBack = onBack
            )
        },
        bottomBar = {
            BottomBar(scope, { shouldScroll = it },onSnack)
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                groupedMessages.forEach { (date, messagesForDate) ->
                    item {
                        DayHeader(dayString = date)
                    }

                    items(messagesForDate.size) { mId ->
                        val message = messagesForDate[mId]
                        val author = users[message.author?.id ?: ""]
                        val isCurrentUser = currentUser?.id == message.author?.id


                        val globalIndex = globalIndexMap[message]
                        val isUnreadBanner = globalIndex == indexUnreadMessages
                        Log.d("INDEX-CHAT", "id: ${indexUnreadMessages}, item: $globalIndex")
                        if (isUnreadBanner && !isCurrentUser && shouldShowUnreadBanner) {
                            UnreadMessagesBanner()
                        }

                        ChatMessage(
                            message = message,
                            author = author,
                            isCurrentUser = isCurrentUser,
                            updateLastUserAccess = viewModel::updateLastUserAccess,
                            scope = scope
                        )
                    }
                }


            }
        }
    }
}

@Composable
fun UserInput(
    onMessageSent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var focusState by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 2.dp,
        contentColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .padding(8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                // TextField
                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { state -> focusState = state.isFocused },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions {
                        if (textState.text.isNotBlank()) {
                            onMessageSent(textState.text)
                            textState = TextFieldValue() // Reset text
                        }
                    }
                )

                // Placeholder
                if (textState.text.isEmpty() && !focusState) {
                    Text(
                        text = "Type a message",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }

            IconButton(
                onClick = {
                    if (textState.text.isNotBlank()) {
                        onMessageSent(textState.text)
                        textState = TextFieldValue() // Reset text
                    }
                },
                enabled = textState.text.isNotBlank()
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_send),
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun BottomBar(scope: CoroutineScope, setShouldScroll: (Boolean) -> Unit, onSnack: (String) -> Job, viewModel: ChatViewModel = hiltViewModel()) {
    BottomAppBar(
        containerColor = AppBarColors.BottomBarContainerColor(),
        contentColor = AppBarColors.BottomBarContentColor(),
        tonalElevation = AppBarColors.BottomBarTonalElevation,
        modifier = Modifier.fillMaxWidth().imePadding()
    ) {
        UserInput(
            onMessageSent = { text ->
                scope.launch {
                    if (viewModel.sendMessage(text)) {
                        setShouldScroll(true)
                    } else {
                        onSnack("Error sending the message")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars)
                ) // Padding per gestire la tastiera
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppBar(
    title: String,
    memberCount: Int,
    onBack: () -> Unit
) {

    TopAppBar(
        title = {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = pluralStringResource(R.plurals.chat_view_members, memberCount, memberCount),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                    )
                }
            }
        },
        colors = AppBarColors.TopAppBarColor()
    )
}

fun Date.toTimeString(): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(this)
}

@Composable
fun ChatMessage(
    message: Message,
    author: User?,
    isCurrentUser: Boolean,
    scope: CoroutineScope,
    updateLastUserAccess: suspend (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        scope.launch {
            updateLastUserAccess(message)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            ProfilePicture(
                fullName = author?.fullName ?: "Unknown",
                profilePictureURI = author?.profilePicture?.toUri(),
                pxl = 40
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (!isCurrentUser) {
                Text(
                    text = author?.fullName ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = message.timestamp?.toDate()?.toTimeString() ?: "Unknown Time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun UnreadMessagesBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Unread messages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
