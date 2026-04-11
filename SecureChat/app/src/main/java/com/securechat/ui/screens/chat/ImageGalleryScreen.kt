package com.securechat.ui.screens.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGalleryScreen(
    imageSources: List<String>,
    startIndex: Int,
    onBack: () -> Unit
) {
    if (imageSources.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF10142A)),
            contentAlignment = Alignment.Center
        ) {
            Text("Khong co anh de hien thi", color = Color.White)
        }
        return
    }

    val safeStartIndex = startIndex.coerceIn(0, imageSources.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeStartIndex, pageCount = { imageSources.size })
    val scales = remember { mutableStateMapOf<Int, Float>() }
    val offsetX = remember { mutableStateMapOf<Int, Float>() }
    val offsetY = remember { mutableStateMapOf<Int, Float>() }

    LaunchedEffect(pagerState.currentPage) {
        scales[pagerState.currentPage] = scales[pagerState.currentPage] ?: 1f
        offsetX[pagerState.currentPage] = offsetX[pagerState.currentPage] ?: 0f
        offsetY[pagerState.currentPage] = offsetY[pagerState.currentPage] ?: 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10142A))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            var pageScale by remember(page) { mutableFloatStateOf(scales[page] ?: 1f) }
            var pageOffset by remember(page) {
                androidx.compose.runtime.mutableStateOf(
                    Offset(offsetX[page] ?: 0f, offsetY[page] ?: 0f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(page) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (pageScale > 1f) {
                                    pageScale = 1f
                                    pageOffset = Offset.Zero
                                } else {
                                    pageScale = 2f
                                }
                                scales[page] = pageScale
                                offsetX[page] = pageOffset.x
                                offsetY[page] = pageOffset.y
                            }
                        )
                    }
                    .pointerInput(page) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (pageScale * zoom).coerceIn(1f, 4f)
                            pageScale = newScale
                            if (newScale <= 1f) {
                                pageOffset = Offset.Zero
                            } else {
                                pageOffset = Offset(pageOffset.x + pan.x, pageOffset.y + pan.y)
                            }
                            scales[page] = pageScale
                            offsetX[page] = pageOffset.x
                            offsetY[page] = pageOffset.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageSources[page],
                    contentDescription = "Anh ${page + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = pageScale,
                            scaleY = pageScale,
                            translationX = pageOffset.x,
                            translationY = pageOffset.y
                        ),
                    loading = {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    },
                    error = {
                        Text(
                            text = "Khong tai duoc anh",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "${pagerState.currentPage + 1}/${imageSources.size}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x6620294D)
        ) {
            Text(
                text = "Double tap de zoom • Swipe de chuyen anh",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = Color(0xFFE3E7FF),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

