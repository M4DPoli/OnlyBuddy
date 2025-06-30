package com.example.EZTravel.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

object NotificationPermissionsUtils {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val NOTIFICATION_PERMISSIONS = mutableListOf(
        Manifest.permission.POST_NOTIFICATIONS
    ).toTypedArray()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun notificationPermissionsGranted(context: Context) = NOTIFICATION_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkNotificationPermissions(permissions: Map<String, Boolean>): Boolean {
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in NOTIFICATION_PERMISSIONS && !it.value)
                permissionGranted = false
        }

        return permissionGranted
    }



}