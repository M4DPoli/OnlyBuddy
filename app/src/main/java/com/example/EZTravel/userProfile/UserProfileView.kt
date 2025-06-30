package com.example.EZTravel.userProfile

import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.HighlightsSection
import com.example.EZTravel.R
import com.example.EZTravel.TravelCardSmallWithClick
import com.example.EZTravel.mergePadding
import com.example.EZTravel.notification.NotificationPermissionsUtils
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.UserReview

@Preview(name = "Pixel 7", device = "id:pixel_7")
@Composable
fun UserProfileViewPreview() {
    UserProfileView(
        onBack = {},
        onTravel = {},
        onEdit = {},
        onNotifications = {},
        onSignOut = {},
        outerPadding = PaddingValues(0.dp)
    )
}

fun String.getMonogram(): String {
    if (this.isBlank()) {
        return ""
    }
    val list = this.trim().split(" ").filter { it.isNotBlank() }
    if (list.size > 1) {
        val res = listOf(list.first(), list.last()).map { it[0].uppercaseChar() }
            .joinToString(separator = "")
        return res
    } else {
        val res = list.first().first().uppercaseChar()
        return res.toString()
    }
}

@Composable
fun UserProfileView(
    onBack: () -> Unit,
    onTravel: (String) -> Unit,
    onEdit: (String) -> Unit,
    onNotifications: () -> Unit,
    onSignOut: () -> Unit,
    outerPadding: PaddingValues = PaddingValues(),
    vm: UserProfileViewModel = hiltViewModel()
) {

    val loggedIn by vm.loggedIn.collectAsState()
    val user by vm.user.collectAsState()
    val isOwner by vm.isOwner.collectAsState()
    val nrPastAccepted by vm.pastAccepted.collectAsState()
    val nrPastCreated by vm.pastCreated.collectAsState()
    val totalTravelsRating by vm.totalTravelsRating.collectAsState()
    val n by vm.nNotifications.collectAsState()
    val buddyReviews by vm.buddyReviews.collectAsState()

    if (!loggedIn && isOwner) {
        onBack()
    }

    val ctx = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!NotificationPermissionsUtils.checkNotificationPermissions(permissions)) {
                    Toast.makeText(
                        ctx,
                        "Permission request denied, go to settings to grant the requested permission",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onNotifications()
                }
            }

        }

    Scaffold(
        topBar = {
            TopAppBar(
                isOwner,
                user.fullName,
                user.id,
                n,
                notificationPermissionLauncher,
                onBack,
                onEdit,
                onNotifications,
                onSignOut
            )
        }) { innerPadding ->
        // Main content
        val mergedPadding = mergePadding(innerPadding, outerPadding)
        UserProfileContent(
            mergedPadding,
            user,
            isOwner,
            onTravel,
            nrPastCreated + nrPastAccepted,
            totalTravelsRating,
            buddyReviews
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    isOwner: Boolean,
    fullName: String,
    userId: String,
    n: Int,
    notificationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>?,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onNotifications: () -> Unit,
    onSignOut: () -> Unit
) {
    val ctx = LocalContext.current

    val title = if (isOwner) {
        stringResource(R.string.profile_page_own_title)
    } else {
        stringResource(R.string.profile_page_others_title, fullName.split(" ").first())
    }

    val openAlertDialog = remember { mutableStateOf(false) }

    if (openAlertDialog.value) {
        LogoutDialog(
            onDismissRequest = { openAlertDialog.value = false },
            onConfirmation = onSignOut
        )
    }


    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (!isOwner) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back"
                    )
                }
            } else {
                IconButton(onClick = {
                    openAlertDialog.value = true
                }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_logout),
                        contentDescription = "Logout"
                    )
                }
            }
        }, actions = {
            if (isOwner) {
                IconButton(onClick = { onEdit(userId) }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_edit_square),
                        contentDescription = "Edit"
                    )
                }
                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (NotificationPermissionsUtils.notificationPermissionsGranted(ctx)) {
                            onNotifications()
                        } else {
                            notificationPermissionLauncher?.launch(
                                NotificationPermissionsUtils.NOTIFICATION_PERMISSIONS
                            )
                        }
                    } else {
                        onNotifications()
                    }

                }) {
                    BadgedBox(
                        badge = {
                            if (n > 0) {
                                Badge {
                                    Text(n.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_notifications),
                            contentDescription = ""
                        )
                    }
                }
            }
        },
        colors = AppBarColors.TopAppBarColor()
    )
}


