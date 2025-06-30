package com.example.EZTravel.filters

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import com.example.EZTravel.travelPage.Highlights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class FiltersViewModel : ViewModel() {

    protected val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    val searchFieldState = TextFieldState()

    fun updateFilter(
        duration: Int,
        size: Int,
        range: ClosedFloatingPointRange<Float>,
        highlights: Set<Highlights>
    ) {
        val isActive = !(range == 0f..2500f && duration == 0 && size == 0 && highlights.isEmpty())
        _filters.value = Filters(isActive, range, duration, size, highlights)
    }

    fun resetFilter() {
        _filters.value = Filters()
    }

    fun clearSearchFieldState() {
        searchFieldState.setTextAndPlaceCursorAtEnd("")
    }
}