package com.example.EZTravel.newTravelPage

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.travelPage.Activity
import com.example.EZTravel.travelPage.Location
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.example.EZTravel.travelPage.adaptTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs
import com.example.EZTravel.userProfile.UserProfileModel
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.UUID


@HiltViewModel
class NewTravelViewModel @Inject constructor(
    private val model: TravelModel,
    private val userModel: UserProfileModel,
    userManager: AuthUserManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: String = savedStateHandle[EZTravelDestinationsArgs.TRAVEL_ID] ?: ""

    private val currentUser = userManager.currentUser

    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent

    private val _travel = MutableStateFlow(Travel(size = 2))
    val travel: StateFlow<Travel> = _travel

    //Temporary field for map
    val tempMap = MutableStateFlow<MutableMap<Int, List<Activity>>>(mutableMapOf())

    //Temporary fields to store price range as string + function to set them from existing travel
    val from = MutableStateFlow("0.0")
    fun setFrom(s: String) {
        from.value = s
    }

    val to = MutableStateFlow("0.0")
    fun setTo(s: String) {
        to.value = s
    }

    private fun getStringPrice(priceStart: Double, priceEnd: Double) {
        from.value = priceStart.toString()
        to.value = priceEnd.toString()
    }

    init {
        if (id != "") {
            viewModelScope.launch {
                model.getTravelById(id).collect {
                    _travel.value = it
                    getStringPrice(it.priceStart, it.priceEnd)
                    val startLocalDate =
                        it.dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

                    val activitiesByDay: Map<Int, List<Activity>> = it.itinerary
                        .groupBy { activity ->
                            val activityDate =
                                activity.date.toInstant().atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            ChronoUnit.DAYS.between(startLocalDate, activityDate).toInt() + 1
                        }
                        .mapValues { entry ->
                            entry.value.sortedBy { activity ->
                                activity.timeStart!!.toInstant()
                            }
                        }
                        .toSortedMap()
                    tempMap.value = activitiesByDay.toMutableMap()
                }
            }
        }
    }

    fun toggleIsLoading(){
        _isLoadingContent.value = !_isLoadingContent.value
    }
    suspend fun addTravel(context: Context): Boolean {
        _travel.value = _travel.value.copy(itinerary = tempMap.value.values.flatten())
        val id = model.addTravel(_travel.value, context)
        if (id == null) return false
        return true
    }

    suspend fun updateTravel(context: Context): Boolean {
        _travel.value = _travel.value.copy(itinerary = tempMap.value.values.flatten())
        return model.updateTravel(_travel.value, context)
    }

    fun addImage(image: Uri) {
        val tmp = _travel.value.images.toMutableList()
        tmp.add(image.toString())
        _travel.value = _travel.value.copy(images = tmp)
    }

    fun removeImage(index: Int) {
        if (index >= _travel.value.images.size) {
            val tmp = _travel.value.images.toMutableList()
            tmp.removeAt(index - _travel.value.images.size)
            _travel.value = _travel.value.copy(images = tmp)
        } else {
            val tmp = _travel.value.images.toMutableList()
            tmp.removeAt(index)
            _travel.value = _travel.value.copy(images = tmp)
        }

    }

    //Logic to edit travel object
    fun setTravelTitle(title: String) {
        _travel.value = _travel.value.copy(title = title)
    }

    fun setTravelDescription(description: String) {
        _travel.value = _travel.value.copy(description = description)
    }

    fun setTravelLocation(place: Place) {
        val newLocation = Location(
            name = place.name ?: "",
            placeId = place.id,
            geoPoint = place.latLng?.let { GeoPoint(it.latitude, it.longitude) }
        )
        _travel.value = _travel.value.copy(location = newLocation)
    }

    fun setTravelDateRange(dateRange: Pair<Date, Date>) {
        val days =
            (abs((dateRange.second.time - dateRange.first.time)) / (1000 * 60 * 60 * 24)).toInt() + 1
        val map = tempMap.value.toMutableMap()
        if (map.size < days) {
            for (i in map.size + 1..days)
                if (!map.containsKey(i)) {
                    map[i] = listOf()
                }
        } else {
            for (i in map.size downTo days + 1) {
                map.remove(i)
            }
        }
        tempMap.value = map
        _travel.value =
            _travel.value.copy(dateStart = dateRange.first, dateEnd = dateRange.second, days = days)
        updateActivitiesDates()
    }

    private fun updateActivitiesDates() {
        val startDate = _travel.value.dateStart
        val updatedMap = tempMap.value.mapValues { (day, activities) ->
            activities.map { activity ->
                val updatedDate = calculateDateForDay(startDate, day)


                fun updateDateKeepTime(oldDateTime: Date, newDate: Date): Date {
                    val calOld = Calendar.getInstance().apply { time = oldDateTime }
                    val calNew = Calendar.getInstance().apply { time = newDate }

                    calNew.set(Calendar.HOUR_OF_DAY, calOld.get(Calendar.HOUR_OF_DAY))
                    calNew.set(Calendar.MINUTE, calOld.get(Calendar.MINUTE))
                    calNew.set(Calendar.SECOND, calOld.get(Calendar.SECOND))
                    calNew.set(Calendar.MILLISECOND, calOld.get(Calendar.MILLISECOND))

                    return calNew.time
                }

                activity.copy(
                    date = updatedDate,
                    timeStart = updateDateKeepTime(activity.timeStart ?: updatedDate, updatedDate),
                    timeEnd = updateDateKeepTime(activity.timeEnd ?: updatedDate, updatedDate)
                )
            }
        }.toMutableMap()

        tempMap.value = updatedMap
    }

    private fun calculateDateForDay(startDate: Date, day: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = startDate
        cal.add(Calendar.DAY_OF_MONTH, day - 1)
        return cal.time
    }


    fun setTravelPriceRange(priceRange: Pair<Double, Double>) {
        _travel.value =
            travel.value.copy(priceStart = priceRange.first, priceEnd = priceRange.second)
    }

    fun increaseGroup() {
        _travel.value = _travel.value.copy(size = travel.value.size + 1)
    }

    fun decreaseGroup() {
        if (_travel.value.size > 2) _travel.value =
            _travel.value.copy(size = _travel.value.size - 1)
    }

    fun setHighlights(highlights: List<Int>) {
        _travel.value = travel.value.copy(highlights = highlights)
    }


    //Error values
    val titleError = MutableStateFlow("")
    val descriptionError = MutableStateFlow("")
    val locationError = MutableStateFlow("")
    val dateRangeError = MutableStateFlow("")
    val priceRangeError = MutableStateFlow("")
    val highlightsError = MutableStateFlow("")
    val itineraryError = MutableStateFlow("")

    fun validateTravelFields(): Boolean {
        titleError.value = ""
        descriptionError.value = ""
        locationError.value = ""
        dateRangeError.value = ""
        priceRangeError.value = ""
        highlightsError.value = ""
        itineraryError.value = ""

        val price_from = from.value.toDoubleOrNull()
        val price_to = to.value.toDoubleOrNull()

        var valid = true

        val emptyDays: MutableList<Int> = mutableListOf()

        if (_travel.value.title.length < 5 || _travel.value.title.length > 40) {
            valid = false
            titleError.value = "Title length must be between 5 and 40 characters"
        }
        if (_travel.value.description.length < 10 || _travel.value.description.length > 500) {
            valid = false
            descriptionError.value = "Description must be between 10 and 500 characters"
        }
        if (_travel.value.location?.placeId == null || _travel.value.location?.name?.isBlank() == true) {
            valid = false
            locationError.value = "Location name must be between 2 and 30 characters"
        }
        if (_travel.value.dateStart.before(Date())) {
            valid = false
            dateRangeError.value = "Please pick a date range"
        }

        if (price_from == null || price_to == null) {
            valid = false
            priceRangeError.value = "Please select valid values for prices"
        } else if (price_from > price_to) {
            valid = false
            priceRangeError.value = "Please select a valid price range"
        } else if (price_from < 0 || price_to < 0) {
            valid = false
            priceRangeError.value = "Please select a valid positive values for price"
        } else {
            setTravelPriceRange(Pair(price_from, price_to))
        }

        if (_travel.value.highlights.isEmpty() || _travel.value.highlights.size > 4) {
            valid = false
            highlightsError.value = "Please select 1 to 4 highlights"
        }

        if (_travel.value.days > 0) {
            if (tempMap.value.size != travel.value.days) {
                valid = false
                itineraryError.value = "Itinerary must have the same stops as the number of days"
            }

            tempMap.value.forEach {
                if (it.value.isEmpty()) {
                    valid = false
                    emptyDays.add(it.key)
                }
            }

            if (emptyDays.size == 1) {
                itineraryError.value =
                    "Day ${emptyDays[0]} has no activities, please provide at least one"
            } else if (emptyDays.size > 1) {
                itineraryError.value =
                    "Days ${emptyDays.joinToString(", ")} have no activities, please provide at least one"
            }
        }

        for (i in tempMap.value.keys) {
            for (j in 0 until tempMap.value[i]!!.size) {
                val current = tempMap.value[i]!![j]

                if (j < tempMap.value[i]!!.size - 1) {
                    val next = tempMap.value[i]!![j + 1]
                    if (current.timeEnd!!.time > next.timeStart!!.time) {
                        valid = false
                        itineraryError.value =
                            "Invalid schedule in day $i, check the activities and their start and end time"
                    }
                }
            }
        }

        return valid
    }

    /*
    Activity related stuff
     */

    val activity = MutableStateFlow(Activity())

    //Activity errors
    val activityTitleError = MutableStateFlow("")
    val activityDescriptionError = MutableStateFlow("")
    val timeError = MutableStateFlow("")
    val suggestionError = MutableStateFlow("")

    fun resetActivityErrors() {
        activityTitleError.value = ""
        activityDescriptionError.value = ""
        timeError.value = ""
        suggestionError.value = ""
    }

    fun validateActivityFields(): Boolean {
        activityTitleError.value = ""
        activityDescriptionError.value = ""
        timeError.value = ""
        suggestionError.value = ""

        var valid = true

        if (activity.value.title.length < 3 || activity.value.title.length > 60) {
            valid = false
            activityTitleError.value = "Activity title must be between 3 and 60 characters"
        }
        if (activity.value.description.length < 5 || activity.value.description.length > 100) {
            valid = false
            activityDescriptionError.value =
                "Activity description must be between 5 and 100 characters"
        }
        if (activity.value.timeStart == null || activity.value.timeStart == null) {
            valid = false
            timeError.value = "Please select both start and end time"
        } else if (activity.value.timeStart!!.time > activity.value.timeEnd!!.time) {
            valid = false
            timeError.value = "Please select a valid start and end time"
        } else if (activity.value.timeEnd!!.time - activity.value.timeStart!!.time < 30 * 60 * 1000) {
            valid = false
            timeError.value = "Activities must be at least 30 minutes long"
        }
        if (!activity.value.mandatory) {
            if (activity.value.suggestedActivities.length < 10 || activity.value.suggestedActivities.length > 50) {
                suggestionError.value = "Activity suggestions must be between 10 and 50 characters"
            }
        }

        return valid

    }

    fun addActivity(day: Int, activity: Activity) {
        val calendar = Calendar.getInstance()
        calendar.time = _travel.value.dateStart
        calendar.add(Calendar.DAY_OF_MONTH, day - 1)

        val activityDate = calendar.time
        fun setTimeFrom(old: Date, baseDate: Date): Date {
            val calTime = Calendar.getInstance()
            calTime.time = old

            val calDate = Calendar.getInstance()
            calDate.time = baseDate

            calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY))
            calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE))
            calDate.set(Calendar.SECOND, calTime.get(Calendar.SECOND))
            calDate.set(Calendar.MILLISECOND, 0)

            return calDate.time
        }

        val temp_activity_list = tempMap.value[day]!!.toMutableList()
        val newActivity = activity.copy(
            id = UUID.randomUUID().toString(),
            date = activityDate,
            timeStart = setTimeFrom(activity.timeStart!!, activityDate),
            timeEnd = setTimeFrom(activity.timeEnd!!, activityDate)
        )
        temp_activity_list.add(newActivity)
        val temp_days_map = tempMap.value.toMutableMap()
        temp_days_map[day] = temp_activity_list
        tempMap.value = temp_days_map
    }

    fun editActivity(activity: Activity, day: Int, index: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = _travel.value.dateStart
        calendar.add(Calendar.DAY_OF_MONTH, day - 1)

        val activityDate = calendar.time
        fun setTimeFrom(old: Date, baseDate: Date): Date {
            val calTime = Calendar.getInstance()
            calTime.time = old

            val calDate = Calendar.getInstance()
            calDate.time = baseDate

            calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY))
            calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE))
            calDate.set(Calendar.SECOND, calTime.get(Calendar.SECOND))
            calDate.set(Calendar.MILLISECOND, 0)

            return calDate.time
        }

        val newActivity = activity.copy(
            date = activityDate,
            timeStart = setTimeFrom(activity.timeStart!!, activityDate),
            timeEnd = setTimeFrom(activity.timeEnd!!, activityDate)
        )
        val temp_activity_list = tempMap.value[day]!!.toMutableList()
        temp_activity_list[index] = newActivity
        val temp_days_map = tempMap.value.toMutableMap()
        temp_days_map[day] = temp_activity_list
        tempMap.value = temp_days_map
    }

    fun removeActivity(day: Int, index: Int) {
//        Log.i("PROBLEMA","$day --- $index}")
//        Log.i("PROBLEMA",travel.value.itinerary.toString())
        val temp_activity_list = tempMap.value[day]!!.toMutableList()
//        Log.i("RemoveActivity", "Index: $index, Size of list: ${temp_activity_list.size}")


//        Log.i("PROBLEMA", "Before removal: ${temp_activity_list}")

        temp_activity_list.removeAt(index)

//        Log.i("PROBLEMA", "After removal: ${temp_activity_list}")

        val temp_days_map = tempMap.value.toMutableMap()

        temp_days_map[day] = temp_activity_list

        tempMap.value = temp_days_map

    }

    fun fetchActivity(day: Int, id: Int) {
        activity.value = tempMap.value[day]!![id]
    }

    fun resetActivity() {
        activity.value = (Activity())
    }

    //Functions to modify the activity
    fun setActivityName(s: String) {
        activity.value = activity.value.copy(title = s)
    }

    fun setStartTime(time: Long) {
        activity.value =
            activity.value.copy(timeStart = adaptTime(time))
    }

    fun setEndTime(time: Long) {
        activity.value = activity.value.copy(timeEnd = adaptTime(time))
    }

    fun setMandatory(mandatory: Boolean) {
        activity.value = activity.value.copy(mandatory = mandatory)
    }

    fun setDescription(s: String) {
        activity.value = activity.value.copy(description = s)
    }

    fun setSuggestions(s: String) {
        activity.value = activity.value.copy(suggestedActivities = s)
    }

    suspend fun deleteTravel(context: Context): Boolean {
        return model.deleteTravel(_travel.value.id)
    }
}