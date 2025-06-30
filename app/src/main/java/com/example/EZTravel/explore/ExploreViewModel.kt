package com.example.EZTravel.explore


import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.filters.FiltersViewModel
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.example.EZTravel.userProfile.User
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val model: TravelModel,
    userManager: AuthUserManager
) : FiltersViewModel() {

    private val _travelList = MutableStateFlow<List<Travel>>(emptyList())
//    val travelList: StateFlow<List<Travel>> = _travelList

    private val _currentUser: StateFlow<User?> = userManager.currentUser
    val currentUser = _currentUser

    private val favouriteTravelList = MutableStateFlow<List<Travel>>(emptyList())

    private var lastDocument: DocumentSnapshot? = null
    private var endReached = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            _currentUser.firstOrNull()?.let { user ->
                loadMoreTravels(user)
            }
        }

        viewModelScope.launch {
            _currentUser.flatMapLatest {  user ->
                if (user != null) {
                    val travelIds = user.travelRefs["favorites"].orEmpty()
                    if (travelIds.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val travelFlows = travelIds.map { id ->
                            model.getTravelById(id.id)
                        }
                        combine(travelFlows) { travelsArray ->
                            travelsArray.toList()
                        }
                    }
                } else {
                    flowOf(emptyList())
                }
            }.collect {
                favouriteTravelList.value = it
            }
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
                    travelIsCompliant &&
                    ((travel.title.contains(searchFieldState.trim(), ignoreCase = true) ||
                    travel.location?.name?.contains(searchFieldState.trim(), ignoreCase = true) ?: true))
                } else {
                    travelIsCompliant
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, _travelList.value)

    @OptIn(FlowPreview::class)
    val filteredFavoritesTravelList: StateFlow<List<Travel>> = combine(
        favouriteTravelList, _filters, snapshotFlow { searchFieldState.text }.debounce(500)
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
                    ) || travel.location?.name?.contains(searchFieldState.trim(), ignoreCase = true) == true)
                } else {
                    travelIsCompliant
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, favouriteTravelList.value)

    fun loadMoreTravels(user: User? = _currentUser.value) {
        if (isLoading.value || endReached) return

        _isLoading.value = true

        viewModelScope.launch {
            var fetchedEnough = false
            var attempts = 0
            val maxAttempts = 5

            while (!fetchedEnough && !endReached && attempts < maxAttempts) {
                val result = model.getUpcomingTravelsPaginated(lastDocument)
                lastDocument = result.lastVisible

                val filtered = if (user != null) {
                    result.travels.filter { it.owner?.id != _currentUser.value?.id }
                } else {
                    result.travels
                }

                if (filtered.isNotEmpty()) {
                    _travelList.update { old ->
                        (old + filtered).distinctBy { it.id }
                    }
                    fetchedEnough = true
                }

                if (result.travels.isEmpty() || lastDocument == null) {
                    endReached = true
                }

                attempts++
            }
        }
        _isLoading.value = false
    }

    fun refreshTravels() {
        viewModelScope.launch {
            _isRefreshing.value = true
            endReached = false
            lastDocument = null
            _travelList.value = emptyList()
            loadMoreTravels()
            _isRefreshing.value = false
        }
    }
}