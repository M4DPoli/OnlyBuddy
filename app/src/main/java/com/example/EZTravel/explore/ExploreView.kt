package com.example.EZTravel.explore

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.FilterSearchBar
import com.example.EZTravel.R
import com.example.EZTravel.TravelCard
import com.example.EZTravel.filters.FiltersView
import com.example.EZTravel.mergePadding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Preview(name = "Pixel 7", device = "id:pixel_7")
@Composable
fun ExploreViewPreviewPixel7() {
    ExploreView({ _-> })
}

@Preview(name = "Pixel 5", device = "id:pixel_5")
@Composable
fun ExploreViewPreviewPixel5() {
    ExploreView({ _ -> })
}

@Preview(name = "Galaxy S20", device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
fun ExploreViewPreviewS20() {
    ExploreView({ _ -> })
}

@Preview(name = "Galaxy S20 landscape", device = "spec:width=800dp,height=360dp,dpi=420")
@Composable
fun ExploreViewPreviewS20Landscape() {
    ExploreView({ _-> })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreView(
    onTravel: (String) -> Unit,
    outerPadding: PaddingValues = PaddingValues(),
    vm: ExploreViewModel = hiltViewModel()
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val user = vm.currentUser.collectAsState().value

    val allTravels by vm.filteredTravelList.collectAsState()
    val favTravels by vm.filteredFavoritesTravelList.collectAsState()

    val travelToShow = if (selectedTabIndex == 0) allTravels else favTravels

    val filters = vm.filters.collectAsState()

    val gridState = rememberLazyGridState()

    val isRefreshing by vm.isRefreshing.collectAsState()

    //Filter BottomSheet
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterBottomSheet by rememberSaveable { mutableStateOf(false) }

    //FilterSearchBar
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    LaunchedEffect(gridState, selectedTabIndex) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 3 // adjust buffer if needed
            }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore && selectedTabIndex == 0) {
                    vm.loadMoreTravels()
                }
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                FilterSearchBar(vm, vm.filteredTravelList, scrollBehavior, {
                    onTravel(it)
                }, onFilterTap = {
                    showFilterBottomSheet = true
                })

                if (user != null && user.travelRefs["favorites"]?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Tabs(selectedTabIndex) { selectedTabIndex = it }
                }
            }

        },
    ) { innerPadding ->
        val mergedPadding = mergePadding(innerPadding,outerPadding)

        PullToRefreshBox(
            isRefreshing,
            onRefresh = { vm.refreshTravels() },
            modifier = Modifier
                .padding(mergedPadding)
        ) {
            Column {
                val columns = if (isLandscape) 2 else 1
                if (travelToShow.isNotEmpty()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                        itemsIndexed(travelToShow) { i, t ->
                            Spacer(modifier = Modifier.height(16.dp))
                            TravelCard(onTravel = onTravel, travel = t)
                            if (i >= travelToShow.size - 3) {
                                vm.loadMoreTravels()
                            }
                        }
                    }
                } else if (vm.isLoading.collectAsState().value || vm.isRefreshing.collectAsState().value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }



        if (showFilterBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showFilterBottomSheet = false
                }, sheetState = filterSheetState
            ) {
                FiltersView(
                    modifier = Modifier
                        .padding(
                            bottom =  mergedPadding.calculateBottomPadding()
                        )
                        .safeContentPadding(),
                    onButtonClick = {
                        showFilterBottomSheet = false
                    },
                    filters = filters.value,
                    updateFilters = { duration, size, range, highlights ->
                        vm.updateFilter(duration, size, range, highlights)
                    },
                    resetFilters = {
                        vm.resetFilter()
                    })
            }
        }
    }
}

data class ExploreTab(@StringRes val nameRes: Int, val icon: ImageVector)

@Composable
fun Tabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        ExploreTab(
            R.string.explore_page_tab_all,
            ImageVector.vectorResource(R.drawable.ic_location_on_filled)
        ),
        ExploreTab(
            R.string.explore_page_tab_favorites,
            ImageVector.vectorResource(R.drawable.ic_favorite)
        ),
    )

    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        tabs.forEachIndexed { i, t ->
            Tab(
                selected = i == selectedTabIndex,
                onClick = { onTabSelected(i) },
                text = { Text(stringResource(t.nameRes)) },
                icon = {
                    Icon(
                        t.icon,
                        contentDescription = stringResource(t.nameRes)
                    )
                }
            )
        }
    }
}