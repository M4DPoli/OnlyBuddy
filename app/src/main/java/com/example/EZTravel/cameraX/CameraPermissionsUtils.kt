package com.example.EZTravel.cameraX

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object CameraPermissionsUtils {

    val CAMERA_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA
    ).toTypedArray()

    fun cameraPermissionsGranted(context: Context) = CAMERA_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun checkCameraPermissions(permissions: Map<String, Boolean>): Boolean {
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in CAMERA_PERMISSIONS && !it.value)
                permissionGranted = false
        }

        return permissionGranted
    }

}


