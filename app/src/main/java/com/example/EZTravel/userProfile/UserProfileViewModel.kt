package com.example.EZTravel.userProfile


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.notification.NotificationData
import com.example.EZTravel.notification.NotificationModel
import com.example.EZTravel.travelPage.State
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.example.EZTravel.travelPage.UserReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileModel: UserProfileModel,
    private val travelModel: TravelModel,
    private val notificationModel: NotificationModel,
    userManager: AuthUserManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val loggedIn = userManager.loggedIn

    private val _user: MutableStateFlow<User> = MutableStateFlow(User())
    val user: StateFlow<User> = _user

    private val _tempUser: MutableStateFlow<User> = MutableStateFlow(User())
    val tempUser = _tempUser

    val currentUser: StateFlow<User?> = userManager.currentUser

    private val _createdTravels = MutableStateFlow<List<Travel>>(listOf())
    val createdTravels = _createdTravels

    private val _pastTravels = MutableStateFlow<List<Travel>>(listOf())
    val pastTravels = _pastTravels

    private val _nextTravels = MutableStateFlow<List<Travel>>(listOf())
    val nextTravels = _nextTravels

    private val _pastAccepted = MutableStateFlow(0)
    val pastAccepted = _pastAccepted

    private val _pastCreated = MutableStateFlow(0)
    val pastCreated = _pastCreated

    private val _totalTravelsRating = MutableStateFlow(0.0)
    val totalTravelsRating = _totalTravelsRating

    private val otherUserId: String? = savedStateHandle[EZTravelDestinationsArgs.USER_ID]

    private val _isOwner = MutableStateFlow(false)
    var isOwner = _isOwner

    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications

    private val _buddyReviews = MutableStateFlow<List<Triple<User, Travel, UserReview>>>(emptyList())
    val buddyReviews: StateFlow<List<Triple<User, Travel, UserReview>>> = _buddyReviews

    private val _nNotifications = MutableStateFlow(0)
    val nNotifications: StateFlow<Int> = _nNotifications


    init {
        viewModelScope.launch {
            val userFlow = if (otherUserId == null) {
                _isOwner.value = true
                currentUser.filterNotNull()
            } else {
                _isOwner.value = false
                userProfileModel.getUserById(otherUserId)
            }

            userFlow.collectLatest { u ->
                _user.value = u
                _tempUser.value = u
            }
        }

        viewModelScope.launch {
            _user.collectLatest { loadCreatedTravels() }
        }

        viewModelScope.launch {
            _user.collectLatest { loadAppliedTravels() }
        }

        viewModelScope.launch {
            _user.collectLatest { user ->
                // Svuota i dati precedenti
                _buddyReviews.value = emptyList()

                val combinedFlows = user.reviewsAsBuddy.map { r ->
                    val userFlow = userProfileModel.getUserById(r.reviewer!!.id)
                    val travelFlow = travelModel.getTravelById(r.travel!!.id)

                    combine(userFlow, travelFlow) { reviewer, travel ->
                        Triple(reviewer, travel, r)
                    }
                }

                combinedFlows.forEach { flow ->
                    flow.onEach { triple ->
                        _buddyReviews.update { it + triple }
                    }.launchIn(this)
                }
            }
        }


        viewModelScope.launch {
            currentUser
                .flatMapLatest {user ->
                    if (user != null) {
                        notificationModel.getCurrentUserNotifications()
                    } else {
                        flowOf(Result.success(emptyList())) // Nessuna notifica se l'utente è vuoto o nullo
                    }}
                .collectLatest { result ->
                    result.onSuccess { notifications ->
                        _notifications.value = notifications
                        _nNotifications.value = notifications.count { !it.isRead }
                    }.onFailure {
                        _notifications.value = emptyList()
                        _nNotifications.value = 0
                    }
                }
        }
    }

    private suspend fun loadCreatedTravels() {
        _user.flatMapLatest { u ->
            val travelIds = u.travelRefs["created"].orEmpty()
            if (travelIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                val travelFlows = travelIds.map { id ->
                    travelModel.getTravelById(id.id)
                }
                combine(travelFlows) { travelsArray ->
                    travelsArray.toList()
                }
            }
        }.collect { travels ->
            _createdTravels.value = travels
            val rated = travels.map { it.rating }.filter { it > 0.0 }
            val avg = if (rated.isNotEmpty()) rated.average() else 0.0
            val nrPastCreated = travels.count { it.dateEnd.before(Date()) }
            _pastCreated.value = nrPastCreated
            _totalTravelsRating.value = avg
        }
    }

    private suspend fun loadAppliedTravels() {
        _user.flatMapLatest { u ->
            val travelIds = u.travelRefs["applied"].orEmpty()
            if (travelIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                val travelFlows = travelIds.map { id ->
                    travelModel.getTravelById(id.id)
                }
                combine(travelFlows) { travelsArray ->
                    travelsArray.toList()
                }
            }
        }
            .collect { travels ->
                _pastTravels.value = travels.filter { travel ->
                    val userId = _user.value.id
                    val userApplication = travel.applications.firstOrNull { it.user?.id == userId }
                    travel.dateEnd.before(Date()) && userApplication?.state == State.ACCEPTED.ordinal
                }

                val nrPastAccepted = _pastTravels.value.size
                _nextTravels.value = travels.filter { it.dateEnd.after(Date()) }
                _pastAccepted.value = nrPastAccepted
            }
    }

    fun setFullName(s: String) {
        _tempUser.value = _tempUser.value.copy(fullName = s)
    }
    val fullNameError = mutableStateOf("")

    fun setUserName(s: String) {
        _tempUser.value = _tempUser.value.copy(username = s)
    }
    val usernameError = mutableStateOf("")


    fun setProfilePictureURI(s: Uri?) {
        tempUser.value = tempUser.value.copy(profilePicture = s.toString())
    }
    fun setEmail(s: String) {
        _tempUser.value = _tempUser.value.copy(email = s)
    }
    val emailError = mutableStateOf("")

    fun setPhone(s: String) {
        _tempUser.value = _tempUser.value.copy(phone = s)
    }
    val phoneError = mutableStateOf("")


    fun setBio(s: String) {
        _tempUser.value = _tempUser.value.copy(bio = s)
    }

    fun setShowPastTravels(b: Boolean) {
        tempUser.value = tempUser.value.copy(showPastTravels = b)
    }

    fun setHighlights(hl: List<Int>) {
        _tempUser.value = _tempUser.value.copy(highlights = hl)
    }

    val highlightsError = mutableStateOf("")

    private val _showBottomSheet = mutableStateOf(false)
    val showBottomSheet = _showBottomSheet
    fun setShowBottom(b: Boolean) {
        _showBottomSheet.value = b
    }

    //Validation logic
    private fun validate(): Boolean {
        var valid = true
        fullNameError.value = ""
        usernameError.value = ""
        emailError.value = ""
        phoneError.value = ""
        highlightsError.value = ""

        if (_tempUser.value.fullName.isBlank()) {
            valid = false
            fullNameError.value = "Name cannot be blank!"
        }
        if (!_tempUser.value.username.isValidUsername()) {
            valid = false
            usernameError.value =
                "Username should contain at least 3 characters, and cannot contain \"/\",\"@\" or \"\\\""
        }

        if (!_tempUser.value.email.isValidEmail()) {
            valid = false
            emailError.value = "Please provide a valid E-Mail"
        }

        if (!_tempUser.value.phone.isValidPhone()) {
            valid = false
            phoneError.value = "Please provide a valid phone number"
        }

        if (_tempUser.value.highlights.isEmpty() || _tempUser.value.highlights.size > 4) {
            valid = false
            highlightsError.value = "Please select up to 4 highlights"
        }

        return valid
    }

    suspend fun save(context: Context): Boolean {
        if (!validate())
            return false
        return userProfileModel.updateUser(_tempUser.value, context)
    }
}

fun String.isValidUsername(): Boolean {
    val bannedChars = "/\\@"
    return !(this.length < 3 || this.any { it in bannedChars })
}

fun String.isValidEmail(): Boolean {
    val regex = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$".toRegex()
    return this.matches(regex)
}

fun String.isValidPhone(): Boolean {
    val regex = "^(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\$".toRegex()
    return this.matches(regex)
}