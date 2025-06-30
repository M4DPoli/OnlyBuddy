package com.example.EZTravel

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.PluralsRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.EZTravel.filters.FiltersViewModel
import com.example.EZTravel.travelPage.Highlights
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.userProfile.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import com.example.EZTravel.travelPage.Application
import com.example.EZTravel.travelPage.State
import com.example.EZTravel.userProfile.UserProfileViewModel
import java.util.Date

object AppBarColors {
    @OptIn(ExperimentalMaterial3Api::class)
    val TopAppBarColor: @Composable () -> TopAppBarColors = {
        topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    val BottomBarContainerColor: @Composable () -> Color = {

        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val BottomBarContentColor: @Composable () -> Color = {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val BottomBarTonalElevation: Dp = 3.dp
}

@Composable
fun NavBar(
    navController: NavController,
    onHome: () -> Unit,
    onExplore: () -> Unit,
    onCreateTravelNew: () -> Unit,
    onUser: () -> Unit,
    onChatList: () -> Unit,
    onLogin: () -> Unit,
    vm: NavbarViewModel = hiltViewModel()
) {

    val user by vm.currentUser.collectAsState()
    val n by vm.nNotifications.collectAsState()
    val unread by vm.unreadChats.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home", onClick = { onHome() }, icon = {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_home_filled),
                    contentDescription = "Home"
                )
            },
            label = { Text(stringResource(R.string.navbar_home_label)) })
        NavigationBarItem(
            selected = currentRoute == "explore", onClick = { onExplore() }, icon = {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_search),
                    contentDescription = "Search"
                )
            },
            label = { Text(stringResource(R.string.navbar_explore_label)) })
        NavigationBarItem(
            selected = currentRoute == "create",
            onClick = {
                if (user == null)
                    onLogin()
                else {
                    onCreateTravelNew()
                }
            },
            icon = {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_add),
                    contentDescription = "New travel"
                )
            },
            label = { Text(stringResource(R.string.navbar_new_travel_label)) })
        NavigationBarItem(
            selected = currentRoute == "Chat",
            onClick = {
                if (user == null)
                    onLogin()
                else {
                    onChatList()
                }
            },
            icon = {
                BadgedBox(
                    badge = {
                        if (unread > 0) {
                            Badge {
                                Text(unread.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_chat),
                        contentDescription = "Chat"
                    )
                }

            },
            label = { Text(stringResource(R.string.navbar_chat_label)) })
        NavigationBarItem(selected = currentRoute == "user", onClick = {

            if (user == null)
                onLogin()
            else {
                onUser()
            }

        }, icon = {
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
                    ImageVector.vectorResource(R.drawable.ic_person),
                    contentDescription = "Profile"
                )
            }
        }, label = { Text(stringResource(R.string.navbar_profile_label)) })
    }
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
fun EditableProfilePicture(
    fullName: String,
    profilePictureURI: Uri?,
    pxl: Int,
    isEditing: Boolean,
    setTakePhotoMode: ((Boolean) -> Unit)? = null,
    photoPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>? = null,
    cameraPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = if (isEditing) {
            Modifier
                .size(pxl.dp)
                .clickable(onClick = { expanded = !expanded })
        } else {
            Modifier.size(pxl.dp)
        }, contentAlignment = Alignment.BottomCenter
    ) {
        if (profilePictureURI == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName.getMonogram(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = (0.30 * pxl).sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profilePictureURI,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

            }
        }
    }
}

@Composable
fun ProfilePicture(
    fullName: String, profilePictureURI: Uri?, pxl: Int
) {
    Box(
        modifier = Modifier.size(pxl.dp), contentAlignment = Alignment.BottomCenter
    ) {
        if (profilePictureURI == null || profilePictureURI.toString().isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName.getMonogram(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = (0.30 * pxl).sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profilePictureURI,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

            }
        }
    }
}

@Composable
fun TravelCardSmallWithClickForSearch(
    t: Travel,
    onCreateTravel: (String) -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCreateTravel(t.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = t.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (t.reviews.isNotEmpty()) {
                    val avg = t.reviews.map { it.stars }.average()
                    ReviewsWithStarsSmall(avg)
                }
            }

            AsyncImage(
                model = t.images.firstOrNull() ?: R.drawable.placeholder_landscape,
                contentDescription = null,
                imageLoader = ImageLoader.Builder(LocalContext.current).build(),
                placeholder = painterResource(R.drawable.placeholder_landscape),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        }
    }
}

