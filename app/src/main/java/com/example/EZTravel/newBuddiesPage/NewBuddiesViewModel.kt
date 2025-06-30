package com.example.EZTravel.newBuddiesPage

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.travelPage.Application
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewBuddiesViewModel @Inject constructor(
    private val model: TravelModel,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle[EZTravelDestinationsArgs.TRAVEL_ID])

    private val _travel = MutableStateFlow(Travel())
    val travel: StateFlow<Travel> = _travel

    val buddies_name = mutableStateListOf<String>()
    val buddies_surname = mutableStateListOf<String>()
    val errors_name = mutableStateListOf<Boolean>()
    val errors_surname = mutableStateListOf<Boolean>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun toggleIsLoading(){
        _isLoading.value = !_isLoading.value
    }

    fun initBuddies(n: Int) {
        if (buddies_name.size != n - 1) {
            repeat(n - 1) {
                buddies_name.add("")
                buddies_surname.add("")
                errors_name.add(false)
                errors_surname.add(false)
            }
        }
    }

    init {
        viewModelScope.launch {
            model.getTravelById(id).collect {
                _travel.value = it
            }
        }
    }

    fun validate(n: Int): Boolean {
        var allValid = true
        for (i in 0 until n - 1) {
            val valid = buddies_name[i].isNotBlank()
            errors_name[i] = !valid
            if (!valid) allValid = false
            val valid_surname = buddies_surname[i].isNotBlank()
            errors_surname[i] = !valid_surname
            if (!valid_surname) allValid = false
        }
        return allValid
    }

    fun getFullNames(): List<String> {
        return buddies_name.zip(buddies_surname) { name, surname ->
            "$name $surname".trim()
        }
    }


    suspend fun addNewApplications(application: Application): Boolean {
        return model.addApplication(application, _travel.value.id)
    }
}