package com.example.EZTravel

import android.util.Log
import com.example.EZTravel.di.UsersCollection
import com.example.EZTravel.travelPage.UserReview
import com.example.EZTravel.userProfile.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthUserManager @Inject constructor(
    private val auth: FirebaseAuth,
    @UsersCollection private val usersCollection: CollectionReference
) {

    private var listenerRegistration: ListenerRegistration? = null

    private val _newUser = MutableStateFlow(false)
    val newUser = _newUser

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn = _loggedIn


    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentUserRef = MutableStateFlow<DocumentReference?>(null)
    val currentUserRef: StateFlow<DocumentReference?> = _currentUserRef.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                observeCurrentUser(firebaseUser.uid)

            } else {
                clearUserState()
            }
        }
    }

    /*
    private fun observeLoggedUser(userId: String) {
        listenerRegistration?.remove()
        listenerRegistration =
            usersCollection.document(userId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                snapshot?.toObject(User::class.java)?.let { user ->
                    user.id = snapshot.id
                    _currentUser.value = user
                    Log.d("UserManager", "Observed user: $user")
                }
            }
    }

     */

    fun observeCurrentUser(userId: String) {
        listenerRegistration?.remove()
        Log.d("LOGIN", "Observing user")

        val userDocRef = usersCollection.document(userId)
        val reviewsRef = userDocRef.collection("buddy_reviews")

        var currentUser: User? = null
        var currentReviews: List<UserReview> = emptyList()

        var hasUser = false
        var hasReviews = false
        var initialized = false

        fun updateCurrentUserIfReady(force: Boolean = false) {
            if ((hasUser && hasReviews) || force) {
                currentUser?.let { user ->
                    _currentUserRef.value = userDocRef
                    _loggedIn.value = true
                    _newUser.value = false
                    _currentUser.value = user.copy(
                        reviewsAsBuddy = currentReviews,
                        ratingAsBuddy = if (currentReviews.isNotEmpty())
                            ((currentReviews.count { it.isPositive }.toDouble() / currentReviews.size) * 100).toInt()
                        else 0
                    )
                }
            }
        }

        val userListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                Log.e("LOGIN", "User does not exist or error occurred")
                _newUser.value = true
                _loggedIn.value = true
                _currentUserRef.value = userDocRef
                return@addSnapshotListener
            }

            Log.d("LOGIN", "User exists")
            currentUser = snapshot.toObject(User::class.java)?.apply { id = snapshot.id }
            hasUser = currentUser != null

            if (!initialized) {
                updateCurrentUserIfReady()
                initialized = hasUser && hasReviews
            } else {
                updateCurrentUserIfReady(force = true)
            }
        }

        val reviewsListener = reviewsRef.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("USER_PROFILE_MODEL", "Error fetching live reviews: $error")
                return@addSnapshotListener
            }

            currentReviews = snap?.documents?.mapNotNull { it.toObject(UserReview::class.java) } ?: emptyList()
            hasReviews = true

            if (!initialized) {
                updateCurrentUserIfReady()
                initialized = hasUser && hasReviews
            } else {
                updateCurrentUserIfReady(force = true)
            }
        }

        listenerRegistration = ListenerRegistration {
            userListener.remove()
            reviewsListener.remove()
        }

        Log.d("LOGIN", "Added listeners for user and reviews")
    }



    private fun clearUserState() {
        Log.d("Logout","Clearing user state...")
        listenerRegistration?.remove()
        listenerRegistration = null
        _currentUserRef.value = null
        _currentUser.value = null
        _loggedIn.value = false
    }

}