@Composable
fun TravelCardSmallWithClick(
    t: Travel,
    user: User?,
    onTravel: (String) -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onTravel(t.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = t.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val userApplication = t.applications.firstOrNull { it.user?.id == user?.id }
                when {
                    t.dateStart.before(Date()) && t.rating != 0.0 -> {
                        ReviewsWithStarsSmall(t.rating)
                    }

                    user != null && userApplication != null -> {
                        TravelApplicationStatus(userApplication)
                    }

                    t.owner?.id == user?.id && t.applications.any { it.state == State.PENDING.ordinal } -> {
                        TravelApplications(t.applications)
                    }
                }
            }

            AsyncImage(
                model = t.images.firstOrNull() ?: R.drawable.placeholder_landscape,
                contentDescription = null,
                imageLoader = ImageLoader.Builder(LocalContext.current).build(),
                placeholder = painterResource(R.drawable.placeholder_landscape),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        }
    }
}

@Composable
fun TravelApplications(applications: List<Application>) {
    val nrPending = applications.count { it.state == State.PENDING.ordinal }
    AssistChip(
        onClick = {},
        label = {
            Text(text = "$nrPending pending")
        },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_person_filled),
                contentDescription = "Applications pending",
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            labelColor = MaterialTheme.colorScheme.primary,
            leadingIconContentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 24.dp)
            .heightIn(min = 24.dp)
    )
}

@Composable
fun TravelApplicationStatus(application: Application) {
    val (statusText, statusIcon, statusColor) = when (application.state) {
        State.PENDING.ordinal -> Triple(
            "Pending",
            R.drawable.ic_hourglass_empty,
            MaterialTheme.colorScheme.outline
        )

        State.ACCEPTED.ordinal -> Triple(
            "Accepted",
            R.drawable.ic_check,
            MaterialTheme.colorScheme.primary
        )

        State.REJECTED.ordinal -> Triple(
            "Rejected",
            R.drawable.ic_close,
            MaterialTheme.colorScheme.error
        )

        else -> Triple("N/A", R.drawable.ic_hourglass_empty, MaterialTheme.colorScheme.outline)
    }

    AssistChip(
        onClick = {},
        label = {
            Text(text = statusText)
        },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(statusIcon),
                contentDescription = "Application status",
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = statusColor.copy(alpha = 0.12f),
            labelColor = statusColor,
            leadingIconContentColor = statusColor
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 24.dp)
            .heightIn(min = 24.dp)
    )
}

