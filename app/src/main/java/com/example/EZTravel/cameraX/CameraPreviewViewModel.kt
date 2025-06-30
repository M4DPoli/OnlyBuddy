package com.example.EZTravel.cameraX

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraPreviewViewModel : ViewModel() {
    // Used to set up a link between the Camera and your UI.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri

    private val _captureInProgress = MutableStateFlow(false)
    val captureInProgress: StateFlow<Boolean> = _captureInProgress

    private val imageCaptureUseCase =
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
        }
    }

    fun takePhoto(context: Context) {
        viewModelScope.launch {
            _captureInProgress.update { true }
            try {
                val imageUri = captureImage(context)
                _imageUri.update { imageUri }
            } catch (e: Exception) {
                Log.e("CameraPreviewViewModel", "Error capturing image", e)
            } finally {
                _captureInProgress.update { false }
            }
        }
    }


    private suspend fun captureImage(context: Context): Uri =
        suspendCancellableCoroutine { continuation ->
            // Create output file options
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "MAD_${timeStamp}_.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            imageCaptureUseCase.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CAMERA", "onError", exception)
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        val updatedContentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }

                        // Update the MediaStore entry.
                        if (savedUri != null) {
                            context.contentResolver.update(
                                savedUri,
                                updatedContentValues,
                                null,
                                null
                            )
                        } else {
                            Log.e(
                                "CameraX",
                                "Error updating MediaStore entry, the savedUri is null"
                            )
                            val exception =
                                Exception("Error updating MediaStore entry, the savedUri is null")
                            if (continuation.isActive) continuation.resumeWithException(
                                exception
                            )
                        }

                        if (savedUri != null) {
                            if (continuation.isActive) continuation.resume(savedUri)
                            Log.d("CameraX", "Image saved in: $savedUri")
                        } else {
                            Log.e("CameraX", "Error capturing image, the savedUri is null")
                            val exception = Exception("Error capturing image, the savedUri is null")
                            if (continuation.isActive) continuation.resumeWithException(exception)
                        }
                    }
                })
        }

    fun clearImageUri() {
        _imageUri.update { null }
    }


    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        processCameraProvider.bindToLifecycle(
            lifecycleOwner, DEFAULT_FRONT_CAMERA, cameraPreviewUseCase, imageCaptureUseCase
        )

        // Cancellation signals we're done with the camera
        try {
            awaitCancellation()
        } finally {
            processCameraProvider.unbindAll()
        }
    }
}