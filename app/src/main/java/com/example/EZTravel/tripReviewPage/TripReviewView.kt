package com.example.EZTravel.tripReviewPage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.R
import com.example.EZTravel.ReviewBuddyComponent
import com.example.EZTravel.StarReviewComponent
import kotlinx.coroutines.launch

@Preview
@Composable
fun TripReviewPreview() {
    TripReviewView(onBack = {}, onSnack = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReviewView(
    onBack: () -> Unit,
    onSnack: (String) -> Unit,
    vm: TripReviewViewModel = hiltViewModel()
) {
    val overallRate = vm.overallRate.collectAsState()
    val destinationRate = vm.destinationRate.collectAsState()
    val organizationRate = vm.organizationRate.collectAsState()
    val assistanceRate = vm.assistanceRate.collectAsState()
    val reviewText = vm.reviewText.collectAsState()

    val travel = vm.travel.collectAsState()
    val buddiesReviews = vm.buddiesReviews.collectAsState()

    val fieldErrors = vm.fieldErrors.collectAsState()
    val localContext = LocalContext.current
    val context = LocalContext.current
    val reviewImages = vm.reviewImages.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val isLoading by vm.isLoading.collectAsState()
    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
        ) { uris: List<Uri> ->
            vm.addReviewImages(uris)
        }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    else{
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.travel_review_title, travel.value.title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = AppBarColors.TopAppBarColor()
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = AppBarColors.BottomBarContainerColor(),
                    contentColor = AppBarColors.BottomBarContentColor(),
                    tonalElevation = AppBarColors.BottomBarTonalElevation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                            .navigationBarsPadding()
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    vm.toggleIsLoading()
                                    val success =
                                        vm.validateAndPublishReview(localContext, vm.isOwner)
                                    if (success) {
                                        onSnack("Review added successfully")
                                        onBack()
                                    } else {
                                        vm.toggleIsLoading()
                                        onSnack("Error adding review")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_send),
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.travel_review_publish),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize(),
            ) {

                if (!vm.isOwner) {
                    // --- Overall Rating ---
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.travel_review_overall),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .padding(
                                        start = 8.dp, end = 8.dp, top = 8.dp, bottom = 14.dp
                                    )
                                    .fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StarReviewComponent(overallRate.value, vm::setOverallRate)
                            }
                            ErrorField(
                                TripReviewViewModel.ReviewFieldError.OVERALL_RATE,
                                fieldErrors.value,
                                Arrangement.Center
                            )
                        }
                    }

                    // --- Secondary Ratings ---
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.travel_review_destination)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    StarReviewComponent(
                                        destinationRate.value,
                                        vm::setDestinationRate
                                    )
                                }
                            }
                        }

                        ErrorField(
                            TripReviewViewModel.ReviewFieldError.DESTINATION_RATE,
                            fieldErrors.value,
                            Arrangement.Start
                        )

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.travel_review_organization)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    StarReviewComponent(
                                        organizationRate.value,
                                        vm::setOrganizationRate
                                    )
                                }
                            }
                        }

                        ErrorField(
                            TripReviewViewModel.ReviewFieldError.ORGANIZATION_RATE,
                            fieldErrors.value,
                            Arrangement.Start
                        )

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.travel_review_assistance)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    StarReviewComponent(assistanceRate.value, vm::setAssistanceRate)
                                }
                            }
                        }

                        ErrorField(
                            TripReviewViewModel.ReviewFieldError.ASSISTANCE_RATE,
                            fieldErrors.value,
                            Arrangement.Start
                        )
                    }

                    // --- Divider ---
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                    }

                    // --- Review Text ---
                    item {
                        Column {
                            OutlinedTextField(
                                value = reviewText.value,
                                onValueChange = vm::setReviewText,
                                label = { Text(stringResource(R.string.travel_review_review_optional)) },
                                placeholder = { Text(stringResource(R.string.travel_review_review_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4
                            )
                        }
                    }

                    // --- Optional Image ---
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.travel_review_photos),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (reviewImages.value.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    items(reviewImages.value) { uri ->
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .height(120.dp)
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface.copy(
                                                            alpha = 0.7f
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { vm.removeReviewImage(uri) }
                                                    .padding(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                                    contentDescription = "Remove image",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            FilledTonalButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_photos),
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.travel_review_add_photo),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // --- Buddies Review Section ---
                item {
                    Text(
                        text = stringResource(R.string.travel_review_buddies_review),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(
                    buddiesReviews.value.size,
                    key = { buddiesReviews.value[it].first.id }
                ) { index ->
                    val user = buddiesReviews.value[index].first
                    ReviewBuddyComponent(
                        user,
                        { vm.setBuddyThumb(index, it) },
                        buddiesReviews.value[index].second.first,
                        buddiesReviews.value[index].second.second,
                        { vm.updateBuddyText(index, it) },
                        user.id == travel.value.owner?.id
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp))
                }

                item {
                    ErrorField(
                        TripReviewViewModel.ReviewFieldError.BUDDIES,
                        fieldErrors.value,
                        Arrangement.Center
                    )
                }
            }
        }
    }




}


@Composable
fun ErrorField(
    selectedField: String,
    fields: Map<String, Int>,
    horizontalArrangement: Arrangement.Horizontal
) {
    fields[selectedField]?.let {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .fillMaxSize(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(it),
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}