@Composable
fun ReviewsWithStarsSmall(rating: Double) {
    AssistChip(
        onClick = {},
        label = {
            Text(text = stringResource(R.string.review_stars_out_of_five, rating))
        },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_filled),
                contentDescription = "Travel rating",
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            labelColor = MaterialTheme.colorScheme.outline,
            leadingIconContentColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 24.dp)
            .heightIn(min = 24.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersHighlightsSection(
    highlights: Set<Highlights>,
    modifier: Modifier = Modifier,
    onClick: ((Highlights) -> Unit)? = null
) {
    Column {
        Row {
            Text(
                text = stringResource(R.string.profile_page_highlights_label),
                style = MaterialTheme.typography.titleMedium
            )
        }
        FlowRow(
            modifier = modifier.then(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ), overflow = FlowRowOverflow.Visible
        ) {
            Highlights.entries.forEach { h ->
                val hName = stringResource(h.tag)
                FilterChip(
                    onClick = { onClick?.let { it(h) } },
                    label = { Text(hName) },
                    selected = highlights.contains(h),
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(h.icon),
                            contentDescription = hName
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}


@Composable
fun HighlightsSection(highlights: List<Int>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        highlights.forEach { index ->
            val entry = Highlights.entries[index]
            AssistChip(
                onClick = {},
                label = { Text(LocalContext.current.getString(entry.tag)) },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(entry.icon),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
fun EditHighlightsSection(
    highlights: List<Int>,
    openModal: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Title Row with Edit Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.profile_page_highlights_label),
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(
                onClick = openModal,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled),
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.edit),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Highlight chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            highlights.forEach { h ->
                AssistChip(
                    onClick = {}, // Non-clickable
                    label = {
                        Text(text = context.getString(Highlights.entries[h].tag))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(Highlights.entries[h].icon),
                            contentDescription = context.getString(Highlights.entries[h].tag)
                        )
                    }
                )
            }
        }

    }
}

@Composable
fun TravelCard(
    onTravel: (String) -> Unit,
    travel: Travel,
) {
    val formatter = SimpleDateFormat("dd MMM")
    val context = LocalContext.current

    val priceText = if (travel.priceStart == travel.priceEnd) {
        stringResource(R.string.travel_card_price_single, travel.priceStart)
    } else {
        stringResource(R.string.travel_card_price_range, travel.priceStart, travel.priceEnd)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onTravel(travel.id) },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            AsyncImage(
                model = travel.images.firstOrNull() ?: R.drawable.placeholder_landscape,
                contentDescription = null,
                imageLoader = ImageLoader.Builder(LocalContext.current).build(),
                placeholder = painterResource(R.drawable.placeholder_landscape),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Title
            Text(
                text = travel.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(
                    R.string.travel_card_info,
                    formatter.format(travel.dateStart),
                    formatter.format(travel.dateEnd),
                    travel.size,
                    priceText
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            TravelCardHighlights(
                travel.highlights,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )


        }
    }
}

@Composable
fun TravelCardHighlights(
    highlights: List<Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SubcomposeLayout(modifier = modifier) { constraints ->
        val containerWidth = constraints.maxWidth
        val spacing = 8.dp.roundToPx()
        val chipHeight = 32.dp.roundToPx()

        // Compose chips to measure widths
        val chipsPlaceables = highlights.map { h ->
            val placeable = subcompose(h) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = context.getString(Highlights.entries[h].tag),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(Highlights.entries[h].icon),
                            contentDescription = context.getString(Highlights.entries[h].tag),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 24.dp)
                        .heightIn(min = 24.dp)
                )
            }[0].measure(Constraints())
            placeable
        }

        var usedWidth = 0
        var visibleCount = 0

        for (placeable in chipsPlaceables) {
            if (usedWidth + placeable.width > containerWidth) break
            usedWidth += placeable.width + spacing
            visibleCount++
        }

        val remainingCount = highlights.size - visibleCount

        // Compose "+X more" chip only once, with real label
        val moreChipPlaceable = if (remainingCount > 0) {
            subcompose("more") {
                AssistChip(
                    onClick = { },
                    label = { Text("+$remainingCount more") },
                    enabled = false,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 24.dp)
                        .heightIn(min = 24.dp)
                )
            }[0].measure(Constraints())
        } else null

        // Adjust visible count if "+X more" chip won't fit
        if (remainingCount > 0) {
            if (usedWidth + (moreChipPlaceable?.width ?: 0) > containerWidth) {
                if (visibleCount > 0) {
                    visibleCount--
                    // Recalculate usedWidth after removing last chip
                    usedWidth = chipsPlaceables.take(visibleCount).sumOf { it.width + spacing }
                }
            }
        }

        layout(containerWidth, chipHeight) {
            var xPos = 0
            for (i in 0 until visibleCount) {
                chipsPlaceables[i].placeRelative(x = xPos, y = 0)
                xPos += chipsPlaceables[i].width + spacing
            }

            moreChipPlaceable?.placeRelative(x = xPos, y = 0)
        }
    }
}

