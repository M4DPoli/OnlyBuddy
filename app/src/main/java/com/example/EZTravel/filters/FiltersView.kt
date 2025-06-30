package com.example.EZTravel.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.EZTravel.FiltersHighlightsSection
import com.example.EZTravel.HighlightsSection
import com.example.EZTravel.ManualMinusPlusCounter
import com.example.EZTravel.R
import com.example.EZTravel.travelPage.Highlights

//Slider position saver
private val floatRangeSaver: Saver<ClosedFloatingPointRange<Float>, *> = Saver(
    save = { range -> listOf(range.start, range.endInclusive) },
    restore = { list -> list[0]..list[1] })

//Modifiers
private val commonRowModifier = Modifier
    .padding(horizontal = 10.dp)
    .fillMaxWidth()
private val commonFirstColumnModifier =
    Modifier
        .padding(bottom = 8.dp)
        .defaultMinSize(minWidth = 80.dp, minHeight = 30.dp)
private val commonSecondColumnModifier = Modifier.padding(start = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersView(
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    filters: Filters,
    updateFilters: (Int, Int, ClosedFloatingPointRange<Float>, Set<Highlights>) -> Unit,
    resetFilters: () -> Unit
) {

    //Highlights BottomSheet
    val (showHighlightsBottomSheet, editShowHighlightsBottomSheet) = rememberSaveable {
        mutableStateOf(
            false
        )
    }
    val sheetHighlightState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    //Local
    val (sliderPosition, editSliderPosition) = rememberSaveable(stateSaver = floatRangeSaver) {
        mutableStateOf(
            filters.priceRange
        )
    }
    val (tripDuration, editTripDuration) = rememberSaveable { mutableIntStateOf(filters.tripDuration) }
    val (tripGroupSize, editTripGroupSize) = rememberSaveable { mutableIntStateOf(filters.tripGroupSize) }
    val (tripHighlights, editTripHighlights) = rememberSaveable { mutableStateOf(filters.tripHighlights) }

    FiltersContent(
        sliderPosition = sliderPosition,
        editSliderPosition = editSliderPosition,
        tripDuration = tripDuration,
        editTripDuration = editTripDuration,
        tripGroupSize = tripGroupSize,
        editTripGroupSize = editTripGroupSize,
        tripHighlights = tripHighlights,
        editShowHighlightsBottomSheet = editShowHighlightsBottomSheet,
        onConfirm = {
            updateFilters(tripDuration, tripGroupSize, sliderPosition, tripHighlights)
            onButtonClick()
        },
        onReset = {
            resetFilters()
            onButtonClick()
        },
    )

    if (showHighlightsBottomSheet) {
        val (tmpHighlights, editTmpHighlights) = rememberSaveable { mutableStateOf(tripHighlights) }

        ModalBottomSheet(
            onDismissRequest = {
                editShowHighlightsBottomSheet(false)
            }, sheetState = sheetHighlightState
        ) {
            HighlightsFilterContent(
                tmpHighlights,
                editTmpHighlights,
                editTripHighlights,
                editShowHighlightsBottomSheet,
                modifier
            )
        }
    }

}


@Composable
fun FiltersContent(
    sliderPosition: ClosedFloatingPointRange<Float>,
    editSliderPosition: (ClosedFloatingPointRange<Float>) -> Unit,
    tripDuration: Int,
    editTripDuration: (Int) -> Unit,
    tripGroupSize: Int,
    editTripGroupSize: (Int) -> Unit,
    tripHighlights: Set<Highlights>,
    editShowHighlightsBottomSheet: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
//            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Text(
                text = stringResource(R.string.filters_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.filters_price_range),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "€${sliderPosition.start.toInt()} - ${
                            if (sliderPosition.endInclusive == 2500f) "€2500+" else "€${sliderPosition.endInclusive.toInt()}"
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                RangeSlider(
                    value = sliderPosition,
                    steps = 4,
                    onValueChange = editSliderPosition,
                    valueRange = 0f..2500f,
                )
            }
        }

        item {
            FilterRow(
                title = stringResource(R.string.filters_duration),
                content = {
                    ManualMinusPlusCounter(
                        tripDuration,
                        onMinusClick = {
                            if (tripDuration > 0) editTripDuration(tripDuration - 1)
                        },
                        onPlusClick = { editTripDuration(tripDuration + 1) },
                        coreText = R.plurals.filters_duration_day,
                        disabledOnZeroValue = true
                    )
                }
            )
        }

        item {
            FilterRow(
                title = stringResource(R.string.filters_group_size),
                content = {
                    ManualMinusPlusCounter(
                        tripGroupSize,
                        onMinusClick = {
                            if (tripGroupSize > 0) editTripGroupSize(tripGroupSize - 1)
                        },
                        onPlusClick = { editTripGroupSize(tripGroupSize + 1) },
                        coreIcon = ImageVector.vectorResource(R.drawable.ic_person_filled),
                        disabledOnZeroValue = true
                    )
                }
            )
        }

        item {
            FilterRow(
                title = stringResource(R.string.filters_type),
                content = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (tripHighlights.isNotEmpty()) {
                            HighlightsSection(tripHighlights.map { it.ordinal })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalIconButton(
                            onClick = { editShowHighlightsBottomSheet(true) }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled),
                                contentDescription = "Edit",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.button_reset))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.button_apply))
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            content()
        }
    }
}

@Composable
fun HighlightsFilterContent(
    tmpHighlights: Set<Highlights>,
    editTmpHighlights: (Set<Highlights>) -> Unit,
    editTripHighlights: (Set<Highlights>) -> Unit,
    editShowHighlightsBottomSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Row(
                modifier = commonRowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.highlights_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        item {
            Row(
                modifier = commonRowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FiltersHighlightsSection(
                    highlights = tmpHighlights, onClick = { selected ->
                        val tmpH = if (tmpHighlights.contains(selected)) tmpHighlights.minus(
                            selected
                        )
                        else tmpHighlights.plus(selected)
                        editTmpHighlights(tmpH)
                    })
            }
        }

        item {
            Row(
                modifier = commonRowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    editTripHighlights(tmpHighlights)
                    editShowHighlightsBottomSheet(false)
                }) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_check_small),
                        contentDescription = null
                    )
                    Text(stringResource(R.string.button_apply))
                }
            }
        }


    }
}