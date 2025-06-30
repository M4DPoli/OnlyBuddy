package com.example.EZTravel.userProfile

import android.content.Context
import android.util.Log
import com.example.EZTravel.di.TravelsCollection
import com.example.EZTravel.di.UsersCollection
import com.example.EZTravel.getPublicUrl
import com.example.EZTravel.travelPage.UserReview
import com.example.EZTravel.uploadImageToSupabase
import com.example.EZTravel.uriStringToByteArray
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class User(
    @set:Exclude
    @get:Exclude
    var id: String = "",

    @set:PropertyName("full_name")
    @get:PropertyName("full_name")
    var fullName: String = "",

    var username: String = "",

    @set:Exclude
    @get:Exclude
    var nrTravels: Int = 0,

    @set:Exclude
    @get:Exclude
    var ratingAsBuddy: Int = 0,

    @set:Exclude
    @get:Exclude
    var createdTravelsRating: Double = 0.0,
    @set:Exclude
    @get:Exclude
    var reviewsAsBuddy: List<UserReview> = listOf(),

    @set:PropertyName("profile_picture")
    @get:PropertyName("profile_picture")
    var profilePicture: String? = null,

    var email: String = "",
    var bio: String = "",

    @set:PropertyName("phone_nr")
    @get:PropertyName("phone_nr")
    var phone: String = "",

    var highlights: List<Int> = listOf(),

    @set:PropertyName("travels")
    @get:PropertyName("travels")
    var travelRefs: Map<String, List<DocumentReference>> = mapOf(),

    @set:PropertyName("chats")
    @get:PropertyName("chats")
    var chatRefs: List<DocumentReference> = listOf(),

    @set:PropertyName("show_past_travels")
    @get:PropertyName("show_past_travels")
    var showPastTravels: Boolean = true,
)

class UserProfileModel @Inject constructor(
    @TravelsCollection private val travelsCollection: CollectionReference,
    @UsersCollection private val usersCollection: CollectionReference,
    private val firestore: FirebaseFirestore
) {

    fun getUserById(id: String): Flow<User> = callbackFlow {
        var currentUser: User? = null
        var currentBuddyReviews: List<UserReview> = emptyList()

        var hasUser = false
        var hasBuddyReviews = false
        var initialized = false

        fun emitIfReady(force: Boolean = false) {
            if ((hasUser && hasBuddyReviews) || force) {
                currentUser?.let { user ->
                    val updatedUser = user.copy(
                        reviewsAsBuddy = currentBuddyReviews,
                        ratingAsBuddy = if (currentBuddyReviews.isNotEmpty()) {
                            ((currentBuddyReviews.count { it.isPositive }.toDouble() / currentBuddyReviews.size) * 100).toInt()
                        } else {
                            0
                        }
                    )
                    trySend(updatedUser)
                }
            }
        }

        val userListener = usersCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    currentUser = snapshot.toObject(User::class.java)?.apply { this.id = snapshot.id }
                    hasUser = currentUser != null
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasUser && hasBuddyReviews
                    } else {
                        emitIfReady(force = true)
                    }
                }
            }

        val buddyReviewsListener = usersCollection.document(id).collection("buddy_reviews")
            .addSnapshotListener { snap, error ->
                if (error == null && snap != null) {
                    currentBuddyReviews = snap.documents.mapNotNull { doc ->
                        doc.toObject(UserReview::class.java)
                    }
                    hasBuddyReviews = true
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasUser && hasBuddyReviews
                    } else {
                        emitIfReady(force = true)
                    }
                }
            }

        awaitClose {
            userListener.remove()
            buddyReviewsListener.remove()
        }
    }


    fun getUserReferenceById(id: String): DocumentReference {
        return usersCollection.document(id)
    }

    suspend fun updateUser(user: User, context: Context): Boolean {
        return try {

            val snapshot = usersCollection.document(user.id).get().await()
            val currentImageUrl = snapshot.getString("profile_picture")
            var imageUrl: String? = null
            var bytes: ByteArray? = null
            if (user.profilePicture != null && user.profilePicture != "" && currentImageUrl != user.profilePicture) {
                bytes = uriStringToByteArray(context, user.profilePicture!!)
            }
            if (bytes != null) {
                val fileName = "${UUID.randomUUID()}.jpg"
                val success = uploadImageToSupabase(fileName, bytes)
                if (success) imageUrl = getPublicUrl(fileName = fileName)
            }
            if (currentImageUrl == user.profilePicture) imageUrl = currentImageUrl

            usersCollection.document(user.id)
                .set(user.copy(profilePicture = imageUrl), SetOptions.merge())
            Log.i("Update", "Success")
            true
        } catch (e: Error) {
            Log.i("Update", "Failure: ${e.message}")
            false
        }
    }

    fun addTravelRefTo(field: String, travelId: String, userId: String) {
        if (field != ("created") && field != ("applied") && field != ("favorites")) return

        val travelRef = travelsCollection.document(travelId)
        val userRef = usersCollection.document(userId)

        firestore.runTransaction { t ->
            t.get(userRef)
            t.update(
                userRef,
                "travels.$field",
                FieldValue.arrayUnion(travelRef)
            )

            if (field == "favorites") {
                t.update(
                    travelRef,
                    "favoriteBy",
                    FieldValue.arrayUnion(userRef)
                )
            }
            null
        }.addOnSuccessListener {
            Log.d("Add", "Travel reference added successfully")
        }.addOnFailureListener {
            Log.e("Add", "Error adding travel reference", it)
        }
    }

    fun deleteTravelRefFrom(field: String, travelId: String, userId: String) {
        if (field != ("created") && field != ("applied") && field != ("favorites")) return

        val travelRef = travelsCollection.document(travelId)
        val userRef = usersCollection.document(userId)

        firestore.runTransaction { t ->
            t.update(
                userRef,
                "travels.$field",
                FieldValue.arrayRemove(travelRef)
            )

            if (field == "favorites") {
                t.update(
                    travelRef,
                    "favoriteBy",
                    FieldValue.arrayRemove(userRef)
                )
            }
            null
        }.addOnSuccessListener {
            Log.d("Delete", "Travel reference deleted successfully")
        }.addOnFailureListener {
            Log.e("Delete", "Error deleting travel reference", it)
        }
    }

    fun addUserReview(userReview: UserReview, reviewedId: String) {
        usersCollection.document(reviewedId).collection("buddy_reviews").add(userReview)
            .addOnSuccessListener {
                Log.d("Add", "User review added successfully")
            }
            .addOnFailureListener {
                Log.e("Add", "Error adding user review", it)
            }
    }
}