package com.example.EZTravel.travelPage


import android.content.Context
import android.util.Log
import com.example.EZTravel.AuthUserManager
import com.example.EZTravel.chat.Chat
import com.example.EZTravel.R
import com.example.EZTravel.deleteImageFromSupabase
import com.example.EZTravel.di.ChatsCollection
import com.example.EZTravel.di.TravelsCollection
import com.example.EZTravel.di.UsersCollection
import com.example.EZTravel.downloadBytesFromUrl
import com.example.EZTravel.getPublicUrl
import com.example.EZTravel.uploadImageToSupabase
import com.example.EZTravel.uriStringToByteArray
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject


enum class Highlights(val tag: Int, val icon: Int) {
    ADVENTURE(R.string.highlights_adventure, R.drawable.ic_andventure),
    ROAD_TRIP(R.string.highlights_road_trip, R.drawable.ic_car),
    PARTY((R.string.highlights_party), R.drawable.ic_party),
    MUSEUMS(R.string.highlights_museum, R.drawable.ic_gallery),
    SCUBA_DIVING(R.string.highlights_scuba_diving, R.drawable.ic_scuba),
    NIGHTLIFE(R.string.highlights_nightlife, R.drawable.ic_nightlife),
    SPORT(R.string.highlights_sport, R.drawable.ic_sport)
}

enum class State {
    ACCEPTED,
    REJECTED,
    PENDING
}

data class PaginatedResult(
    val travels: List<Travel>,
    val lastVisible: DocumentSnapshot?
)

@IgnoreExtraProperties
data class Activity(
    @get:Exclude
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Date = Date(),
    @set:PropertyName("time_start")
    @get:PropertyName("time_start")
    var timeStart: Date? = null,
    @set:PropertyName("time_end")
    @get:PropertyName("time_end")
    var timeEnd: Date? = null,
    var mandatory: Boolean = false,
    @set:PropertyName("suggested_activities")
    @get:PropertyName("suggested_activities")
    var suggestedActivities: String = ""
)

@IgnoreExtraProperties
data class Application(
    @get:Exclude
    var id: String = "",
    var user: DocumentReference? = null,
    var state: Int = 0,
    var size: Int = 0,
    @set:PropertyName("extra_buddies")
    @get:PropertyName("extra_buddies")
    var buddies: List<String> = listOf()
)

@IgnoreExtraProperties
data class TravelReview(
    @get:Exclude
    var id: String = "",
    var user: DocumentReference? = null,
    var description: String = "",
    var stars: Int = 0,
    @set:PropertyName("destination_rate")
    @get:PropertyName("destination_rate")
    var destinationRate: Int = 0,
    @set:PropertyName("organization_rate")
    @get:PropertyName("organization_rate")
    var organizationRate: Int = 0,
    @set:PropertyName("assistance_rate")
    @get:PropertyName("assistance_rate")
    var assistanceRate: Int = 0,
    var images: List<String> = emptyList(),
    @get:Exclude
    var skip : Boolean = false
)

data class UserReview(
    var reviewer: DocumentReference? = null,
    var travel: DocumentReference? = null,

    @set: PropertyName("is_positive")
    @get: PropertyName("is_positive")
    var isPositive: Boolean = false,

    var text: String = ""
)

data class Location(
    var name: String = "",

    @set:PropertyName("place_id")
    @get:PropertyName("place_id")
    var placeId: String? = null,

    @set:PropertyName("geo_point")
    @get:PropertyName("geo_point")
    var geoPoint: GeoPoint? = null
)

