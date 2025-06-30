package com.example.EZTravel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.EZTravel.di.UsersCollection
import com.example.EZTravel.notification.NotificationModel
import com.example.EZTravel.ui.theme.EZTravelTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EZTravelActivity() : ComponentActivity() {

    val TAG = "EZTravelActivity"

    @Inject
    lateinit var authUserManager : AuthUserManager

    @Inject
    lateinit var credentialManager :CredentialManager

    @Inject
    @UsersCollection
    lateinit var userCollection : CollectionReference

    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1001


    @Inject
    lateinit var notificationModel: NotificationModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("299403430382-in44erm244h9ft6jspc10mb1vbfc24pn.apps.googleusercontent.com") // stesso clientId
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        Log.d("LOGIN", "Forcing UserManager init: ${authUserManager.currentUser.value}")

        intent?.let {
            val notificationId = it.getStringExtra("notificationId")
            notificationId?.let { id ->
                lifecycleScope.launch {
                    try {
                        notificationModel.markNotificationAsRead(id)
                        Log.d("MainActivity", "Notification $id marked as read")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to mark notification as read: ${e.message}")
                    }
                }
            }
        }
        // Legge la destinazione dalla notifica
        val destinationToReach = intent?.getStringExtra("destination") ?: EZTravelDestinations.HOME_ROUTE

        enableEdgeToEdge()
        setContent {
            EZTravelTheme {
                EZTravelNavGraph(launchCredentialManager = {launchCredentialManager()}, startDestination = EZTravelDestinations.HOME_ROUTE,signOut = {signOut()},destinationToReach)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("Ingresso5","Entro qui")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Log.w(TAG, "Google Sign-In failed: null ID token")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed: ${e.localizedMessage}")
            }
        }
    }


    private fun launchGoogleSignIn() {
        Log.d("Ingresso4","Entro qui")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
    }


    private fun launchCredentialManager(){
        // [START create_credential_manager_request]
        // Instantiate a Google sign-in request
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId("299403430382-in44erm244h9ft6jspc10mb1vbfc24pn.apps.googleusercontent.com")
            // Only show accounts previously used to sign in.
            .setFilterByAuthorizedAccounts(false)
            .build()

        // Create the Credential Manager request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        // [END create_credential_manager_request]

        lifecycleScope.launch {
            try {
                // Launch Credential Manager UI
                val result = credentialManager.getCredential(
                    context = baseContext,
                    request = request
                )

                // Extract credential from the result returned by Credential Manager
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Couldn't retrieve user's credentials: ${e.localizedMessage}")
                launchGoogleSignIn()
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
        }
    }

    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("Ingresso","Entro qui")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Ingresso2","Entro qui")
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("LOGIN", "signInWithCredential:success")
                    val user = Firebase.auth.currentUser
                    //updateUI(user)
                } else {
                    Log.d("Ingresso3","Entro qui")
                    // If sign in fails, display a message to the user
                    Log.w("LOGIN", "signInWithCredential:failure", task.exception)
                    //updateUI(null)
                }
            }
    }
    // [END auth_with_google]


    // [START sign_out]
    private fun signOut() {
        Log.d("LOGOUT","Loggingout...")
        // Firebase sign out
        Firebase.auth.signOut()

        // When a user signs out, clear the current user credential state from all credential providers.
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                //updateUI(null)
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
            }
        }
    }
    // [END sign_out]

    /*
    private fun updateUI(user: FirebaseUser?) {
        if ( user == null ) return

        lifecycleScope.launch {
            val userId = user.uid

            try {
                val userDoc = userCollection.document(userId).get().await()
                if (!userDoc.exists()) {
                    val newUser = User(
                        id = userId,
                        fullName = user.displayName ?: "",
                        email = user.email ?: "",
                        profilePicture = user.photoUrl?.toString() ?: "",
                        username = "",
                        phone = user.phoneNumber ?: "",
                        travelRefs = mutableMapOf(),
                    )

                    userCollection.document(userId)
                        .set(newUser)
                        .addOnSuccessListener {
                            Log.d(TAG, "User added to Firestore")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Failed to add user to Firestore", it)
                        }
                } else {
                    Log.d(TAG, "User already exists in Firestore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking/creating Firestore user: ${e.localizedMessage}")
            }
        }
    }

     */
}