@Composable
fun ManualMinusPlusCounter(
    coreValue: Int,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabledOnZeroValue: Boolean = false,
    coreIcon: ImageVector? = null,
    @PluralsRes coreText: Int? = null
) {
    val isMinusEnabled = !(disabledOnZeroValue && coreValue == 0)

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedIconButton(
            onClick = onMinusClick,
            enabled = isMinusEnabled
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_remove),
                contentDescription = "Decrease",
                modifier = Modifier.size(24.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            if (disabledOnZeroValue && coreValue == 0) {
                Text(
                    text = stringResource(R.string.filters_disabled),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = coreText?.let { pluralStringResource(it, coreValue, coreValue) }
                        ?: coreValue.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                coreIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        OutlinedIconButton(onClick = onPlusClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                contentDescription = "Increase",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSearchBar(
    vm: FiltersViewModel,
    filteredTravelList: StateFlow<List<Travel>>,
    searchBarScrollBehavior: SearchBarScrollBehavior,
    onResultTap: (String) -> Unit,
    onFilterTap: () -> Unit,
    onBack: () -> Unit = {},
    isSearch: Boolean = false
) {
    val filters by vm.filters.collectAsState()
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            searchBarState = searchBarState,
            textFieldState = vm.searchFieldState,
            onSearch = {
                scope.launch { searchBarState.animateToCollapsed() }
            },
            placeholder = {
                Text(text = stringResource(R.string.explore_page_searchBar_placeholder))
            },
            leadingIcon = {
                when {
                    searchBarState.currentValue == SearchBarValue.Expanded -> {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    isSearch -> {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    else -> {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingIcon = {
                Row {
                    if (vm.searchFieldState.text.isNotBlank()) {
                        IconButton(onClick = { vm.clearSearchFieldState() }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                contentDescription = "Clear search",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (filters.isActive) {
                        FilledIconButton(onClick = onFilterTap) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_filter_alt),
                                contentDescription = "Filters",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onFilterTap) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_filter_alt),
                                contentDescription = "Filters",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        )
    }

    TopSearchBar(
        scrollBehavior = searchBarScrollBehavior,
        state = searchBarState,
        inputField = inputField
    )

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField
    ) {
        FilteredSearchResults(
            onResultTap = onResultTap,
            filteredTravelList = filteredTravelList
        )
    }
}

@Composable
fun FilteredSearchResults(
    onResultTap: (String) -> Unit,
    filteredTravelList: StateFlow<List<Travel>>
) {
    val fTravelList by filteredTravelList.collectAsState()

    if (fTravelList.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            items(fTravelList) { travel ->
                TravelCardSmallWithClick(
                    t = travel,
                    user = null,    // don't care
                    onTravel = onResultTap
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.filters_no_travel_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

//Carosello di immagini
@Composable
fun Carousel(
    images: List<String>,
    onBack: () -> Unit,
    onAdd: (uri: Uri) -> Unit,
    onRemove: (index: Int) -> Unit,
    onClone: () -> Unit,
    editMode: Boolean
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
                FilledIconButton(onClick = { onClone() }) {
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

@Composable
fun mergePadding(a: PaddingValues, b: PaddingValues): PaddingValues {
    return PaddingValues(
        start = maxOf(
            a.calculateStartPadding(LayoutDirection.Ltr),
            b.calculateStartPadding(LayoutDirection.Ltr)
        ), top = maxOf(a.calculateTopPadding(), b.calculateTopPadding()), end = maxOf(
            a.calculateEndPadding(LayoutDirection.Ltr), b.calculateEndPadding(LayoutDirection.Ltr)
        ), bottom = maxOf(a.calculateBottomPadding(), b.calculateBottomPadding())
    )
}

@Composable
fun StarReviewComponent(rate: Int, updateRate: (Int) -> Unit) {
    for (i in 1..5) {
        IconButton(onClick = {
            if (i == rate) {
                updateRate(0)
            } else {
                updateRate(i)
            }
        }) {
            if (rate >= i) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_star_filled),
                    contentDescription = "$i star",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_empty_star),
                    contentDescription = "$i star",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewBuddyComponent(
    user: User,
    setThumb: (Boolean?) -> Unit,
    thumb: Boolean?,
    reviewText: String,
    updateReviewText: (String) -> Unit,
    isOwner: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfilePicture(user.fullName, user.profilePicture?.toUri(), 40)
            Spacer(Modifier.width(8.dp))

            Text(
                user.fullName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isOwner) {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                )
                Text(
                    text = stringResource(R.string.travel_review_buddy_owner),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconToggleButton(
                modifier = Modifier.size(36.dp),
                checked = thumb == true,
                onCheckedChange = {
                    setThumb(if (thumb == true) null else true)
                },
                content = {
                    Icon(
                        painterResource(R.drawable.ic_thumb_up),
                        contentDescription = "Thumb up",
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            Spacer(Modifier.width(4.dp))
            IconToggleButton(
                modifier = Modifier.size(36.dp),
                checked = thumb == false,
                onCheckedChange = {
                    setThumb(if (thumb == false) null else false)
                },
                content = {
                    Icon(
                        painterResource(R.drawable.ic_thumb_down),
                        contentDescription = "Thumb down",
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = reviewText,
            onValueChange = updateReviewText,
            placeholder = {
                Text(stringResource(R.string.travel_review_review_placeholder))
            },
            label = {
                Text(stringResource(R.string.travel_review_buddy_optional, user.fullName))
            },
            singleLine = true
        )
    }
}
