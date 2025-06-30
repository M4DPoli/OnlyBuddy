package com.example.EZTravel.applicationsPage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.travelPage.Application
import com.example.EZTravel.travelPage.State
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.example.EZTravel.userProfile.User
import com.example.EZTravel.userProfile.UserProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val model: TravelModel,
    private val userModel: UserProfileModel,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle[EZTravelDestinationsArgs.TRAVEL_ID])

    private val _travel = MutableStateFlow(Travel())
    val travel: StateFlow<Travel> = _travel

    private val _listOfApplications = MutableStateFlow<List<Pair<User,Application>>>(emptyList())
    val listOfApplications: StateFlow<List<Pair<User,Application>>> = _listOfApplications

    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent

    private val _isLoadingChanges = MutableStateFlow(false)
    val isLoadingChanges: StateFlow<Boolean> = _isLoadingChanges

    val hasModified = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _isLoadingContent.value = true
                model.getTravelById(id).collect {
                    _travel.value = it
                    _isLoadingContent.value = false
                }
        }

        viewModelScope.launch {
            _travel
                .flatMapLatest { travel ->
                    val userFlows = travel.applications.map { application ->
                        userModel.getUserById(application.user!!.id)
                    }
                    if (userFlows.isNotEmpty()) {
                        combine(userFlows) { usersList ->
                            usersList.zip(travel.applications) { user, application -> user to application }
                        }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { pairsList ->
                    _listOfApplications.value = pairsList
                    hasModified.value = false
                }
        }
    }


    fun cancel() {
        val travel = _travel.value
        val pairs = travel.applications.mapNotNull { application ->
            application.user!!.id.let { userId ->
                _listOfApplications.value.find { it.first.id == userId }?.first
                    ?.let { user -> user to application }
            }
        }

        _listOfApplications.value = pairs
        hasModified.value = false
    }

    suspend fun saveChanges(): Boolean {
        return model.updateApplications(_travel.value.id, _listOfApplications.value.map { it.second })

    }

    fun updateApplicationState(target: Application, newState: Int):Boolean {
        var modified = false
        val acceptedCount = _listOfApplications.value.filter { application ->
            State.entries[application.second.state] == State.ACCEPTED }.sumOf { application-> application.second.size }

        val updatedList = _listOfApplications.value.map { (user, application) ->
            if (application == target) {
                val canAccept = newState == State.ACCEPTED.ordinal &&
                        (_travel.value.size - acceptedCount > target.size - 1)

                val updatedApplication = if (newState == State.ACCEPTED.ordinal) {
                    if (canAccept) {
                        modified = true
                        application.copy(state = newState)
                    }
                    else {
                        application
                    }
                } else {
                    application.copy(state = newState)
                }
                user to updatedApplication
            } else {
                user to application
            }
        }
        _listOfApplications.value = updatedList
        hasModified.value = true
        return modified
    }

    fun toggleIsLoading(){
        _isLoadingChanges.value = !_isLoadingChanges.value
    }

}