package com.example.EZTravel.travelPage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import com.example.EZTravel.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.ManualMinusPlusCounter
import com.example.EZTravel.ProfilePicture
import com.example.EZTravel.userProfile.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale


@Composable
fun TravelView(
    onBack: () -> Unit,
    onApplications: (String) -> Unit,
    onBuddies: (String, Int) -> Unit,
    onClone: (String) -> Unit,
    onEdit: (String) -> Unit,
    onPhotos: (String) -> Unit,
    onOtherUser: (String) -> Unit,
    onReview: (String) -> Unit,
    onLogin: () -> Unit,
    onSnack: (String) -> Job,
    vm: TravelViewModel = hiltViewModel()
) {
    val travel by vm.travel.collectAsState()

    val owner by vm.owner.collectAsState()
    val showModal = rememberSaveable { mutableStateOf(false) }
    val showDialog = rememberSaveable { mutableStateOf(false) }
    val currentUser by vm.currentUser.collectAsState()
    val isFavorite by vm.isFavorite.collectAsState()
    val listOfReviews by vm.listOfReviews.collectAsState()

    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomBar(
                showModal,
                showDialog,
                travel,
                owner,
                currentUser,
                onApplications,
                onPhotos,
                onReview,
                onLogin
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            CarouselForTravelView(
                images = travel.images,
                onBack = onBack,
                onAdd = { },
                onRemove = { },
                onClone = { onClone(travel.id) },
                editMode = false,
                currentUser = currentUser,
                onLogin = onLogin
            )
            HeaderSection(
                travel,
                owner,
                currentUser,
                owner.id == currentUser?.id,
                isFavorite,
                onEdit,
                onOtherUser
            )
            Spacer(modifier = Modifier.height(24.dp))
            TravelInfo(travel)
            Spacer(modifier = Modifier.height(24.dp))
            Itinerary(travel)
            Spacer(modifier = Modifier.height(24.dp))
            if (travel.dateStart.after(Date())) {
                ApplicationStatusSection(travel, owner, currentUser)
            }
            if (travel.dateStart.before(Date()) && travel.reviews.any { it.stars != 0 }) {
                Reviews(travel, onOtherUser, listOfReviews)
            }
        }
    }
    if (showModal.value) {
        ApplyBottomSheet(
            onConfirm = { n ->
                if (n == 1) {
                    scope.launch {
                      if(!vm.addNewApplications(
                            Application(
                                "",
                                null,
                                State.PENDING.ordinal,
                                n,
                                listOf()
                            )
                        )){
                          onSnack("Problems on adding new application")
                      }
                    }
                } else if (travel.size - vm.getTotalSize() > n - 1) {
                    onBuddies(travel.id, n)
                }
                else if (travel.size - vm.getTotalSize() <= n-1){
                    onSnack("There aren't enough spots")
                }
                showModal.value = false
            },
            onDismiss = {
                showModal.value = false
            }
        )
    }
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Delete confirmation") },
            text = { Text("Are you sure to delete your Application?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = vm.deleteApplication()
                        if (!success) onSnack("Problems on deleting your application")
                    }
                    showDialog.value = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BottomBar(
    showModal: MutableState<Boolean>,
    showDialog: MutableState<Boolean>,
    travel: Travel,
    owner: User,
    currentUser: User?,
    onApplications: (String) -> Unit,
    onPhotos: (String) -> Unit,
    onReview: (String) -> Unit,
    onLogin: () -> Unit,
    vm: TravelViewModel = hiltViewModel()
) {
    val isApplied by vm.isApplied.collectAsState()
    val isRejected by vm.isRejected.collectAsState()

    BottomAppBar(
        containerColor = AppBarColors.BottomBarContainerColor(),
        contentColor = AppBarColors.BottomBarContentColor(),
        tonalElevation = AppBarColors.BottomBarTonalElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (travel.dateStart.after(Date())) {
                Button(
                    onClick = {
                        if (owner.id == currentUser?.id)
                            onApplications(travel.id)
                        else {
                            if (!isApplied && travel.size - vm.getTotalSize() > 0) {
                                if (currentUser != null) showModal.value = true
                                else onLogin()
                            } else if (isApplied) showDialog.value = true
                        }
                    },
                    enabled = if (owner.id == currentUser?.id) true else if (isApplied) {
                        !isRejected
                    } else if (travel.size - vm.getTotalSize() <= 0) false else true,
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isApplied || owner.id == currentUser?.id) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .size(18.dp),
                        imageVector =
                            if (owner.id == currentUser?.id) ImageVector.vectorResource(
                                R.drawable.ic_group
                            )
                            else {
                                if (!isApplied) ImageVector.vectorResource(R.drawable.ic_check_small)
                                else ImageVector.vectorResource(
                                    R.drawable.ic_close
                                )
                            },
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(end = 24.dp),
                        text =
                            if (owner.id == currentUser?.id) stringResource(
                                R.string.button_applications
                            )
                            else {
                                if (!isApplied) stringResource(R.string.button_apply)
                                else stringResource(R.string.button_cancel)
                            }
                    )
                }
            } else {
                Button(
                    onClick = {
                        onPhotos(travel.id)
                    },
                    modifier = if (vm.canReview(currentUser?.id) || owner.id == currentUser?.id) {
                        Modifier
                            .padding(8.dp)
                            .weight(1f)
                    } else Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_photos),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null
                    )
                    Text(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        text = stringResource(R.string.travel_page_photos)
                    )
                }

                if (vm.canReview(currentUser?.id) || owner.id == currentUser?.id) {
                    Button(
                        onClick = {
                            onReview(travel.id)
                        },
                        enabled = !vm.alreadyReviewed(currentUser?.id),
                        modifier = Modifier
                            .padding(8.dp)
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_mail),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.travel_page_review)
                        )
                    }
                }
            }
        }

    }
}

