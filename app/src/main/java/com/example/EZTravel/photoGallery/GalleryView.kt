package com.example.EZTravel.photoGallery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.EZTravel.AppBarColors
import com.example.EZTravel.R

@Preview(name = "Pixel 7", device = "id:pixel_7")
@Composable
fun ExploreViewPreviewPixel7() {
    GalleryView( onBack = {})
}

@Preview(name = "Pixel 5", device = "id:pixel_5")
@Composable
fun ExploreViewPreviewPixel5() {
    GalleryView(onBack = {})
}

@Preview(name = "Galaxy S20", device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
fun ExploreViewPreviewS20() {
    GalleryView( onBack = {})
}

@Preview(name = "Galaxy S20 landscape", device = "spec:width=800dp,height=360dp,dpi=420")
@Composable
fun ExploreViewPreviewS20Landscape() {
    GalleryView( onBack = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryView(
    onBack: () -> Unit,
    vm: GalleryViewModel = hiltViewModel()
) {
    val travel by vm.travel.collectAsState()
    val selectedIndex by vm.selectedImageIndex.collectAsState()


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.photo_gallery_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = AppBarColors.TopAppBarColor()
            )
        }
    ) { padding ->
        if (travel.reviewImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.photo_gallery_no_photos),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells
                    .Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(travel.reviewImages.size) { index ->
                    GalleryImageItem(
                        source = travel.reviewImages[index],
                        index = index,
                        onClick = { vm.selectImage(it) }
                    )
                }
            }
        }
    }
    // Fullscreen dialog
    if (selectedIndex != null) {
        FullscreenImageDialog(
            sources = travel.reviewImages,
            initialPage = selectedIndex!!,
            onDismiss = { vm.clearSelection() }
        )
    }
}

@Composable
fun GalleryImageItem(source: Any, index: Int = 0, onClick: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick(index) }
    ) {
        Log.d("Source","$source")
        AsyncImage(
            contentDescription = null,
            model = source,
            contentScale = ContentScale.Crop
        )
    }
}


@Composable
fun FullscreenImageDialog(
    sources: List<Any>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { sources.size }
    )

    var dragOffsetY by remember { mutableStateOf(0f) }
    val dragThreshold = 100f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            val (_, dy) = dragAmount
                            dragOffsetY += dy
                            if (dragOffsetY > dragThreshold) {
                                onDismiss()
                                dragOffsetY = 0f
                            }
                        },
                        onDragEnd = {
                            dragOffsetY = 0f
                        }
                    )
                }
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = sources[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
