package com.example.EZTravel.cameraX

import android.net.Uri
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import com.example.EZTravel.R

@Composable
fun CameraPreviewContent(
    scaffoldPadding: PaddingValues,
    onImageCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraPreviewViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val captureInProgress by viewModel.captureInProgress.collectAsStateWithLifecycle()
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val imageUri by viewModel.imageUri.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }


    // Handle image uri
    LaunchedEffect(imageUri) {
        try {
            if (imageUri != null) {
                onImageCaptured(imageUri!!)
            }
        } finally {
            viewModel.clearImageUri() // <-- This will *always* be called
        }
    }

    Box(modifier = modifier) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.padding(top = scaffoldPadding.calculateTopPadding())
            )
            Button(
                onClick = {
                    viewModel.takePhoto(context)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = scaffoldPadding.calculateBottomPadding()),
                enabled = !captureInProgress
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_camera),
                    contentDescription = "Take a photo"
                )
            }

            if (captureInProgress) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

}