@IgnoreExtraProperties
data class Travel(
    @get:Exclude
    var id: String = "",
    var title: String = "",
    var description: String = "",
    @set:PropertyName("price_start")
    @get:PropertyName("price_start")
    var priceStart: Double = 0.0,
    @set:PropertyName("price_end")
    @get:PropertyName("price_end")
    var priceEnd: Double = 0.0,
    @set:PropertyName("date_start")
    @get:PropertyName("date_start")
    var dateStart: Date = Date(),
    @set:PropertyName("date_end")
    @get:PropertyName("date_end")
    var dateEnd: Date = Date(),
    var days: Int = 0,
    var images: List<String> = emptyList(),
    var owner: DocumentReference? = null,
    var location: Location? = null,
    var size: Int = 0,
    @get:Exclude
    var applications: List<Application> = emptyList(),
    var highlights: List<Int> = emptyList(),
    @set:PropertyName("activities")
    @get:PropertyName("activities")
    @get:Exclude
    var itinerary: List<Activity> = emptyList(),
    @get:Exclude
    var reviews: List<TravelReview> = emptyList(),
    @set:PropertyName("review_images")
    @get:PropertyName("review_images")
    var reviewImages: List<String> = emptyList(),
    var rating: Double = 0.0,
    @set:PropertyName("favorite_by")
    @get:PropertyName("favorite_by")
    var favoriteBy: List<DocumentReference> = emptyList(),
    var chat: DocumentReference? = null
)


class TravelModel @Inject constructor(
    @TravelsCollection private val travelsCollection: CollectionReference,
    @UsersCollection private val usersCollection: CollectionReference,
    @ChatsCollection private val chatsCollection: CollectionReference,
    private val firestore: FirebaseFirestore,
    private val userManager: AuthUserManager
) {
//    fun getAllUpcomingTravels(): Flow<List<Travel>> = callbackFlow {
//        val listener = travelsCollection
//            .addSnapshotListener { snapshot, error ->
//                if (snapshot != null) {
//                    val travels = snapshot.documents.mapNotNull { doc ->
//                        val travel = doc.toObject(Travel::class.java)
//                        travel?.id = doc.id
//                        travel
//                    }
//                    trySend(travels)
//                    Log.d("Travels", "Fetched travels successfully")
//                } else {
//                    Log.e("Error", error.toString())
//                    trySend(emptyList())
//                }
//            }
//
//        awaitClose { listener.remove() }
//    }

    suspend fun getUpcomingTravelsPaginated(
        lastVisible: DocumentSnapshot? = null,
        limit: Long = 10L
    ): PaginatedResult {
        var query = travelsCollection
            .orderBy("date_start")
            .whereGreaterThan("date_start", Timestamp.now())

        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        val snapshot = query.limit(limit).get().await()

        val travels = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Travel::class.java)?.apply { id = doc.id }
        }

        return PaginatedResult(travels, snapshot.documents.lastOrNull())
    }

    suspend fun getAllTravelsPaginated(
        lastDocument: DocumentSnapshot? = null,
        limit: Long = 10L
    ): PaginatedResult {
        var query = travelsCollection.limit(limit)

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val travels = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Travel::class.java)?.apply { id = doc.id }
        }

        return PaginatedResult(travels, snapshot.documents.lastOrNull())
    }

