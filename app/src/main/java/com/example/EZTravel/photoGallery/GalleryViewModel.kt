package com.example.EZTravel.photoGallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val model: TravelModel,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle[EZTravelDestinationsArgs.TRAVEL_ID])

    private val _travel = MutableStateFlow(Travel())
    val travel: StateFlow<Travel> = _travel

    init {
        viewModelScope.launch {
            model.getTravelById(id).collect {
                _travel.value = it
            }
        }
    }

    private val _selectedImageIndex = MutableStateFlow<Int?>(null)
    val selectedImageIndex: MutableStateFlow<Int?> = _selectedImageIndex

    fun selectImage(index: Int) {
        _selectedImageIndex.value = index
    }

    fun clearSelection() {
        _selectedImageIndex.value = null
    }
}