@Composable
fun UserProfileContent(
    innerPadding: PaddingValues,
    user: User,
    isOwner: Boolean,
    onTravel: (String) -> Unit,
    nrTravels: Int,
    totalTravelsRating: Double,
    buddyReviews: List<Triple<User, Travel, UserReview>>
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (maxWidth < maxHeight) {     // Portrait mode
            // Main column with all the content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)

            ) {
                item {
                    HeaderSection(user, nrTravels, totalTravelsRating)
                }

                item { HorizontalDivider() }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.profile_page_highlights_label),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HighlightsSection(user.highlights)
                    }
                }

                item {
                    BioSection(user.bio)
                }

                item {
                    TravelsSection(
                        isOwner,
                        user.showPastTravels,
                        onTravel
                    )
                }

                item {
                    BuddyReviewsSection(buddyReviews)
                }

                if (isOwner) {
                    item { PersonalInfoSection(user.email, user.phone) }
                }
            }
        } else {        // Landscape mode
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    HeaderSection(user, nrTravels, totalTravelsRating)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(2f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HighlightsSection(user.highlights)
                    }
                    item {
                        BioSection(user.bio)
                    }

                    item {
                        TravelsSection(
                            isOwner,
                            user.showPastTravels,
                            onTravel
                        )
                    }

                    if (user.reviewsAsBuddy.isNotEmpty())
                        item {
                            BuddyReviewsSection(buddyReviews)
                        }

                    if (isOwner) {
                        item {
                            PersonalInfoSection(user.email, user.phone)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    user: User,
    nrTravels: Int,
    totalTravelsRating: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfilePicture(
                fullName = user.fullName,
                profilePictureURI = user.profilePicture?.takeIf { it.isNotBlank() }?.toUri(),
                sizeDp = 80
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.profile_page_username, user.username),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStatistic(
                headline = nrTravels.toString(),
                label = pluralStringResource(
                    id = R.plurals.profile_page_nr_travels_label,
                    count = nrTravels
                )
            )
            ProfileStatistic(
                headline = stringResource(R.string.profile_page_buddy_rating, user.ratingAsBuddy),
                label = stringResource(R.string.profile_page_buddy_rating_label)
            )
            ProfileStatistic(
                headline = totalTravelsRating.toString(),
                label = stringResource(R.string.profile_page_travels_rating_label)
            )
        }
    }
}

@Composable
fun ProfilePicture(
    fullName: String,
    profilePictureURI: Uri?,
    sizeDp: Int
) {
    val initials = fullName.getMonogram()

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (profilePictureURI == null) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
        } else {
            AsyncImage(
                model = profilePictureURI,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun ProfileStatistic(headline: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BioSection(s: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.profile_page_bio_label),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = s,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class TravelSectionTab(val name: String, val icon: ImageVector)

@Composable
fun TravelsSection(
    isOwner: Boolean,
    showPastTravels: Boolean,
    onTravel: (String) -> Unit,
    vm: UserProfileViewModel = hiltViewModel()
) {
    val tabs = mutableListOf<TravelSectionTab>()

    if (vm.createdTravels.collectAsState().value.isNotEmpty())
        tabs.add(TravelSectionTab("Created", ImageVector.vectorResource(R.drawable.ic_edit_filled)))

    if (vm.pastTravels.collectAsState().value.isNotEmpty() &&
        (showPastTravels || isOwner)
    ) {
        tabs.add(TravelSectionTab("Past", ImageVector.vectorResource(R.drawable.ic_date_range)))
    }

    if (vm.nextTravels.collectAsState().value.isNotEmpty() && isOwner) {
        tabs.add(0, TravelSectionTab("Applied", ImageVector.vectorResource(R.drawable.ic_check)))
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Row {
            Text(
                text = "Travels",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (tabs.isEmpty())
            Text(
                text = "Nothing to show here... yet",
                style = MaterialTheme.typography.bodyMedium,
            )
        else
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 4.dp
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(top = 8.dp)
            ) {
                Column {
                    Row {
                        SecondaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(tab.name) },
                                    icon = {
                                        Icon(
                                            tab.icon, contentDescription = "${tab.name} travels"
                                        )
                                    })
                            }
                        }
                    }
                    if (tabs[selectedTabIndex].name == "Applied") {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vm.nextTravels.value) { t->
                                TravelCardSmallWithClick(t, vm.user.collectAsState().value, onTravel)
                            }
                        }
                    }
                    if (tabs[selectedTabIndex].name == "Created") {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vm.createdTravels.value) { t->
                                TravelCardSmallWithClick(t, vm.user.collectAsState().value, onTravel)
                            }
                        }
                    }
                    if (tabs[selectedTabIndex].name == "Past") {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vm.pastTravels.value) { t->
                                TravelCardSmallWithClick(t, vm.user.collectAsState().value, onTravel)
                            }
                        }
                    }
                }
            }
    }
}

