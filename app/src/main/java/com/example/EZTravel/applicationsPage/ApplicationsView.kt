package com.example.EZTravel.applicationsPage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.ProfilePicture
import com.example.EZTravel.R
import com.example.EZTravel.getMonogram
import com.example.EZTravel.travelPage.Application
import com.example.EZTravel.travelPage.State
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.userProfile.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ApplicationsView(
    onBack: () -> Unit,
    onUser: (String) -> Unit,
    onSnack: (String) -> Job,
    vm: ApplicationsViewModel = hiltViewModel()
) {
    val travel by vm.travel.collectAsState()
    val hasModified by vm.hasModified.collectAsState()
    val applicationList by vm.listOfApplications.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val isLoadingContent by vm.isLoadingContent.collectAsState()
    val isLoadingChanges by vm.isLoadingChanges.collectAsState()
    if (isLoadingChanges) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            contentWindowInsets = contentWindowInsets,
            topBar = {
                TopAppBar(onBack)
            },
            bottomBar = {
                BottomBar(setShowDialog = { showDialog = it}, hasModified, vm)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoadingContent) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (applicationList.isEmpty()) {
                        Text(
                            text = stringResource(R.string.applications_page_no_applications),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp, start = 8.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.applications_page_pending),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp, start = 8.dp)
                        )
                        Row {
                            ApplicationsList(
                                applicationList.filter { it.second.state == State.PENDING.ordinal },
                                travel,
                                State.PENDING.ordinal,
                                onUser,
                                onSnack
                            )
                        }
                        Text(
                            text = stringResource(R.string.applications_page_accepted),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp, start = 8.dp)
                        )
                        Row {
                            ApplicationsList(
                                applicationList.filter { it.second.state == State.ACCEPTED.ordinal },
                                travel,
                                State.ACCEPTED.ordinal,
                                onUser,
                                onSnack
                            )
                        }
                        Text(
                            text = stringResource(R.string.applications_page_rejected),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp, start = 8.dp)
                        )
                        Row {
                            ApplicationsList(
                                applicationList.filter { it.second.state == State.REJECTED.ordinal },
                                travel,
                                State.REJECTED.ordinal,
                                onUser,
                                onSnack
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            ConfirmSaveDialog(
                onConfirm = {
                    showDialog = false
                    coroutineScope.launch {
                        vm.toggleIsLoading()
                        val success = vm.saveChanges()
                        if (success) {
                            onSnack("Applications modified successfully")
                            onBack()
                        } else {
                            onSnack("Error on modifying applications or too many accepted applications spots")
                            vm.toggleIsLoading()
                        }
                    }
                },
                onDismiss = {
                    vm.cancel()
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun BottomBar(setShowDialog: (Boolean) -> Unit, hasModified: Boolean, vm: ApplicationsViewModel) {
    BottomAppBar(
        containerColor = AppBarColors.BottomBarContainerColor(),
        contentColor = AppBarColors.BottomBarContentColor(),
        tonalElevation = AppBarColors.BottomBarTonalElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = { vm.cancel() },
                    enabled = hasModified
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.button_cancel))
                }

                Button(
                    onClick = { setShowDialog(true) },
                    enabled = hasModified,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check_small),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.button_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    onBack: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.applications_page_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back"
                )
            }
        },
        colors = AppBarColors.TopAppBarColor()
    )
}

@Composable
fun ApplicationsList(
    applications: List<Pair<User, Application>>,
    travel: Travel,
    type: Int,
    onUser: (String) -> Unit,
    onSnack: (String) -> Job
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (applications.isNotEmpty()) {
            applications.forEach { application ->
                val state = travel.applications.find { application.second.id == it.id }?.state ?: 0
                ApplicationItem(type, application, state, onUser, onSnack)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApplicationItem(
    type: Int,
    application: Pair<User, Application>,
    previousState: Int,
    onUser: (String) -> Unit,
    onSnack: (String) -> Job,
    vm: ApplicationsViewModel = hiltViewModel()
) {
    val expanded = rememberSaveable { mutableStateOf(false) }
    val hasBuddies = application.second.buddies.isNotEmpty()

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { onUser(application.first.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {

                ProfilePicture(
                    fullName = application.first.fullName,
                    profilePictureURI = application.first.profilePicture?.toUri(),
                    pxl = 40
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = application.first.fullName,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    modifier = Modifier.weight(1f)
                )

                if (type == State.PENDING.ordinal) {
                    IconButton(
                        onClick = {
                            if (!vm.updateApplicationState(
                                    application.second,
                                    State.ACCEPTED.ordinal
                                )
                            ) {
                                onSnack("There aren't enough spots")
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                            contentDescription = "Accept",
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    IconButton(
                        onClick = {
                            vm.updateApplicationState(
                                application.second,
                                State.REJECTED.ordinal
                            )
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                            contentDescription = "Reject",
                            tint = Color(0xFFF44336)
                        )
                    }

                } else if (previousState == State.PENDING.ordinal)  {
                    IconButton(
                        onClick = {
                            vm.updateApplicationState(application.second, State.PENDING.ordinal)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_undo),
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (hasBuddies) {
                Column(modifier = Modifier.padding(top = 4.dp, start = 52.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded.value = !expanded.value
                            },
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.applications_page_show_buddies,
                                application.second.buddies.size
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (expanded.value)
                                    R.drawable.ic_keyboard_arrow_up
                                else
                                    R.drawable.ic_keyboard_arrow_down
                            ),
                            contentDescription = "See additional buddies",
                        )
                    }

                    AnimatedVisibility(
                        visible = expanded.value,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            application.second.buddies.forEachIndexed { idx, buddy ->
                                Text(
                                    text = buddy,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                if (idx != application.second.buddies.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            end = 8.dp,
                                            start = 8.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmSaveDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text("Are you sure?")
        },
        text = {
            Text("Once you save, your choices cannot be changed. Are you sure you want to proceed?")
        },
//        shape = RoundedCornerShape(16.dp)
    )
}