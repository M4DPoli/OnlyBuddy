package com.example.EZTravel.homePage

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.filters.FiltersViewModel
import com.example.EZTravel.travelPage.State
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.example.EZTravel.userProfile.User
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val model: TravelModel,
    userManager: AuthUserManager
) : FiltersViewModel() {

    val currentUser: StateFlow<User?> = userManager.currentUser

    val loggedIn : StateFlow<Boolean> = userManager.loggedIn

    private val _previous = MutableStateFlow<Boolean?>(null)
    val previous: StateFlow<Boolean?> = _previous

    fun setPrevious(value: Boolean) {
        _previous.value = value
    }

    private val _confirmedTrips = MutableStateFlow<List<Travel>>(emptyList())
    val confirmedTrips: StateFlow<List<Travel>> = _confirmedTrips

    private val _futureCreatedTrips = MutableStateFlow<List<Travel>>(emptyList())
    val futureCreatedTrips: StateFlow<List<Travel>> = _futureCreatedTrips

    private val _suggestions = MutableStateFlow<List<Travel>>(emptyList())
    val suggestions: StateFlow<List<Travel>> = _suggestions

    private var lastDocument: DocumentSnapshot? = null
    private var endReached = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            currentUser
                .flatMapLatest { user ->
                    if (user != null) {
                        val travelIds = user.travelRefs["applied"].orEmpty()
                        if (travelIds.isEmpty()) {
                            flowOf(emptyList())
                        } else {
                            val travelFlows = travelIds.map { id ->
                                model.getTravelById(id.id)
                            }
                            combine(travelFlows) { travelsArray ->
                                travelsArray.filter { travel ->
                                    travel.applications.any { app ->
                                        app.user?.id == user.id && app.state == State.ACCEPTED.ordinal
                                    }
                                }
                            }
                        }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { filteredTravels ->
                    _confirmedTrips.value = filteredTravels.filter { travel -> travel.dateStart.after(Date()) }.sortedBy { travel -> travel.dateStart }
                }
        }
        viewModelScope.launch {
            currentUser
                .flatMapLatest { user ->
                    if (user != null) {
                        val travelIds = user.travelRefs["created"].orEmpty()
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
                }
                .collect { filteredTravels ->
                    _futureCreatedTrips.value = filteredTravels.filter { travel -> travel.dateStart.after(Date()) }.sortedBy { travel -> travel.dateStart }
                }
        }

        viewModelScope.launch {
            currentUser.collectLatest { refreshSuggestions() }
        }
    }

    fun loadMoreSuggestions() {
        if (_isLoading.value || endReached) return

        _isLoading.value = true

        viewModelScope.launch {
            val result = model.getUpcomingTravelsPaginated(lastDocument)

            val filtered = result.travels.filter { it.owner?.id != currentUser.value?.id}

            if (filtered.isEmpty()) {
                endReached = true
            } else {
                _suggestions.update { old ->
                    (old + filtered).distinctBy { it.id } }
                lastDocument = result.lastVisible
            }
        }

        _isLoading.value = false
    }

    fun refreshSuggestions() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            endReached = false
            lastDocument = null
            val result = model.getUpcomingTravelsPaginated(null)
            val filtered = result.travels.filter { it.owner?.id != currentUser.value?.id}
            _suggestions.value = filtered
            lastDocument = result.lastVisible
            _isRefreshing.value = false
        }
    }
}