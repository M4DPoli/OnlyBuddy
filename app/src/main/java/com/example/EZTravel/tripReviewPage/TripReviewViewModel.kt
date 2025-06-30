package com.example.EZTravel.tripReviewPage

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.EZTravelDestinationsArgs
import com.example.EZTravel.R
import com.example.EZTravel.travelPage.Travel
import com.example.EZTravel.travelPage.TravelModel
import com.example.EZTravel.travelPage.TravelReview
import com.example.EZTravel.travelPage.UserReview
import com.example.EZTravel.userProfile.User
import com.example.EZTravel.userProfile.UserProfileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripReviewViewModel @Inject constructor(
    private val travelModel: TravelModel,
    private val userModel: UserProfileModel,
    userManager: AuthUserManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _overallRate = MutableStateFlow(0)
    val overallRate: StateFlow<Int> = _overallRate.asStateFlow()

    private val _destinationRate = MutableStateFlow(0)
    val destinationRate = _destinationRate.asStateFlow()

    private val _organizationRate = MutableStateFlow(0)
    val organizationRate = _organizationRate.asStateFlow()

    private val _assistanceRate = MutableStateFlow(0)
    val assistanceRate = _assistanceRate.asStateFlow()

    private val _reviewText = MutableStateFlow("")
    val reviewText = _reviewText.asStateFlow()

    private val _reviewImages = MutableStateFlow<List<Uri>>(emptyList())
    val reviewImages = _reviewImages.asStateFlow()


    //Validazione
    private val _fieldErrors: MutableStateFlow<Map<String, Int>> = MutableStateFlow(mapOf())
    val fieldErrors: StateFlow<Map<String, Int>> = _fieldErrors.asStateFlow()

    private val travelID: String =
        checkNotNull(savedStateHandle[EZTravelDestinationsArgs.TRAVEL_ID])

    private val _travel: MutableStateFlow<Travel> = MutableStateFlow(Travel())
    val travel: StateFlow<Travel> = _travel

    private val _buddiesReviews: MutableStateFlow<List<Pair<User,Pair<Boolean?,String>>>> = MutableStateFlow(
        emptyList()
    )
    val buddiesReviews: StateFlow<List<Pair<User,Pair<Boolean?,String>>>> = _buddiesReviews

    private val currentUser: StateFlow<User?> = userManager.currentUser

    var isOwner: Boolean = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun toggleIsLoading(){
        _isLoading.value = !_isLoading.value
    }


    init {
        viewModelScope.launch {
            travelModel.getTravelById(travelID).collect {
                _travel.value = it
                isOwner = it.owner?.id == (currentUser.value?.id ?: false)
            }
        }

        viewModelScope.launch {
            _travel
                .filter { it.applications.isNotEmpty() || it.owner != null }
                .flatMapLatest { travel ->
                    // Get user flows from applications
                    val applicationUserFlows = travel.applications.mapNotNull { application ->
                        application.user?.id?.let { userModel.getUserById(it) }
                    }

                    // Get owner flow
                    val ownerFlow = travel.owner?.id?.let { userModel.getUserById(it) }

                    // Combine both lists
                    val allUserFlows = if (ownerFlow != null) {
                        applicationUserFlows + ownerFlow
                    } else {
                        applicationUserFlows
                    }

                    if (allUserFlows.isNotEmpty()) {
                        combine(allUserFlows) { it.toList() }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { usersList ->
                    val currentUserId = currentUser.value?.id
                    val uniqueUsers = usersList
                        .filter { it.id != currentUserId }
                        .distinctBy { it.id }

                    val updatedList = uniqueUsers.map { user ->
                        val existing = _buddiesReviews.value.find { it.first.id == user.id }
                        user to (existing?.second ?: (null to ""))
                    }

                    _buddiesReviews.value = updatedList
                }
        }
    }

    fun setOverallRate(rate: Int) {
        _overallRate.value = rate
    }

    fun setDestinationRate(rate: Int) {
        _destinationRate.value = rate
    }

    fun setOrganizationRate(rate: Int) {
        _organizationRate.value = rate
    }

    fun setAssistanceRate(rate: Int) {
        _assistanceRate.value = rate
    }

    fun setReviewText(text: String) {
        _reviewText.value = text
    }

    fun addReviewImages(uris: List<Uri>) {
        _reviewImages.value = _reviewImages.value + uris
    }

    fun removeReviewImage(uri: Uri) {
        _reviewImages.value = _reviewImages.value - uri
    }


    fun setBuddyThumb(index: Int, thumb: Boolean?) {
        val updatedList = _buddiesReviews.value.toMutableList()
        val oldPair = updatedList[index]
        updatedList[index] = Pair(oldPair.first,Pair(thumb,oldPair.second.second))
        _buddiesReviews.value = updatedList
    }

    fun updateBuddyText(index: Int, text: String) {
        val updatedList = _buddiesReviews.value.toMutableList()
        val oldPair = updatedList[index]
        updatedList[index] = Pair(oldPair.first,Pair(oldPair.second.first,text))
        _buddiesReviews.value = updatedList
    }

    private fun validateFields(buddiesOnly: Boolean): Boolean {
        val errors = mutableMapOf<String, Int>()
        if (!buddiesOnly) {
            if (_overallRate.value == 0) errors[ReviewFieldError.OVERALL_RATE] =
                R.string.travel_review_error_star
            if (_destinationRate.value == 0) errors[ReviewFieldError.DESTINATION_RATE] =
                R.string.travel_review_error_star
            if (_organizationRate.value == 0) errors[ReviewFieldError.ORGANIZATION_RATE] =
                R.string.travel_review_error_star
            if (_assistanceRate.value == 0) errors[ReviewFieldError.ASSISTANCE_RATE] =
                R.string.travel_review_error_star
        }
        if (_buddiesReviews.value.any { it.second.first == null }) {
            errors[ReviewFieldError.BUDDIES] = R.string.travel_review_error_thumb
        }

        _fieldErrors.value = errors
        return errors.isEmpty()
    }

    suspend fun validateAndPublishReview(context: Context, isOwner: Boolean): Boolean {
        if (validateFields(buddiesOnly = isOwner)) {
            val userRef = userModel.getUserReferenceById(currentUser.value!!.id)
            val travelRef = travelModel.getTravelRefById(travelID)

            if (isOwner) {
                // Owner: Only add buddy reviews
                buddiesReviews.value.forEach { review ->
                    val userReview = UserReview(
                        reviewer = userRef,
                        travel = travelRef,
                        isPositive = review.second.first!!,
                        text = review.second.second
                    )
                    userModel.addUserReview(userReview, review.first.id)
                    //Adding a dummy review in order to avoid duplicate buddy review
                    val dummyReview = TravelReview(
                        user = userRef,
                        description = "placeholder",
                        stars = 0,
                        images = emptyList(),
                        skip = true
                    )
                    travelModel.addReview(dummyReview, travel.value.id, context)
                }
                return true
            } else {
                // Non-owner: Add full travel review + buddy reviews
                val reviewImages = reviewImages.value
                val travelReview = TravelReview(
                    user = userRef,
                    description = reviewText.value,
                    stars = overallRate.value,
                    destinationRate = destinationRate.value,
                    organizationRate = organizationRate.value,
                    assistanceRate = assistanceRate.value,
                    images = reviewImages.map { it.toString() },
                )

                val result = travelModel.addReview(travelReview, travel.value.id, context)

                buddiesReviews.value.forEach { review ->
                    val userReview = UserReview(
                        reviewer = userRef,
                        travel = travelRef,
                        isPositive = review.second.first!!,
                        text = review.second.second
                    )
                    userModel.addUserReview(userReview, review.first.id)
                }

                return result
            }
        }
        return false
    }

    object ReviewFieldError {
        const val OVERALL_RATE = "overallRate"
        const val DESTINATION_RATE = "destinationRate"
        const val ORGANIZATION_RATE = "organizationRate"
        const val ASSISTANCE_RATE = "assistanceRate"
        const val BUDDIES = "buddies"
    }
}