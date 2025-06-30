package com.example.EZTravel.notification.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.R
import com.example.EZTravel.notification.NotificationData
import com.example.EZTravel.notification.NotificationType


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NotificationPageView(
    onApplications: (String) -> Unit,
    onHome: () -> Unit,
    onTravel: (String) -> Unit,
    onBack: () -> Unit,
    vm: NotificationPageViewModel = hiltViewModel()
) {
    val notifications by vm.notificationList.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.notifications_page_title))
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = "back"
                        )
                    }
                },
                actions = {
                    if (notifications.getOrNull()?.any { !it.isRead } == true) {
                        IconButton(onClick = { vm.markAllAsRead() }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_mark_chat_read),
                                contentDescription = "Mark all as read"
                            )
                        }
                    }
                },
                colors = AppBarColors.TopAppBarColor()
            )
        }
    ) { innerPadding ->
        notifications.onSuccess { values ->
            if (values.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = values,
                        key = { it.id }
                    ) { notification ->
                        val dismissState = rememberDismissState(
                            confirmStateChange = {
                                if (it == DismissValue.DismissedToStart || it == DismissValue.DismissedToEnd) {
                                    vm.deleteNotification(notification.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                                        contentDescription = "delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            dismissContent = {
                                NotificationItem(
                                    notification = notification,
                                    onNavigate = { type, notificationId, relatedId ->
                                        vm.markAsRead(notificationId)
                                        handleNavigation(type, relatedId, onApplications, onHome, onTravel)
                                    }
                                )
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.notifications_page_no_notifications))
                }
            }
        }.onFailure {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.notifications_page_error))
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationData,
    onNavigate: (NotificationType, String, String?) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            onNavigate(
                NotificationType.entries[notification.type],
                notification.id,
                notification.relatedId
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.CenterVertically)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun handleNavigation(
    type: NotificationType,
    relatedId: String?,
    onApplications: (String) -> Unit,
    onHome: () -> Unit,
    onTravel: (String) -> Unit
) {
    when (type) {
        NotificationType.LAST_MINUTE_PROPOSAL,
        NotificationType.RECOMMENDED_TRIPS,
        NotificationType.PENDING_APPLICATION_UPDATE,
        NotificationType.REVIEW_RECEIVED -> {
            if (!relatedId.isNullOrBlank()) {
                onTravel(relatedId)
            } else {
                onHome() // Fallback
            }
        }

        NotificationType.NEW_APPLICATION -> {
            if (!relatedId.isNullOrBlank()) {
                onApplications(relatedId)
            } else {
                onHome() // Fallback
            }
        }

        NotificationType.OTHER -> onHome()
    }
}