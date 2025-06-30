package com.example.EZTravel.login

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.notification.NotificationModel
import com.example.EZTravel.userProfile.User
import com.example.EZTravel.userProfile.UserProfileModel
import com.example.EZTravel.userProfile.isValidEmail
import com.example.EZTravel.userProfile.isValidPhone
import com.example.EZTravel.userProfile.isValidUsername
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val userManager: AuthUserManager,
    private val userProfileModel : UserProfileModel,
    private val firebaseAuth: FirebaseAuth,
    private val notificationModel: NotificationModel
) : ViewModel() {

    val newUser = MutableStateFlow(false)

    val loggedIn = MutableStateFlow(false)

    val _user = MutableStateFlow(User(
        id = firebaseAuth.currentUser?.uid ?: "",
        fullName = firebaseAuth.currentUser?.displayName ?: "",
        username = firebaseAuth.currentUser?.email?.split("@")?.get(0) ?: "",
        email = firebaseAuth.currentUser?.email ?: "",
        phone = firebaseAuth.currentUser?.phoneNumber?:"",
    ))
    val user : StateFlow<User> = _user

    init{
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser!=null){
            _user.value = User(
                id = firebaseUser.uid,
                fullName = firebaseUser.displayName!!,
                email = firebaseUser.email!!,
            )
        }
        viewModelScope.launch {
            userManager.newUser.collect{
                newUser.value = it
            }
        }
        viewModelScope.launch {
            userManager.loggedIn.collect{
                loggedIn.value = it
            }
        }
    }

    fun setFullName(s: String) { _user.value = user.value.copy(fullName = s) }
    val fullNameError = mutableStateOf("")

    fun setUserName(s: String) { _user.value = user.value.copy(username = s) }
    val usernameError = mutableStateOf("")


    fun setProfilePictureURI(s: Uri?) { _user.value = user.value.copy(profilePicture = s.toString()) }

    fun setEmail(s : String) { _user.value = user.value.copy(email = s) }
    val emailError = mutableStateOf("")

    fun setPhone(s : String) { _user.value = user.value.copy(phone = s) }
    val phoneError = mutableStateOf("")


    fun setBio(s : String) { _user.value = user.value.copy(bio = s) }

    fun setShowPastTravels(b : Boolean)  { _user.value = user.value.copy(showPastTravels = b) }

    fun setHighlights(hl : List<Int>){ _user.value = user.value.copy(highlights = hl) }
    val highlightsError = mutableStateOf("")

    val showBottomSheet = mutableStateOf(false)
    fun setShowBottom(b : Boolean) {showBottomSheet.value = b}

    //Validation logic
    private fun validate(): Boolean {
        var valid = true
        fullNameError.value = ""
        usernameError.value = ""
        emailError.value = ""
        phoneError.value = ""
        highlightsError.value = ""

        if (user.value.fullName.isBlank()) {
            valid = false
            fullNameError.value = "Name cannot be blank!"
        }
        if (!_user.value.username.isValidUsername()) {
            valid = false
            usernameError.value =
                "Username should contain at least 3 characters, and cannot contain \"/\",\"@\" or \"\\\""
        }

        if (!_user.value.email.isValidEmail()) {
            valid = false
            emailError.value = "Please provide a valid E-Mail"
        }

        if (!_user.value.phone.isValidPhone()) {
            valid = false
            phoneError.value = "Please provide a valid phone number"
        }

        if (user.value.highlights.isEmpty() || user.value.highlights.size > 4) {
            valid = false
            highlightsError.value = "Please select up to 4 highlights"
        }

        return valid
    }

    suspend fun save(context: Context): Boolean {
        if (!validate())
            return false
        return userProfileModel.updateUser(_user.value, context)
    }

    fun saveNotificationToken() {

        viewModelScope.launch(Dispatchers.IO) {
            val token = notificationModel.getToken()

            if (token.isFailure) {
                Log.e(
                    "EZTravelMessagingService",
                    "Error getting FCM token: ${token.exceptionOrNull()?.message}"
                )
                return@launch
            }
            notificationModel.registerToken(token.getOrThrow()).onSuccess {
                Log.d("EZTravelMessagingService", "Token saved in Firestore")
            }.onFailure { error ->
                Log.e("EZTravelMessagingService", "Error saving FCM token: ${error.message}")
            }

        }
    }


}
