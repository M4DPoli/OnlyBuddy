package com.example.EZTravel.filters

import com.example.EZTravel.travelPage.Highlights

data class Filters(
    val isActive: Boolean = false,
    val priceRange: ClosedFloatingPointRange<Float> = 0f..2500f,
    val tripDuration: Int = 0,
    val tripGroupSize: Int = 0,
    val tripHighlights: Set<Highlights> = setOf()
)