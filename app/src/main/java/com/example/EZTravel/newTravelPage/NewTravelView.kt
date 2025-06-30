package com.example.EZTravel.newTravelPage

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.Carousel
import com.example.EZTravel.HighlightsSection
import com.example.EZTravel.ManualMinusPlusCounter
import com.example.EZTravel.R
import com.example.EZTravel.travelPage.Activity
import com.example.EZTravel.travelPage.Highlights
import com.example.EZTravel.travelPage.Travel
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Preview
@Composable
fun NewTravelViewPreview() {
    NewTravelView({}, {}, {}, false)
}


fun formatTime(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}


@Composable
fun NewTravelView(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSnack: (String) -> Unit,
    create: Boolean,
    vm: NewTravelViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val travel by vm.travel.collectAsState()
    val isLoading by vm.isLoadingContent.collectAsState()
    val showDialog = rememberSaveable { mutableStateOf(false) }
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            bottomBar = {
                BottomBar(showDialog, create, onSave, onSnack, coroutineScope, context)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Carousel(
                    travel.images,
                    onBack = onBack,
                    onAdd = { uri -> vm.addImage(uri) },
                    onRemove = { index -> vm.removeImage(index) },
                    onClone = {},
                    true
                )

                GeneralInfo()
                Spacer(modifier = Modifier.height(24.dp))
                PriceRange()
                Spacer(modifier = Modifier.height(24.dp))
                GroupSize()
                Spacer(modifier = Modifier.height(24.dp))
                TravelHighLights()
                Spacer(modifier = Modifier.height(24.dp))
                ItinerarySection()
                if (showDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showDialog.value = false },
                        title = { Text("Delete confirmation") },
                        text = { Text("Are you sure to delete this trip?") },
                        confirmButton = {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    vm.toggleIsLoading()
                                    val success = vm.deleteTravel(context)
                                    if (success) {
                                        onSnack("Travel deleted successfully!");onSave()
                                    } else {
                                        onSnack("There was an error deleting the travel, try again")
                                        vm.toggleIsLoading()
                                    }
                                }
                                showDialog.value = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

            }

        }
    }
}

