package com.example.EZTravel

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EZTravelApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        //Insert something here?
        FirebaseApp.initializeApp(this)
        Log.d("MyApp", "FirebaseApp initialized; currentUser = ${FirebaseAuth.getInstance().currentUser}")
        Places.initialize(applicationContext, "AIzaSyDr6AR_G-Imj4jqNsx7GGWOSSaUG4pdb4o")
    }
}