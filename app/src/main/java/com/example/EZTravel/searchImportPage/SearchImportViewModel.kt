package com.example.EZTravel.searchImportPage


import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.filters.FiltersViewModel
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchImportViewModel @Inject constructor(private val model: TravelModel) : FiltersViewModel() {
    private val _travelList = MutableStateFlow<List<Travel>>(emptyList())

    private var lastDocument: DocumentSnapshot? = null
    private var endReached = false
    var isLoading = false

    init {
        loadMoreTravels()
    }

    fun loadMoreTravels() {
        if (isLoading || endReached) return

        isLoading = true

        viewModelScope.launch {
            val result = model.getAllTravelsPaginated(lastDocument)
            if (result.travels.isEmpty()) {
                endReached = true
            } else {
                _travelList.update { old ->
                    (old + result.travels).distinctBy { it.id }
                }
                lastDocument = result.lastVisible
            }
            isLoading = false
        }
    }

    @OptIn(FlowPreview::class)
    val filteredTravelList: StateFlow<List<Travel>> = combine(
        _travelList, _filters, snapshotFlow { searchFieldState.text }.debounce(500)
    ) { travelList, filters, searchFieldState ->
        withContext(Dispatchers.Default) {
            travelList.filter { travel ->
                val tripDurationCompliant =
                    filters.tripDuration == 0 || travel.days == filters.tripDuration
                val tripGroupSizeCompliant =
                    filters.tripGroupSize == 0 || travel.size == filters.tripGroupSize
                val priceRangeHighlightsCompliant =
                    travel.priceStart >= filters.priceRange.start &&
                            (travel.priceEnd <= filters.priceRange.endInclusive ||
                                    filters.priceRange.endInclusive == 2500f)

                val highlightsCompliant = travel.highlights.containsAll(
                    filters.tripHighlights.map { it.ordinal }
                )

                val travelIsCompliant =
                    tripDurationCompliant &&
                            tripGroupSizeCompliant &&
                            priceRangeHighlightsCompliant &&
                            highlightsCompliant

                if (searchFieldState.isNotBlank()) {
                    travelIsCompliant && (travel.title.contains(
                        searchFieldState.trim(), ignoreCase = true
                    ) || travel.location?.name?.contains(searchFieldState.trim(), ignoreCase = true) == true )
                } else {
                    travelIsCompliant
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, _travelList.value)
}

