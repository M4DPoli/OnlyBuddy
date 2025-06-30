package com.example.EZTravel.login

import com.example.EZTravel.userProfile.getMonogram
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.EZTravel.cameraX.CameraPermissionsUtils
import com.example.EZTravel.EditHighlightsSection
import com.example.EZTravel.R
import com.example.EZTravel.cameraX.CameraPreviewContent
import com.example.EZTravel.travelPage.Highlights
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationView(
    onHome: () -> Unit,
    vm: RegistrationViewModel = hiltViewModel()
) {
    //cameraX
    val (takePhotoMode, setTakePhotoMode) = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    if (takePhotoMode) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(stringResource(R.string.camera_preview_title))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            //vm.setProfilePictureURI(null)
                            setTakePhotoMode(false)
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Column {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreviewContent(scaffoldPadding = padding, {
                        vm.setProfilePictureURI(it)
                        setTakePhotoMode(false)
                    })
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.safeDrawingPadding(),
            topBar = { TopAppBar(onHome) },
            bottomBar = {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = {
                        scope.launch {
                            if(vm.save(ctx)){
                                vm.saveNotificationToken()
                                onHome()
                            }
                        }
                    }) {
                        Icon(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 8.dp)
                                .size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.profile_page_edit_save),
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Content(
                innerPadding,
                setTakePhotoMode
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    onBack: () -> Unit
) {

    val title = "Create profile"

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {

            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back"
                )
            }
        },
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content(
    innerPadding: PaddingValues,
    setTakePhotoMode: ((Boolean) -> Unit),
    vm: RegistrationViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val showBottomSheet by vm.showBottomSheet
    val scope = rememberCoroutineScope()

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            it?.let { uri ->
                ctx.contentResolver?.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                vm.setProfilePictureURI(uri)
            }


        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle Permission granted/rejected
            if (!CameraPermissionsUtils.checkCameraPermissions(permissions)) {
                Toast.makeText(
                    ctx,
                    "Permission request denied, go to settings to grant the requested permission",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                setTakePhotoMode(true)
            }
        }


    val user by vm.user.collectAsState()
    val fullNameError by vm.fullNameError
    val usernameError by vm.usernameError
    val phoneError by vm.phoneError
    val emailError by vm.emailError
    val highlightsError by vm.highlightsError

//    val context = LocalContext.current


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 50.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            EditProfilePicture(
                user.fullName,
                if(user.profilePicture == null) null else user.profilePicture!!.toUri(),
                96,
                setTakePhotoMode,
                photoPickerLauncher,
                cameraPermissionLauncher
            )
            Text(
                text = stringResource(R.string.profile_page_your_profile),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.profile_page_your_profile_info),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Personal Info
            Text(
                text = stringResource(R.string.profile_page_personal_info_label),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.profile_page_edit_personal_info),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            //Full Name
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = user.fullName,
                onValueChange = { vm.setFullName(it) },
                label = { Text(stringResource(R.string.profile_page_edit_full_name)) },
                isError = fullNameError.isNotBlank(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                ),
                shape = MaterialTheme.shapes.medium
            )
            if (fullNameError.isNotBlank()) {
                Text(
                    text = fullNameError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            //Username
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = user.username,
                onValueChange = { vm.setUserName(it) },
                isError = usernameError.isNotBlank(),
                label = { Text(stringResource(R.string.profile_page_edit_username)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                ),
                shape = MaterialTheme.shapes.medium
            )
            if (usernameError.isNotBlank()) {
                Text(
                    text = usernameError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            //Email
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = user.email,
                onValueChange = { vm.setEmail(it) },
                label = { Text("E-Mail") },
                isError = emailError.isNotBlank(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done, keyboardType = KeyboardType.Email
                ),
                shape = MaterialTheme.shapes.medium
            )
            if (emailError.isNotBlank()) {
                Text(
                    text = emailError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            //Phone
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = user.phone,
                onValueChange = { vm.setPhone(it) },
                label = { Text(stringResource(R.string.profile_page_edit_phone)) },
                isError = phoneError.isNotBlank(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done, keyboardType = KeyboardType.Number
                ),
                shape = MaterialTheme.shapes.medium
            )
            if (phoneError.isNotBlank()) {
                Text(
                    text = phoneError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            EditHighlightsSection (user.highlights) { vm.setShowBottom(true) }
            if (highlightsError.isNotBlank()) {
                Text(
                    text = highlightsError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.profile_page_edit_about_you),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            //Bio
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = user.bio,
                onValueChange = { vm.setBio(it) },
                label = { Text("Bio") },
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                ),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(24.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.profile_page_edit_show_past_travels),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.registration_page_show_past_travels_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = user.showPastTravels,
                    onCheckedChange = { vm.setShowPastTravels(!user.showPastTravels) }
                )
            }

        }
    }

    //Bottom sheet for chips

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { vm.setShowBottom(false) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Highlights", // e.g., "Select Highlights"
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Highlights are a great way to show other what are your preferences when it comes to travels! Select them carefully",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Highlights.entries.forEach { h ->
                        val isSelected = user.highlights.contains(h.ordinal)
                        FilterChip(
                            label = { Text(LocalContext.current.getString(h.tag)) },
                            selected = isSelected,
                            onClick = {
                                val newHighlights = if (isSelected) {
                                    user.highlights.minus(h.ordinal)
                                } else {
                                    user.highlights.plus(h.ordinal)
                                }
                                vm.setHighlights(newHighlights)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(h.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${user.highlights.size}/4 selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (user.highlights.size > 4)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                vm.setShowBottom(false)
                            }
                        }
                    },
                    enabled = user.highlights.size in 1..4,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Done",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditProfilePicture(
    fullName: String,
    profilePictureURI: Uri?,
    pxl: Int,
    setTakePhotoMode: ((Boolean) -> Unit)? = null,
    photoPickerLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>? = null,
    cameraPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>? = null
) {
    val ctx = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(pxl.dp)
            .clickable(onClick = { expanded = !expanded }),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (profilePictureURI == null || profilePictureURI.toString() == "") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName.getMonogram(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = (0.30 * pxl).sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profilePictureURI,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

            }
        }
        Box {
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(0.dp, 5.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                imageVector = ImageVector.vectorResource(R.drawable.ic_camera),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Camera icon"
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_page_edit_take_picture)) },
                    onClick = {
                        if (CameraPermissionsUtils.cameraPermissionsGranted(ctx)) {
                            setTakePhotoMode?.let { it(true) }
                        } else {
                            cameraPermissionLauncher?.launch(
                                CameraPermissionsUtils.CAMERA_PERMISSIONS
                            )
                        }
                    })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_page_edit_choose_from_gallery)) },
                    onClick = {
                        photoPickerLauncher?.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        ); expanded = false
                    })
            }
        }
    }
}