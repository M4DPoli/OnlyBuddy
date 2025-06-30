package com.example.EZTravel.homePage

import android.content.res.Configuration
import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.example.EZTravel.R
import com.example.EZTravel.travelPage.Travel
import java.util.Date
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.Carousel
import com.example.EZTravel.TravelCard
import com.example.EZTravel.mergePadding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

val monthNames = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

fun getDateString(date: Date): String {
    val calendar = Calendar.getInstance().apply { time = date }
    return "${calendar.get(Calendar.DAY_OF_MONTH)} ${monthNames[calendar.get(Calendar.MONTH)]}"
}

fun getDateIntervalString(dateRange: Pair<Date, Date>): String {
    val date1 = Calendar.getInstance().apply { time = dateRange.first }
    val date2 = Calendar.getInstance().apply { time = dateRange.second }
    return if (date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH)) {
        "${date1.get(Calendar.DAY_OF_MONTH)} - ${date2.get(Calendar.DAY_OF_MONTH)} ${
            monthNames[date1.get(
                Calendar.MONTH
            )]
        }"
    } else {
        "${date1.get(Calendar.DAY_OF_MONTH)} ${monthNames[date1.get(Calendar.MONTH)]} - ${
            date2.get(
                Calendar.DAY_OF_MONTH
            )
        } ${monthNames[date2.get(Calendar.MONTH)]}"
    }
}

@Preview
@Composable
fun HomePagePreview() {
    HomePageView(onTravel = { }, onSnack = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageView(
    onTravel: (String) -> Unit,
    onSnack:(String) -> Unit,
    outerPadding: PaddingValues = PaddingValues(),
    vm: HomePageViewModel = hiltViewModel()
) {
    val confirmedTrips by vm.confirmedTrips.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val futureCreatedTrips by vm.futureCreatedTrips.collectAsState()
    val previous by vm.previous.collectAsState()
    val loggedIn by vm.loggedIn.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    val listState = rememberLazyListState()
    val showHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100 }
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 2 else 1
    val gridState = rememberLazyGridState()

    LaunchedEffect(loggedIn) {
        if (previous == null) {
//            Log.d("SNAcK","Set previous to $loggedIn")
            // First run: don't show any snackbar, just record the value
            vm.setPrevious(loggedIn)
            return@LaunchedEffect
        }

        if (loggedIn != previous) {
//            Log.d("SNAcK","Triggered snack")
            if (loggedIn) {
//                Log.d("SNAcK","Logged in")
                onSnack("Welcome back ${currentUser?.username}")
            } else {
//                Log.d("SNAcK","Logged out")
                onSnack("Signed out successfully")
            }
//            Log.d("SNAcK","previous = $previous")
//            Log.d("SNAcK","Set previous to $loggedIn")
            vm.setPrevious(loggedIn)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val totalItems = listState.layoutInfo.totalItemsCount
                if (lastVisibleIndex >= totalItems - 3) {
                    vm.loadMoreSuggestions()
                }
            }
    }

    Scaffold { innerPadding ->
        val mergedPadding = mergePadding(innerPadding, outerPadding)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { vm.refreshSuggestions() },
            modifier = Modifier
                .fillMaxSize()
                .padding(mergedPadding)
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // Spinner or loader
                if (isLoading || isRefreshing) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AnimatedVisibility(
                            visible = showHeader,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 })
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }

                    // Confirmed trips carousel
                    if (confirmedTrips.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Your next confirmed trips",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Carousel(confirmedTrips, onTravel)
                        }
                    }

                    // Created trips carousel
                    if (futureCreatedTrips.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Your next created trips",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding( bottom = 8.dp)
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Carousel(futureCreatedTrips, onTravel)
                        }
                    }

                    // 4) Section header (full width)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "You may be interested in…",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 5) Suggestions cards (one per grid cell)
                    itemsIndexed(suggestions) { index, travel ->
                        TravelCard(onTravel = onTravel, travel = travel)
                        // pre-load more when you near the end
                        if (index >= suggestions.lastIndex - 2) {
                            vm.loadMoreSuggestions()
                        }
                    }

                    // 6) Optional footer spacer (span full width)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun Carousel(
    travels: List<Travel>,
    onTravel: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        itemsIndexed(travels, key = { _, item -> item.id }) { index, item ->
            Row {
                if (index == 0) {
                    Spacer(modifier = Modifier.size(16.dp))
                }
                CarouselTravelCard(item, onTravel)
                if (index == travels.size - 1) {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        }

    }
}

@Composable
fun CarouselTravelCard(
    travel: Travel,
    onTravel: (String) -> Unit
) {
    var sizeImage by remember { mutableStateOf(IntSize.Zero) }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.4f),
            Color.Black.copy(alpha = 0.75f)
        ),
        startY = sizeImage.height * 0.4f,
        endY = sizeImage.height.toFloat()
    )

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(320.dp)
            .height(170.dp)
            .clickable { onTravel(travel.id) }
    ) {
        Box {
            AsyncImage(
                model = travel.images.firstOrNull() ?: R.drawable.placeholder_landscape,
                contentDescription = null,
                imageLoader = ImageLoader.Builder(LocalContext.current).build(),
                placeholder = painterResource(R.drawable.placeholder_landscape),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { sizeImage = it.size }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = travel.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (travel.dateStart == travel.dateEnd)
                            getDateString(travel.dateStart)
                        else
                            getDateIntervalString(travel.dateStart to travel.dateEnd),
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}