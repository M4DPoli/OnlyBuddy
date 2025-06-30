package com.example.EZTravel.di

import android.app.NotificationManager
import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TravelsCollection

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UsersCollection

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatsCollection

@Module
@InstallIn(SingletonComponent::class)
object HiltInjectionModule {

    @Singleton
    @Provides
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        return firestore
    }

    @Provides
    @Singleton
    @UsersCollection
    fun provideUsersCollection(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("users")
    }

    @Provides
    @Singleton
    @TravelsCollection
    fun provideTravelsCollection(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("travels")
    }

    @Provides
    @Singleton
    @ChatsCollection
    fun provideChatsCollection(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("chats")
    }

    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context
    ): CredentialManager {
        return CredentialManager.create(context)
    }


    @Provides
    @Singleton
    fun provideFirestoreAuth() : FirebaseAuth {
        return Firebase.auth
    }

}