//Header
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeaderSection(
    travel: Travel,
    owner: User,
    currentUser: User?,
    isOwner: Boolean,
    isFavorite: Boolean,
    onEdit: (String) -> Unit,
    onOtherUser: (String) -> Unit,
    vm: TravelViewModel = hiltViewModel(),
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = travel.title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.weight(1f)
            )

            if (currentUser != null) {
                if (!isOwner) {
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            imageVector = if (!isFavorite)
                                ImageVector.vectorResource(R.drawable.ic_favorite)
                            else
                                ImageVector.vectorResource(R.drawable.ic_favorite_filled),
                            contentDescription = if (!isFavorite) "Mark as Favorite" else "Unmark as Favorite",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (travel.dateStart.after(Date())) {
                    IconButton(onClick = { onEdit(travel.id) }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled),
                            contentDescription = "Edit Travel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            travel.highlights.forEach { highlight ->
                AssistChip(
                    onClick = {}, // Non-interactive
                    label = { Text(stringResource(id = Highlights.entries[highlight].tag)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = Highlights.entries[highlight].icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = travel.description,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfilePicture(
                fullName = owner.fullName,
                profilePictureURI = owner.profilePicture?.toUri(),
                pxl = 32
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.travel_page_maker),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = owner.fullName,
                style = MaterialTheme.typography.labelMedium,
                color = if (owner.id != currentUser?.id)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = if (owner.id != currentUser?.id) TextDecoration.Underline else null,
                modifier = if (owner.id != currentUser?.id) Modifier.clickable {
                    onOtherUser(owner.id)
                } else Modifier
            )
        }
    }
}

@Composable
fun TravelInfo(travel: Travel) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {

        Text(
            text = stringResource(R.string.travel_page_infos),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        IconTextRow(
            text = stringResource(
                R.string.travel_page_days_from,
                travel.days,
                dateFormatter.format(travel.dateStart),
                dateFormatter.format(travel.dateEnd)
            ),
            icon = ImageVector.vectorResource(R.drawable.ic_date_range)
        )

        IconTextRow(
            text = if (travel.priceStart == travel.priceEnd) {
                stringResource(R.string.travel_page_price, travel.priceStart.toInt())
            } else {
                stringResource(
                    R.string.travel_page_price_range,
                    travel.priceStart.toInt(),
                    travel.priceEnd.toInt()
                )
            },
            icon = ImageVector.vectorResource(R.drawable.ic_money)
        )

        IconTextRow(
            text = stringResource(
                R.string.travel_page_location,
                travel.location?.name ?: ""
            ),
            icon = ImageVector.vectorResource(R.drawable.ic_location_on_filled)
        )

        IconTextRow(
            text = stringResource(R.string.travel_page_group_people, travel.size),
            icon = ImageVector.vectorResource(R.drawable.ic_group)
        )
    }
}

@Composable
private fun IconTextRow(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ApplicationStatusSection(
    travel: Travel,
    owner: User,
    currentUser: User?,
    vm: TravelViewModel = hiltViewModel()
) {
    val currentUserApplication = travel.applications.find { it.user?.id == currentUser?.id }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {

        Text(
            text = stringResource(R.string.travel_page_application_status),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Show current user's application status (if not the owner)
        if (currentUser?.id != owner.id && currentUserApplication != null) {
            val state = State.entries[currentUserApplication.state]
            val (txt, icon) = when (state) {
                State.PENDING -> Pair(
                    stringResource(R.string.travel_application_pending),
                    ImageVector.vectorResource(R.drawable.ic_hourglass_empty)
                )

                State.ACCEPTED -> Pair(
                    stringResource(R.string.travel_application_accepted),
                    ImageVector.vectorResource(R.drawable.ic_sentiment_excited)
                )

                State.REJECTED -> Pair(
                    stringResource(R.string.travel_application_rejected),
                    ImageVector.vectorResource(R.drawable.ic_sentiment_sad)
                )
            }

            IconTextRow(text = txt, icon = icon)
        }

        // Accepted applications
        IconTextRow(
            text = pluralStringResource(
                R.plurals.travel_page_application_already_accepted,
                vm.getAcceptedApplications(),
                vm.getAcceptedApplications()
            ),
            icon = ImageVector.vectorResource(R.drawable.ic_check_small)
        )

        // Remaining spots
        val remainingSpots = travel.size - vm.getTotalSize()
        IconTextRow(
            text = pluralStringResource(
                R.plurals.travel_page_remaining_spots,
                remainingSpots,
                remainingSpots
            ),
            icon = ImageVector.vectorResource(R.drawable.ic_person_filled)
        )
    }
}

@Composable
fun Itinerary(travel: Travel) {
    val startLocalDate = remember(travel.dateStart) {
        travel.dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val activitiesByDay = remember(travel.itinerary) {
        travel.itinerary.groupBy { activity ->
            val activityDate = activity.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            ChronoUnit.DAYS.between(startLocalDate, activityDate).toInt() + 1
        }.mapValues { (_, activities) ->
            activities.sortedBy { it.timeStart?.toInstant() }
        }.toSortedMap()
    }

    val totalDays = activitiesByDay.keys.maxOrNull() ?: 0

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.travel_page_itinerary),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        activitiesByDay.forEach { (day, activities) ->
            ItineraryItem(day = day, activities = activities, isLastDay = day == totalDays)
        }
    }
}

@Composable
fun ItineraryItem(day: Int, activities: List<Activity>, isLastDay: Boolean) {
    val expanded = rememberSaveable { mutableStateOf(true) }
    val cardHeight = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val circleColor = MaterialTheme.colorScheme.primary

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                drawCircle(color = circleColor)
            }

            if (!isLastDay) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(maxOf(60.dp, cardHeight.value))
                        .background(
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .onSizeChanged {
                    cardHeight.value = with(density) { it.height.toDp() }
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.travel_page_itinerary_day, day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded.value) {
                            ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_up)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_down)
                        },
                        contentDescription = null,
                        modifier = Modifier.clickable { expanded.value = !expanded.value }
                    )
                }

                AnimatedVisibility(visible = expanded.value) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        activities.forEach {
                            ActivityItem(activity = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity) {
    val expanded = rememberSaveable { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = !expanded.value }
        ) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded.value) {
                    ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_up)
                } else {
                    ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_down)
                },
                contentDescription = null
            )
        }

        AnimatedVisibility(
            visible = expanded.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                activity.timeStart?.let { start ->
                    val startTime = formatter.format(start)
                    val endTime = activity.timeEnd?.let { formatter.format(it) } ?: "--"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.travel_page_activity_time_range,
                                startTime,
                                endTime
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_info_filled),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = if (activity.mandatory)
                            stringResource(R.string.activity_mandatory)
                        else
                            stringResource(R.string.activity_optional),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!activity.mandatory && activity.suggestedActivities.isNotBlank()) {
                    Text(
                        text = activity.suggestedActivities,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyBottomSheet(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val count = rememberSaveable { mutableIntStateOf(1) }
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.apply_modal_apply_button),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                        contentDescription = "Close modal",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = stringResource(R.string.apply_modal_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Counter control
            ManualMinusPlusCounter(
                count.intValue,
                onMinusClick = { if (count.intValue > 1) count.intValue-- },
                onPlusClick = { count.intValue++ },
                modifier = Modifier,
                disabledOnZeroValue = true,
                coreIcon = ImageVector.vectorResource(R.drawable.ic_group)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm button
            Button(
                onClick = { onConfirm(count.intValue) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(0.85f),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.apply_modal_confirm_button),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Reviews(
    travel: Travel,
    onOtherUser: (String) -> Unit,
    listOfReviews: List<Pair<User, TravelReview>>
) {
    val avg = travel.reviews.filter{it.stars != 0}.averageOf { it.stars.toDouble() }
    val dstAvg = travel.reviews.filter{it.stars != 0}.averageOf { it.destinationRate.toDouble() }
    val orgAvg = travel.reviews.filter{it.stars != 0}.averageOf { it.organizationRate.toDouble() }
    val assAvg = travel.reviews.filter{it.stars != 0}.averageOf { it.assistanceRate.toDouble() }

    val reviewsWithComments = listOfReviews
        .filter { it.first.id != travel.owner?.id && it.second.description.isNotBlank() }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.travel_page_reviews, listOfReviews.size, travel.size),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = String.format(Locale.US, "%.1f", avg),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics
        ReviewMetricRow(R.string.travel_review_destination, dstAvg)
        ReviewMetricRow(R.string.travel_review_organization, orgAvg)
        ReviewMetricRow(R.string.travel_review_assistance, assAvg)

        Spacer(modifier = Modifier.height(16.dp))

        if (reviewsWithComments.isNotEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.travel_review_comments),
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    itemsIndexed(reviewsWithComments
                    ) { _, review ->
                        ReviewItem(review, onOtherUser)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewMetricRow(@StringRes label: Int, avg: Double) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Text(
            String.format(Locale.US, "%.1f", avg),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private inline fun <T> List<T>.averageOf(selector: (T) -> Double): Double {
    return if (this.isNotEmpty()) this.sumOf(selector) / this.size else 0.0
}


@Composable
fun ReviewItem(review: Pair<User, TravelReview>, onOtherUser: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ReviewDialog(
            review = review,
            onDismiss = { showDialog = false },
            onOtherUser = onOtherUser
        )
    }

    val (user, reviewData) = review
    val hasDescription = reviewData.description.isNotBlank()

    ElevatedCard(
        modifier = Modifier
            .widthIn(min = 240.dp, max = 320.dp)
            .padding(vertical = 8.dp)
            .clickable { showDialog = true },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                RatingStars(reviewData.stars)
            }

            if (hasDescription) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reviewData.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RatingStars(stars: Int, maxStars: Int = 5) {
    Row {
        for (i in 1..maxStars) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (i <= stars) R.drawable.ic_star_filled else R.drawable.ic_empty_star
                ),
                contentDescription = "Star $i",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ReviewDialog(
    review: Pair<User, TravelReview>,
    onDismiss: () -> Unit,
    onOtherUser: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePicture(
                    review.first.fullName,
                    review.first.profilePicture?.toUri(),
                    45
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = review.first.fullName,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable {
                        onDismiss()
                        onOtherUser(review.first.id)
                    }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (i < review.second.stars) R.drawable.ic_star_filled
                                else R.drawable.ic_empty_star
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (review.second.description.isNotBlank()) {
                    Text(
                        text = review.second.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryRatingRow(
                        label = stringResource(R.string.travel_review_destination),
                        rating = review.second.destinationRate
                    )
                    SecondaryRatingRow(
                        label = stringResource(R.string.travel_review_organization),
                        rating = review.second.organizationRate
                    )
                    SecondaryRatingRow(
                        label = stringResource(R.string.travel_review_assistance),
                        rating = review.second.assistanceRate
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SecondaryRatingRow(label: String, rating: Int, maxRating: Int = 5) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Row {
            repeat(maxRating) { i ->
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (i < rating) R.drawable.ic_star_filled else R.drawable.ic_empty_star
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CarouselForTravelView(
    images: List<String>,
    onBack: () -> Unit,
    onAdd: (uri: Uri) -> Unit,
    onRemove: (index: Int) -> Unit,
    onClone: () -> Unit,
    editMode: Boolean,
    currentUser: User?,
    onLogin: () -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    //   Log.i("LIST", combinedList.toString())
    val pagerState = rememberPagerState(pageCount = { images.size })
    //   Log.i("SIZE", combinedList.size.toString())
    val context = LocalContext.current


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->

        uri?.let {
            context.contentResolver?.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onAdd(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        if (images.isEmpty()) {
            AsyncImage(
                model = R.drawable.placeholder_landscape,
                contentDescription = "Placeholder",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledIconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (editMode) {
                if (images.isEmpty()) {
                    FilledIconButton(onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                            contentDescription = "Add Image",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    FilledIconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled),
                            contentDescription = "Edit carousel",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        DropdownMenu(
                            expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Add another image") }, onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                ); expanded = false
                            })
                            DropdownMenuItem(
                                text = { Text("Remove current image") },
                                onClick = { onRemove(pagerState.currentPage);expanded = false })

                        }
                    }
                }
            } else {
                FilledIconButton(onClick = {
                    if (currentUser == null) {
                        onLogin()
                    } else onClone()
                }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_duplicate),
                        contentDescription = "Duplicate",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        }
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(images.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.Gray)
                    )
                }
            }
        }
    }
}