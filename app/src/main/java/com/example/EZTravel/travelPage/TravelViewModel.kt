package com.example.EZTravel.travelPage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.userProfile.User
import com.example.EZTravel.userProfile.UserProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.sql.Time
import java.util.TimeZone
import javax.inject.Inject

fun adaptTime(time: Long): Time {
    val offset = TimeZone.getDefault().rawOffset
    val adjustedMillis = time - offset
    return Time(adjustedMillis)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TravelViewModel @Inject constructor(
    private val model: TravelModel,
    private val userModel: UserProfileModel,
    userManager: AuthUserManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle[EZTravelDestinationsArgs.TRAVEL_ID])

    private val _isApplied = MutableStateFlow(false)
    val isApplied: StateFlow<Boolean> = _isApplied

    private val _isRejected = MutableStateFlow(false)
    val isRejected: StateFlow<Boolean> = _isRejected

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private val _owner = MutableStateFlow(User(id = ""))
    val owner: StateFlow<User> = _owner

    private val _travel = MutableStateFlow(Travel())
    val travel: StateFlow<Travel> = _travel

    val currentUser: StateFlow<User?> = userManager.currentUser

    private val _listOfReviews = MutableStateFlow<List<Pair<User, TravelReview>>>(emptyList())
    val listOfReviews: StateFlow<List<Pair<User, TravelReview>>> = _listOfReviews

    init {
        viewModelScope.launch {
            model.getTravelById(id).collect {
                _travel.value = it
            }
        }
        viewModelScope.launch {
            combine(_travel, currentUser) { travel, user ->
                val isApplied = travel.applications.any { it.user!!.id == user?.id }
                val isRejected =
                    travel.applications.any { it.user!!.id == user?.id && it.state == State.REJECTED.ordinal }
                val isFavorite =
                    user?.travelRefs?.get("favorites")?.any { fav -> fav.id == travel.id } ?: false
                Triple(isApplied, isRejected, isFavorite)
            }.collect { (applied, rejected, favorite) ->
                _isApplied.value = applied
                _isRejected.value = rejected
                _isFavorite.value = favorite
            }
        }

        viewModelScope.launch {
            _travel
                .filter { it.owner != null }
                .flatMapLatest { travel ->
                    userModel.getUserById(travel.owner!!.id)
                }
                .collect { owner ->
                    _owner.value = owner
                }
        }

        viewModelScope.launch {
            _travel
                .filter { it.reviews.isNotEmpty() }
                .flatMapLatest { travel ->
                    val userFlows = travel.reviews.map { review ->
                        userModel.getUserById(review.user!!.id)
                    }
                    if (userFlows.isNotEmpty()) {
                        combine(userFlows) { usersList ->
                            usersList.zip(travel.reviews.filter { it.user != _travel.value.owner }) { user, application -> user to application }
                        }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { usersList ->
                    _listOfReviews.value = usersList
                }
        }
    }

    fun toggleFavorite() {
        if (currentUser.value != null) {
            if (!_isFavorite.value) userModel.addTravelRefTo(
                "favorites",
                _travel.value.id,
                currentUser.value!!.id
            )
            else userModel.deleteTravelRefFrom(
                "favorites",
                _travel.value.id,
                currentUser.value!!.id
            )
        }
    }


    fun getAcceptedApplications(): Int {
        return _travel.value.applications.count { application ->
            State.entries[application.state] == State.ACCEPTED
        }
    }

    fun getTotalSize(): Int {
        return _travel.value.applications.filter { application ->
            State.entries[application.state] == State.ACCEPTED
        }.sumOf { application -> application.size }
    }

    suspend fun addNewApplications(application: Application): Boolean {
        return model.addApplication(application, _travel.value.id)
    }

    suspend fun deleteApplication(): Boolean {
        return if (currentUser.value != null){
            model.deleteApplication(currentUser.value!!.id, _travel.value.id)
        } else false
    }

    fun canReview(id: String?): Boolean {
        return _travel.value.applications.any { application ->
            application.user!!.id == id && _owner.value.id != id && State.entries[application.state] == State.ACCEPTED
        }
    }

    fun alreadyReviewed(id: String?): Boolean {
        return _travel.value.reviews.any { review ->
            review.user!!.id == id
        }
    }

}