//    fun getAllTravels(): Flow<List<Travel>> = callbackFlow {
//        val listener = travelsCollection
//            .addSnapshotListener { snapshot, error ->
//                if (snapshot != null) {
//                    val travels = snapshot.documents.mapNotNull { doc ->
//                        val travel = doc.toObject(Travel::class.java)
//                        travel?.id = doc.id
//                        travel
//                    }
//                    trySend(travels)
//                } else {
//                    Log.e("Error", error.toString())
//                    trySend(emptyList())
//                }
//            }
//
//        awaitClose { listener.remove() }
//    }

    fun getTravelById(id: String): Flow<Travel> = callbackFlow {
        var currentTravel: Travel? = null
        var currentApplications: List<Application> = emptyList()
        var currentReviews: List<TravelReview> = emptyList()
        var currentActivities: List<Activity> = emptyList()

        var hasTravel = false
        var hasApps = false
        var hasReviews = false
        var hasActivities = false

        var initialized = false

        fun emitIfReady(force: Boolean = false) {
            if ((hasTravel && hasApps && hasReviews && hasActivities) || force) {
                currentTravel?.let {
                    trySend(
                        it.copy(
                            applications = currentApplications,
                            reviews = currentReviews,
                            itinerary = currentActivities
                        )
                    )
                }
            }
        }

        val travelListener = travelsCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    currentTravel =
                        snapshot.toObject(Travel::class.java)?.apply { this.id = snapshot.id }
                    hasTravel = currentTravel != null
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasTravel && hasApps && hasReviews && hasActivities
                    } else {
                        emitIfReady(force = true)
                    }
                }
            }

        val appsListener = travelsCollection.document(id).collection("applications")
            .addSnapshotListener { snap, error ->
                if (error == null && snap != null) {
                    currentApplications = snap.documents.mapNotNull { doc ->
                        doc.toObject(Application::class.java)?.apply { this.id = doc.id }
                    }
                    hasApps = true
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasTravel && hasApps && hasReviews && hasActivities
                    } else {
                        emitIfReady(force = true)
                    }
                }
            }

        val reviewsListener = travelsCollection.document(id).collection("reviews")
            .addSnapshotListener { snap, error ->
                if (error == null && snap != null) {
                    currentReviews = snap.documents.mapNotNull { doc ->
                        doc.toObject(TravelReview::class.java)?.apply { this.id = doc.id }
                    }
                    hasReviews = true
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasTravel && hasApps && hasReviews && hasActivities
                    } else {
                        emitIfReady(force = true)
                    }
                }
            }

        val activitiesListener = travelsCollection.document(id).collection("activities")
            .addSnapshotListener { snap, error ->
                if (error == null && snap != null) {
                    currentActivities = snap.documents.mapNotNull { doc ->
                        doc.toObject(Activity::class.java)?.apply { this.id = doc.id }
                    }
                    hasActivities = true
                    if (!initialized) {
                        emitIfReady()
                        initialized = hasTravel && hasApps && hasReviews && hasActivities
                    } else {
                        emitIfReady(force = true)
                    }
                }
            }

        awaitClose {
            travelListener.remove()
            appsListener.remove()
            reviewsListener.remove()
            activitiesListener.remove()
        }
    }

    fun getTravelRefById(id: String): DocumentReference {
        return travelsCollection.document(id)
    }


    suspend fun addReview(review: TravelReview, travelId: String, context: Context): Boolean {
        val currentUser = userManager.currentUser.value ?: return false
        val travelDocRef = travelsCollection.document(travelId)
        val reviewsCollection = travelDocRef.collection("reviews")
        val userRef = usersCollection.document(currentUser.id)

        try {
            if(review.skip){
                firestore.runTransaction { t ->
                    val newReview = review.copy(user = userRef)
                    val newDocRef = reviewsCollection.document()
                    t.set(newDocRef, newReview)
                    true
                }.await()
            }
            else{
                // Read existing reviews
                val existingReviews = reviewsCollection.get().await().documents.mapNotNull {
                    it.toObject(TravelReview::class.java)
                }
                Log.d("TravelModel", "Existing reviews read")

                // Prevent duplicate reviews
                if (existingReviews.any { it.user == review.user }) return false

                // Calculate new rating
                val newRating =
                    if (review.stars == 0) {
                        (existingReviews.sumOf { it.stars } + review.stars) / (existingReviews.size + 1)
                    } else {
                        existingReviews.sumOf { it.stars } / (existingReviews.size + 1)
                    }

                // Upload all images
                val uploadedImageUrls = mutableListOf<String>()
                for (uriString in review.images) {
                    val bytes = uriStringToByteArray(context, uriString)
                    if (bytes != null) {
                        val fileName = "${UUID.randomUUID()}.jpg"
                        val success = uploadImageToSupabase(fileName, bytes)
                        if (success) {
                            getPublicUrl(fileName = fileName).let { uploadedImageUrls.add(it) }
                        }
                    }
                }

                // write review and update rating
                firestore.runTransaction { t ->
                    val newReview = review.copy(user = userRef, images = uploadedImageUrls)
                    val newDocRef = reviewsCollection.document()
                    t.set(newDocRef, newReview)

                    val updates = mutableMapOf<String, Any>(
                        "rating" to newRating
                    )

                    if (uploadedImageUrls.isNotEmpty()) {
                        updates["review_images"] = FieldValue.arrayUnion(*uploadedImageUrls.toTypedArray())
                    }

                    t.update(travelDocRef, updates)

                    Log.d("TravelModel", "Review added and rating updated")

                    true
                }.await()
            }
            return true
        } catch (e: Exception) {
            Log.d("TravelModel", "Error adding review", e)
            e.printStackTrace()
            return false
        }
    }

    suspend fun addApplication(application: Application, travelId: String): Boolean {
        val currentUser = userManager.currentUser.value ?: return false
        val travelRef = travelsCollection.document(travelId)
        val userRef = usersCollection.document(currentUser.id)
        val applicationsRef = travelRef.collection("applications")


        return try {
            val applications = applicationsRef.get().await().documents.mapNotNull {
                it.toObject(Application::class.java)
            }

            // Check if application already exists
            val alreadyExists = applications.any { it.user?.id == currentUser.id }
            if (alreadyExists) return false

            firestore.runTransaction { t ->

                val travelSnapshot = t.get(travelRef)
                val travel =
                    travelSnapshot.toObject(Travel::class.java) ?: return@runTransaction false

                // Check if there is space
                val acceptedCount = applications.count { it.state == State.ACCEPTED.ordinal }
                if (acceptedCount >= travel.size) return@runTransaction false

                // Add application
                val newApplicationRef = applicationsRef.document()
                t.set(newApplicationRef, application.copy(user = userRef))

                t.update(
                    userRef,
                    "travels.applied",
                    FieldValue.arrayUnion(travelRef)
                )
            }.await()
            true
        } catch (e: Exception) {
            Log.d("TravelModel", "Error adding application", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteApplication(userId: String, travelId: String): Boolean {
        if (userManager.currentUser.value == null) return false
        if (userManager.currentUser.value!!.id != userId) return false

        val travelRef = travelsCollection.document(travelId)
        val userRef = usersCollection.document(userId)

        // Get reference of the application
        val applicationsRef = travelRef.collection("applications")
        val snapshot = applicationsRef.get().await()
        val applicationDoc = snapshot.documents.find {
            it.toObject(Application::class.java)?.user?.id == userId
        } ?: return false

        val applicationRef = applicationsRef.document(applicationDoc.id)


        return try {
            firestore.runTransaction { t ->
                val snap = t.get(applicationRef)

                if (!snap.exists()) {
                    throw IllegalStateException("Application not found")
                }
                val application = snap.toObject(Application::class.java)
                    ?: throw IllegalStateException("Application not found")
                if (application.state == State.REJECTED.ordinal) {
                    throw IllegalStateException("Application already rejected")
                }

                if (application.state == State.ACCEPTED.ordinal) {
                    val travelSnap = t.get(travelRef)
                    val travel = travelSnap.toObject(Travel::class.java)
                        ?: throw Exception("Travel not found")
                    val chatRef = travel.chat ?: throw Exception("Chat ref missing in travel")

                    // Rimuove l'utente dalla mappa last_users_access
                    t.update(chatRef, mapOf("last_users_access.${userRef.id}" to FieldValue.delete()))
                    // Rimuove la chat dalla lista dello user
                    t.update(userRef, "chats", FieldValue.arrayRemove(chatRef))
                }

                t.delete(applicationRef)
                t.update(
                    userRef,
                    "travels.applied",
                    FieldValue.arrayRemove(travelRef)
                )
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    suspend fun updateApplications(travelId: String, newApplications: List<Application>): Boolean {
//        val currentUser = userManager.currentUser.value ?: return false
        val travelRef = travelsCollection.document(travelId)
        val applicationsRef = travelRef.collection("applications")

        return try {
            val existingSnapshot = applicationsRef.get().await()
            val existingApplications = existingSnapshot.documents.mapNotNull {
                it.toObject(Application::class.java)?.copy(id = it.id)
            }
            firestore.runTransaction { t ->
                // Read travel document
                val travelSnapshot = t.get(travelRef)
                val travel = travelSnapshot.toObject(Travel::class.java)
                    ?: throw Exception("Travel not found")
                val chatRef = chatsCollection.document(travel.chat!!.id)
                // Merge: update existing or keep old ones
                val merged = existingApplications.map { oldApp ->
                    newApplications.find { it.id == oldApp.id } ?: oldApp
                }.toMutableList()

                // Add new applications
                val newOnes = newApplications.filter { newApp ->
                    existingApplications.none { it.id == newApp.id }
                }
                merged.addAll(newOnes)

                // Check for size limit
                val acceptedCount = merged
                    .filter { it.state == State.ACCEPTED.ordinal }
                    .sumOf { it.size }

                if (acceptedCount > travel.size) {
                    throw Exception("Too many accepted application spots: $acceptedCount > ${travel.size}")
                }

                // Delete all existing application docs
                for (doc in existingSnapshot) {
                    t.delete(doc.reference)
                }

                // Add updated applications
                for (app in merged) {
                    val userRef = usersCollection.document(app.user!!.id)
                    val docId = app.id
                    t.set(applicationsRef.document(docId), app)
                    if (app.state == State.ACCEPTED.ordinal){
                        t.set(chatRef, mapOf("last_users_access" to mapOf(userRef.id to Timestamp.now())), SetOptions.merge())
                        t.update(userRef, "chats", FieldValue.arrayUnion(chatRef))
                    }
                }

                true
            }.await()
        } catch (e: Exception) {
            Log.e("TravelModel", "Error updating applications for travel $travelId", e)
            false
        }
    }


    suspend fun addTravel(travel: Travel, context: Context): String? {
        if (userManager.currentUser.value == null) return null
        val newDocRef = travelsCollection.document()
        val newChatRef = chatsCollection.document()
        val travelId = newDocRef.id
        val ownerRef = usersCollection.document(userManager.currentUser.value!!.id)

        return try {
            // upload images
            val urls = mutableListOf<String>()

            for (image in travel.images) {
                val bytes = if (image.startsWith("http")) {
                    downloadBytesFromUrl(image)
                } else {
                    uriStringToByteArray(context, image)
                }


                if (bytes != null) {
                    val fileName = "${UUID.randomUUID()}.jpg"
                    val success = uploadImageToSupabase(fileName, bytes)
                    if (success) {
                        urls.add(getPublicUrl(fileName = fileName))
                    }
                }
            }

            firestore.runTransaction { t ->
                val travelToAdd = travel.copy(owner = ownerRef, images = urls, chat = newChatRef, rating = 0.0, reviewImages = emptyList())
                val chatToAdd = Chat(
                    title = travelToAdd.title,
                    photo = urls.firstOrNull() ?: "",
                    lastUsersAccess = mapOf(Pair(ownerRef.id, Timestamp.now())),
                    travel = newDocRef
                )
                t.set(newDocRef, travelToAdd)
                t.set(newChatRef, chatToAdd)
                t.update(
                    ownerRef,
                    "travels.created",
                    FieldValue.arrayUnion(newDocRef)
                )
                t.update(ownerRef, "chats", FieldValue.arrayUnion(newChatRef))
            }.await()


            val activitiesCollection = newDocRef.collection("activities")
            for (activity in travel.itinerary) {
                activitiesCollection.document().set(activity).await()
            }

            travelId
        } catch (e: Exception) {
            Log.e("TravelModel", "Error adding travel", e)
            null
        }
    }

    suspend fun updateTravel(travel: Travel, context: Context): Boolean {
        if (userManager.currentUser.value == null) return false
        val docRef = travelsCollection.document(travel.id)
        val chatRef = chatsCollection.document(travel.chat!!.id)

        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                Log.e("TravelModel", "Travel document with ID ${travel.id} does not exist.")
                return false
            }

            val currentImages = snapshot.get("images") as? List<String> ?: emptyList()
            val updatedImages = travel.images

            val imagesToUpload = updatedImages.filter { it !in currentImages }
            val imagesToDelete = currentImages.filter { it !in updatedImages }

            val newUrls = mutableListOf<String>()

            for (uri in imagesToUpload) {
                val bytes = uriStringToByteArray(context, uri)
                if (bytes != null) {
                    val fileName = "${UUID.randomUUID()}.jpg"
                    val success = uploadImageToSupabase(fileName, bytes)
                    if (success) {
                        newUrls.add(getPublicUrl(fileName = fileName))
                    }
                }
            }

            val keptUrls = updatedImages.filter { it in currentImages }
            val finalImages = keptUrls + newUrls

            firestore.runTransaction { t ->
                val updatedTravel = travel.copy(images = finalImages)
                t.set(docRef, updatedTravel)
                t.update(chatRef,"title",travel.title)
                t.update(chatRef,"photo",finalImages.firstOrNull() ?: "")
            }.await()

            for (urlToDelete in imagesToDelete) {
                val fileName = urlToDelete.substringAfterLast("/")
                deleteImageFromSupabase(fileName)
            }

            val activitiesRef = docRef.collection("activities")
            val existing = activitiesRef.get().await()
            for (document in existing.documents) {
                activitiesRef.document(document.id).delete().await()
            }

            for (activity in travel.itinerary) {
                activitiesRef.document().set(activity).await()
            }
            Log.d("TravelModel", "Travel ${travel.id} updated successfully.")
            true
        } catch (e: Exception) {
            Log.e("TravelModel", "Error updating travel ${travel.id}", e)
            false
        }
    }

    suspend fun deleteTravel(travelId: String): Boolean {
        val currentUser = userManager.currentUser.value ?: return false
        val travelRef = travelsCollection.document(travelId)

        return try {
            val snapshot = travelRef.get().await()
            if (!snapshot.exists()) {
                Log.e("TravelModel", "Travel document with ID $travelId does not exist.")
                return false
            }

            val travel = snapshot.toObject(Travel::class.java)
            val currentImages = snapshot.get("images") as? List<String> ?: emptyList()

            // Delete images from Supabase
            for (image in currentImages) {
                val fileName = image.substringAfterLast("/")
                deleteImageFromSupabase(fileName)
            }

            val applicationsRef = travelRef.collection("applications")
            val applicationsSnap = applicationsRef.get().await()

            val reviewsRef = travelRef.collection("reviews")
            val reviewsSnap = reviewsRef.get().await()

            val activitiesRef = travelRef.collection("activities")
            val activitiesSnap = activitiesRef.get().await()

            // Recupera la chat (fuori dalla transazione)
            val chatRef = travel?.chat
            val chatSnapshot = chatRef?.get()?.await()
            val chat = chatSnapshot?.toObject(Chat::class.java)

            firestore.runTransaction { t ->
                val ownerRef = travel?.owner

                // Remove from applicants
                for (doc in applicationsSnap.documents) {
                    val application = doc.toObject(Application::class.java)
                    val applicantRef = application?.user
                    if (applicantRef != null) {
                        t.update(
                            usersCollection.document(applicantRef.id),
                            "travels.applied",
                            FieldValue.arrayRemove(travelRef)
                        )
                    }
                    t.delete(doc.reference)
                }

                // Remove from favorites
                travel?.favoriteBy?.forEach { uRef ->
                    t.update(
                        uRef,
                        "travels.favorites",
                        FieldValue.arrayRemove(travelRef)
                    )
                }

                // Remove from owner
                if (ownerRef != null) {
                    t.update(
                        ownerRef,
                        "travels.created",
                        FieldValue.arrayRemove(travelRef)
                    )
                }

                // Remove chat from all participants
                if (chatRef != null && chat != null) {
                    for (userId in chat.lastUsersAccess.keys) {
                        val userDocRef = usersCollection.document(userId)
                        t.update(userDocRef, "chats", FieldValue.arrayRemove(chatRef))
                    }
                    t.delete(chatRef)
                }

                // Delete subcollections
                for (doc in activitiesSnap.documents) {
                    t.delete(doc.reference)
                }
                for (doc in reviewsSnap.documents) {
                    t.delete(doc.reference)
                }

                // Delete travel
                t.delete(travelRef)
            }.await()
            val usersSnapshot = usersCollection.get().await()
            val usersId = usersSnapshot.documents.map { it.id }
            for (id in usersId) {
                deleteNotifications(id, travelId)
            }

            Log.d("TravelModel", "Travel $travelId deleted successfully.")
            true
        } catch (e: Exception) {
            Log.e("TravelModel", "Error deleting travel $travelId", e)
            false
        }
    }

    private suspend fun deleteNotifications(userId: String, relatedId: String) {
        val notificationsRef = usersCollection.document(userId).collection("notifications")
        val snapshot = notificationsRef.whereEqualTo("related_id", relatedId).get().await()
        for (doc in snapshot.documents) {
            notificationsRef.document(doc.id).delete().await()
        }
    }
}