package com.example.EZTravel.searchImportPage

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.FilterSearchBar
import com.example.EZTravel.R
import com.example.EZTravel.TravelCardSmallWithClickForSearch
import com.example.EZTravel.filters.FiltersView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchImportView(
    onCreateTravel: (String) -> Unit,
    onBack: () -> Unit,
    vm: SearchImportViewModel = hiltViewModel()
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val travelList = vm.filteredTravelList.collectAsState()
    val filters = vm.filters.collectAsState()

    //Filter BottomSheet
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFilterBottomSheet by rememberSaveable { mutableStateOf(false) }

    //FilterSearchBar
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
            ) {
                FilterSearchBar(
                    vm = vm,
                    filteredTravelList = vm.filteredTravelList,
                    searchBarScrollBehavior = scrollBehavior,
                    onResultTap = onCreateTravel,
                    onFilterTap = { showFilterBottomSheet = true },
                    onBack = onBack,
                    isSearch = true
                )
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->

        val columns = if (isLandscape) 2 else 1

        if (travelList.value.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),

            ) {
                itemsIndexed(travelList.value) { index, travel ->
                    TravelCardSmallWithClickForSearch(travel, onCreateTravel)
                    if (index >= travelList.value.size - 3) {
                        vm.loadMoreTravels()
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.filters_no_travel_found))
            }
        }

        if (showFilterBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showFilterBottomSheet = false
                }, sheetState = filterSheetState
            ) {
                FiltersView(
                    modifier =
                    Modifier
                        .padding(
                            bottom = padding.calculateBottomPadding()
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
                    }
                )
            }
        }
    }
}