@Composable
fun BottomBar(
    showDialog: MutableState<Boolean>,
    create: Boolean,
    onSave: () -> Unit,
    onSnack: (String) -> Unit,
    coroutineScope: CoroutineScope,
    context: Context,
    vm: NewTravelViewModel = hiltViewModel()
) {
    BottomAppBar(
        containerColor = AppBarColors.BottomBarContainerColor(),
        contentColor = AppBarColors.BottomBarContentColor(),
        tonalElevation = AppBarColors.BottomBarTonalElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                if (vm.validateTravelFields()) {
                    if (create) {
                        coroutineScope.launch {
                            vm.toggleIsLoading()
                            val success = vm.addTravel(context)
                            if (success) {
                                onSnack("Travel added successfully");onSave()
                            } else {
                                vm.toggleIsLoading()
                                onSnack ("Error adding travel")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            vm.toggleIsLoading()
                            val success = vm.updateTravel(context)
                            if (success) { onSnack("Travel modified successfully");onSave()}
                            else {
                                vm.toggleIsLoading()
                                onSnack ("Error modifiyng travel")
                            }
                        }
                    }
                }
            }, modifier = Modifier.padding(8.dp)) {
                Icon(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .size(18.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_small),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(end = 24.dp),
                    text = stringResource(R.string.profile_page_edit_save)
                )
            }
            if (!create) {
                Button(
                    onClick = {
                        showDialog.value = true
                    },
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .size(18.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(end = 24.dp),
                        text = stringResource(R.string.edit_travel_page_delete)
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralInfo(vm: NewTravelViewModel = hiltViewModel()) {
    val travel by vm.travel.collectAsState()
    val showPicker = remember { mutableStateOf(false) }

    val titleError by vm.titleError.collectAsState()
    val descriptionError by vm.descriptionError.collectAsState()
    val locationError by vm.locationError.collectAsState()
    val dateRangeError by vm.dateRangeError.collectAsState()

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.new_travel_page_general_info),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Title
        LabeledTextField(
            value = travel.title,
            onValueChange = vm::setTravelTitle,
            label = stringResource(R.string.new_travel_page_title),
            placeholder = "Insert travel title",
            error = titleError,
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        LabeledTextField(
            value = travel.description,
            onValueChange = vm::setTravelDescription,
            label = stringResource(R.string.new_travel_page_description),
            placeholder = "Insert description",
            error = descriptionError,
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences,
            singleLine = false,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Location Picker
        LocationPicker(travel, locationError, vm)

        if (locationError.isNotBlank()) {
            ErrorText(locationError)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Range Picker
        DateRangePicker(travel, dateRangeError, showPicker, formatter)

        if (dateRangeError.isNotBlank()) {
            ErrorText(dateRangeError)
        }

        if (showPicker.value) {
            DateRangePickerModal(
                onDateRangeSelected = { (start, end) ->
                    if (start != null && end != null) {
                        vm.setTravelDateRange(Pair(Date(start), Date(end)))
                    }
                },
                onDismiss = { showPicker.value = false },
                travel = travel
            )
        }
    }
}

@Composable
fun DateRangePicker(
    travel: Travel,
    dateRangeError: String,
    showPicker: MutableState<Boolean>,
    formatter: SimpleDateFormat
) {
    OutlinedButton(
        onClick = { showPicker.value = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (dateRangeError.isEmpty()) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.error
        }),
        colors = if (dateRangeError.isEmpty()) {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        },
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (travel.dateStart.before(Date())) {
                    "Date range"
                } else {
                    "From ${formatter.format(travel.dateStart)} to ${formatter.format(travel.dateEnd)}"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String = "",
    imeAction: ImeAction = ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = error.isNotBlank(),
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = imeAction,
            capitalization = capitalization,
            keyboardType = KeyboardType.Text
        ),
        shape = MaterialTheme.shapes.medium
    )
    if (error.isNotBlank()) {
        ErrorText(error)
    }
}

@Composable
fun ErrorText(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
    )
}

@Composable
fun PriceRange(vm: NewTravelViewModel = hiltViewModel()) {
    val travel by vm.travel.collectAsState()
    val from by vm.from.collectAsState()
    val to by vm.to.collectAsState()
    val priceRangeError by vm.priceRangeError.collectAsState()

    var priceAsRange by remember { mutableStateOf(travel.priceStart != travel.priceEnd) }

    fun sanitizePrice(price: String): String {
        val sanitized = price.replace(",", ".")
        return if (sanitized.matches(Regex("^\\d*(\\.\\d{0,2})?\$"))) {
            sanitized
        } else "0.0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.new_travel_page_price_range),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Insert as range",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = priceAsRange,
                onCheckedChange = {
                    priceAsRange = it
                    if (!it) vm.setTo(from)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (priceAsRange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = from,
                    placeholder = { Text("From") },
                    onValueChange = { vm.setFrom(sanitizePrice(it)) },
                    label = { Text("From") },
                    isError = priceRangeError.isNotBlank(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = to,
                    placeholder = { Text("To") },
                    onValueChange = { vm.setTo(sanitizePrice(it)) },
                    label = { Text("To") },
                    isError = priceRangeError.isNotBlank(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }
        } else {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = from,
                placeholder = { Text("Price") },
                onValueChange = {
                    val sanitized = sanitizePrice(it)
                    vm.setFrom(sanitized)
                    vm.setTo(sanitized)
                },
                label = { Text("Price") },
                isError = priceRangeError.isNotBlank(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                shape = MaterialTheme.shapes.medium
            )
        }

        if (priceRangeError.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = priceRangeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModal(
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit,
    travel: Travel
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = travel.dateStart.time,
        initialSelectedEndDateMillis = travel.dateEnd.time
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        Pair(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {

        DateRangePicker(
            state = dateRangePickerState,
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

    }
}

@Composable
fun GroupSize(vm: NewTravelViewModel = hiltViewModel()) {

    val travel by vm.travel.collectAsState()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.filters_group_size),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ManualMinusPlusCounter(
            coreValue = travel.size,
            onMinusClick = { vm.decreaseGroup() },
            onPlusClick = { vm.increaseGroup() },
            modifier = Modifier.padding(horizontal = 32.dp),
            disabledOnZeroValue = false,
            coreIcon = ImageVector.vectorResource(R.drawable.ic_group),
            coreText = R.plurals.new_travel_people
        )
    }
}

@Composable
fun TravelHighLights(vm: NewTravelViewModel = hiltViewModel()) {
    val travel by vm.travel.collectAsState()
    val showBottomSheet = remember { mutableStateOf(false) }
    val highlightsError by vm.highlightsError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.new_travel_page_highlights),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showBottomSheet.value = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled),
                    contentDescription = "Edit highlights",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.edit),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HighlightsSection(travel.highlights)

        if (highlightsError.isNotBlank()) {
            Text(
                text = highlightsError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            )
        }
    }

    if (showBottomSheet.value) {
        HighlightsModal(travel, showBottomSheet, vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsModal(
    travel: Travel,
    showBottomSheet: MutableState<Boolean>,
    vm: NewTravelViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { showBottomSheet.value = false },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Highlights",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Highlights are a great way to show others what your travel is about. Pick a few that best describe your trip.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Highlights.entries.forEach { h ->
                    val isSelected = travel.highlights.contains(h.ordinal)
                    FilterChip(
                        label = { Text(LocalContext.current.getString(h.tag)) },
                        selected = isSelected,
                        onClick = {
                            val newHighlights = if (isSelected) {
                                travel.highlights.minus(h.ordinal)
                            } else {
                                travel.highlights.plus(h.ordinal)
                            }
                            vm.setHighlights(newHighlights)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(h.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${travel.highlights.size}/4 selected",
                style = MaterialTheme.typography.labelMedium,
                color = if (travel.highlights.size > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet.value = false
                        }
                    }
                },
                enabled = travel.highlights.size in 1..4,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Done",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ItinerarySection(vm: NewTravelViewModel = hiltViewModel()) {
    val itineraryError by vm.itineraryError.collectAsState()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.new_travel_page_itinerary),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (itineraryError.isNotBlank()) {
            Text(
                text = itineraryError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Itinerary(true)
    }
}

//=^.^=

@Composable
fun Itinerary(
    editMode: Boolean,
    vm: NewTravelViewModel = hiltViewModel()
) {
    val travel by vm.travel.collectAsState()
    val map by vm.tempMap.collectAsState()

    val totalDays = travel.days

    map.forEach { (day, activities) ->
        ItineraryItem(day, activities, day == totalDays, editMode)
    }
}

@Composable
fun ItineraryItem(
    day: Int,
    activities: List<Activity>,
    isLastDay: Boolean,
    editMode: Boolean,
    vm: NewTravelViewModel = hiltViewModel()
) {
    val expanded = rememberSaveable { mutableStateOf(true) }
    val cardHeight = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val circleColor = MaterialTheme.colorScheme.primary
    val showBottomSheet = remember { mutableStateOf(false) }
    val currentIndex = remember { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline decoration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                drawCircle(color = circleColor)
            }

            if (!isLastDay) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(maxOf(60.dp, cardHeight.value))
                        .background(
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .onSizeChanged {
                    cardHeight.value = with(density) { it.height.toDp() }
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.travel_page_itinerary_day, day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded.value) {
                            ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_up)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_down)
                        },
                        contentDescription = null,
                        modifier = Modifier.clickable { expanded.value = !expanded.value }
                    )
                }

                AnimatedVisibility(visible = expanded.value) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        activities.forEachIndexed { index, activity ->
                            ActivityItem(
                                activity = activity,
                                editMode = editMode,
                                editActivity = { shouldOpen ->
                                    showBottomSheet.value = shouldOpen
                                    vm.resetActivity()
                                    vm.resetActivityErrors()
                                    if (shouldOpen) {
                                        vm.fetchActivity(day, index)
                                        currentIndex.intValue = index
                                    }
                                }
                            )
                        }

                        if (editMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    showBottomSheet.value = true
                                    vm.resetActivity()
                                    vm.resetActivityErrors()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                                    contentDescription = "Add activity",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Activity")
                            }
                        }
                    }
                }
            }
        }

        // Modal for editing/adding activities
        if (showBottomSheet.value) {
            ActivityModal(day, currentIndex, showBottomSheet, vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityModal(
    day: Int,
    currentIndex: MutableIntState,
    showBottomSheet: MutableState<Boolean>,
    vm: NewTravelViewModel = hiltViewModel()
) {
    val activityTitleError by vm.activityTitleError.collectAsState()
    val activityDescriptionError by vm.activityDescriptionError.collectAsState()
    val timeError by vm.timeError.collectAsState()
    val suggestionError by vm.suggestionError.collectAsState()

    val vmActivity by vm.activity.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val showStartPicker = remember { mutableStateOf(false) }
    val showEndPicker = remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            showBottomSheet.value = false
        }, sheetState = sheetState
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "New activity",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            //Activity name
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = vmActivity.title,
                    placeholder = { Text("Insert travel title") },
                    onValueChange = { vm.setActivityName(it) },
                    label = { Text(stringResource(R.string.new_travel_page_title)) },
                    isError = activityTitleError.isNotBlank(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                if (activityTitleError.isNotBlank()) {
                    Text(
                        text = activityTitleError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                //Description
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = vmActivity.description,
                    placeholder = { Text("Enter description") },
                    onValueChange = { vm.setDescription(it) },
                    label = { Text(stringResource(R.string.new_travel_page_description)) },
                    isError = activityDescriptionError.isNotBlank(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                if (activityDescriptionError.isNotBlank()) {
                    Text(
                        text = activityDescriptionError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    //Start time
                    Box(
                        modifier = Modifier
                            .clickable(onClick = {
                                showStartPicker.value = true
                            })
                            .weight(1f)
                            .padding(),
                    ) {
                        OutlinedTextField(
                            value = formatTime(vmActivity.timeStart),
                            placeholder = { Text("Insert start time") },
                            onValueChange = { },
                            label = { Text("From") },
                            isError = false,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = LocalContentColor.current,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                    if (showStartPicker.value)
                        ActivityTimePicker(onConfirm = { start ->
                            vm.setStartTime(((start.hour * 3600 + start.minute * 60) * 1000).toLong());showStartPicker.value =
                            false
                        }, onDismiss = { showStartPicker.value = false })

                    Spacer(modifier = Modifier.weight(0.2f))


                    //End time
                    Box(
                        modifier = Modifier
                            .clickable(onClick = {
                                showEndPicker.value = true
                            })
                            .weight(1f)
                    ) {
                        OutlinedTextField(
                            value = formatTime(vmActivity.timeEnd),
                            placeholder = { Text("Insert end time") },
                            onValueChange = { },
                            label = { Text("To") },
                            isError = false,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = LocalContentColor.current,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    }



                    if (showEndPicker.value)
                        ActivityTimePicker(onConfirm = { end ->
                            vm.setEndTime(((end.hour * 3600 + end.minute * 60) * 1000).toLong());showEndPicker.value =
                            false
                        }, onDismiss = { showEndPicker.value = false })

                }

                if (timeError.isNotBlank()) {
                    Text(
                        text = timeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            item {
                //Mandatory switch
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Switch(
                            checked = vmActivity.mandatory,
                            onCheckedChange = { vm.setMandatory(it) }
                        )
                        Text(
                            text = "Mandatory",
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .padding(horizontal = 5.dp)
                        )
                    }
                }
            }

            item {
                //Suggestions
                if (!vmActivity.mandatory) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = vmActivity.suggestedActivities,
                        placeholder = { Text("Enter suggested activities") },
                        onValueChange = { vm.setSuggestions(it) },
                        label = { Text("Suggested activities") },
                        isError = suggestionError.isNotBlank(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    if (suggestionError.isNotBlank()) {
                        Text(
                            text = suggestionError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }

            item {
                //Add button
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vmActivity.id == "") {
                        Button(
                            onClick = {
                                if (vm.validateActivityFields()) {
                                    vm.addActivity(day, vmActivity)
                                    showBottomSheet.value = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                                contentDescription = "Add  Or edit button"
                            )
                            Text("Add", modifier = Modifier.padding(8.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                vm.removeActivity(day, currentIndex.intValue)
                                showBottomSheet.value = false


                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)

                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                                contentDescription = "Delete activity"
                            )

                            Text(
                                text = "Delete",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Button(
                            onClick = {
                                if (vm.validateActivityFields()) {
                                    vm.editActivity(vmActivity, day, currentIndex.intValue)
                                    showBottomSheet.value = false
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                                contentDescription = "Edit activity"
                            )
                            Text(
                                text = "Save",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTimePicker(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {

    val timePickerState = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 0,
        is24Hour = true,
    )

    TimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm(timePickerState) }
    ) {
        TimePicker(
            state = timePickerState,
        )
    }
}


@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
    )
}

@Composable
fun ActivityItem(
    activity: Activity,
    editMode: Boolean,
    editActivity: (showBottomSheet: Boolean) -> (Unit)
) {
    val expanded = rememberSaveable { mutableStateOf(false) }
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = !expanded.value }
        ) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded.value) {
                    ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_up)
                } else {
                    ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_down)
                },
                contentDescription = "Toggle icon",
                modifier = Modifier.clickable { expanded.value = !expanded.value })
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    //Info column
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = activity.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_schedule),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "${activity.timeStart?.let { formatter.format(it) }} - ${
                                    activity.timeEnd?.let {
                                        formatter.format(
                                            it
                                        )
                                    }
                                }",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_info_filled),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = if (activity.mandatory)
                                    stringResource(R.string.activity_mandatory)
                                else
                                    stringResource(R.string.activity_optional),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (!activity.mandatory && activity.suggestedActivities.isNotBlank()) {
                            Text(
                                text = activity.suggestedActivities,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    //Column for edit button
                    if (editMode) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.Bottom)
                        ) {
                            IconButton(
                                onClick = { editActivity(true) },
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled),
                                    contentDescription = "Edit activity"
                                )
                            }
                        }
                    }

                }

            }
        }
    }
}

@Composable
fun LocationPicker(travel: Travel, locationError: String, viewModel: NewTravelViewModel) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { intent ->
                val place = Autocomplete.getPlaceFromIntent(intent)
                viewModel.setTravelLocation(place)
            }
        }
    }

    val autocompleteIntent = remember {
        Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        ).build(context)
    }

    OutlinedButton(
        onClick = { launcher.launch(autocompleteIntent) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (locationError.isEmpty()) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.error
        }),
        colors = if (locationError.isEmpty()) {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        },
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = travel.location?.name ?: "Location",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}