@Composable
fun PersonalInfoSection(email: String, phone: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.profile_page_personal_info_label),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        InfoRow(label = stringResource(R.string.profile_page_email, email))
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(label = stringResource(R.string.profile_page_phone, phone))
    }
}

@Composable
private fun InfoRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BuddyReviewsSection(reviews: List<Triple<User, Travel, UserReview>>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTriple by remember { mutableStateOf<Triple<User, Travel, UserReview>?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = reviews.isNotEmpty()) { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.profile_page_buddies_reviews, reviews.size),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (reviews.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded)
                        ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_up)
                    else
                        ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_down),
                    contentDescription = if (expanded) "Collapse reviews" else "Expand reviews",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp)
            ) {
                reviews
                    .filter { it.third.text.isNotBlank() }
                    .forEachIndexed { idx, (user, travel, review) ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (!user.profilePicture.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user.profilePicture,
                                            contentDescription = "Reviewer profile picture",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.fullName.getMonogram(),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Icon(
                                        imageVector = if (review.isPositive)
                                            ImageVector.vectorResource(R.drawable.ic_thumb_up)
                                        else
                                            ImageVector.vectorResource(R.drawable.ic_thumb_down),
                                        contentDescription = null,
                                        tint = if (review.isPositive)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = travel.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val relativeTime = remember(travel.dateStart) {
                                        DateUtils.getRelativeTimeSpanString(
                                            travel.dateStart.time,
                                            System.currentTimeMillis(),
                                            DateUtils.MINUTE_IN_MILLIS
                                        ).toString()
                                    }

                                    Text(
                                        text = relativeTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val maxPreviewLength = 140
                                    val isLongReview = review.text.length > maxPreviewLength

                                    if (isLongReview) {
                                        val showMoreTag = "show_more"
                                        val annotatedText = buildAnnotatedString {
                                            append(review.text.take(maxPreviewLength))
                                            append("... ")

                                            pushStringAnnotation(tag = showMoreTag, annotation = "expand")
                                            withStyle(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            ) {
                                                append(stringResource(id = R.string.show_more))
                                            }
                                            pop()
                                        }

                                        ClickableText(
                                            text = annotatedText,
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                            onClick = { offset ->
                                                annotatedText.getStringAnnotations(tag = showMoreTag, start = offset, end = offset)
                                                    .firstOrNull()?.let {
                                                        selectedTriple = Triple(user, travel, review)
                                                    }
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = review.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (idx < reviews.lastIndex) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }
                        }
                    }
            }
        }

        selectedTriple?.let { (user, travel, review) ->
            ReviewDetailsDialog(
                user = user,
                travel = travel,
                review = review,
                onDismiss = { selectedTriple = null }
            )
        }
    }
}

@Composable
fun ReviewDetailsDialog(
    user: User,
    travel: Travel,
    review: UserReview,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!user.profilePicture.isNullOrBlank()) {
                        AsyncImage(
                            model = user.profilePicture,
                            contentDescription = "Reviewer profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.getMonogram(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = travel.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = review.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun LogoutDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,

    ) {
    AlertDialog(
        title = {
            Text(text = "Attention")
        },
        text = {
            Text(text = "Are you sure you want to log out?")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

