package com.pratyaksh.iykyk.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pratyaksh.iykyk.ui.theme.IykykTheme
import com.pratyaksh.iykyk.viewmodel.HomeViewModel
import com.pratyaksh.iykyk.viewmodel.HomeViewModelFactory

private val Jakarta = FontFamily.SansSerif

@Composable
fun HomeScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current

    val isInspection = LocalInspectionMode.current

    val homeViewModel: HomeViewModel? = if (!isInspection) {
        val factory = HomeViewModelFactory(
            contentResolver = context.contentResolver,
            context = context
        )
        viewModel(factory = factory)
    } else {
        null
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        homeViewModel?.onVideoSelected(uri)
    }

    val selectedVideoUri = if (!isInspection) {
        homeViewModel?.selectedVideoUri
    } else {
        null
    }

    val isVideoSelected = if (!isInspection) {
        selectedVideoUri != null
    } else {
        false
    }

    val videoName = homeViewModel?.selectedVideoName ?: "my_video.mp4"
    val videoDurationText = homeViewModel?.videoDurationText ?: "0:30"
    val videoInfo = "1080 × 1920 · $videoDurationText"
    val thumbnailBitmap = homeViewModel?.videoThumbnailBitmap

    HomeScreenContent(
        contentPadding = contentPadding,
        isVideoSelected = isVideoSelected,
        videoUri = selectedVideoUri,
        videoName = videoName,
        videoInfo = videoInfo,
        videoDurationText = videoDurationText,
        thumbnailBitmap = thumbnailBitmap,
        onPickVideo = {
            videoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        },
        onChangeVideo = {
            videoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        },
        onMakeCollage = { homeViewModel?.runSegmentationTest() }
    )
}

@Composable
fun HomeScreenContent(
    contentPadding: PaddingValues,
    isVideoSelected: Boolean,
    videoUri: Uri? = null,
    videoName: String,
    videoInfo: String,
    videoDurationText: String = "0:30",
    thumbnailBitmap: Bitmap? = null,
    onPickVideo: () -> Unit,
    onChangeVideo: () -> Unit,
    onMakeCollage: () -> Unit
) {
    val isInspection = LocalInspectionMode.current
    var isEntered by remember { mutableStateOf(isInspection) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    val anim1Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 50, easing = FastOutSlowInEasing),
        label = "anim1Alpha"
    )
    val anim1OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 50, easing = FastOutSlowInEasing),
        label = "anim1OffsetY"
    )

    val anim2Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "anim2Alpha"
    )
    val anim2OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "anim2OffsetY"
    )

    val anim3Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 280, easing = FastOutSlowInEasing),
        label = "anim3Alpha"
    )
    val anim3OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 280, easing = FastOutSlowInEasing),
        label = "anim3OffsetY"
    )

    val anim4Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "anim4Alpha"
    )
    val anim4OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "anim4OffsetY"
    )

    val anim5Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 520, easing = FastOutSlowInEasing),
        label = "anim5Alpha"
    )
    val anim5OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 520, easing = FastOutSlowInEasing),
        label = "anim5OffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "homeInfinite")

    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val floatRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatRot"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IykykTheme.colors.appBackground)
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = anim1Alpha
                            translationY = anim1OffsetY.dp.toPx()
                        }
                ) {
                    BrandWordmark(
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = anim2Alpha
                            translationY = anim2OffsetY.dp.toPx()
                        }
                ) {
                    Text(
                        text = "turn your video\ninto a people\ncollage",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.8).sp,
                            color = IykykTheme.colors.ink
                        ),
                        modifier = Modifier.width(280.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "pick a portrait video and we'll do the rest.",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.08).sp,
                            color = IykykTheme.colors.mutedInk
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            alpha = anim3Alpha
                            translationY = anim3OffsetY.dp.toPx()
                        }
                ) {
                    if (isVideoSelected) {
                        VideoPickerCardSelected(
                            modifier = Modifier.fillMaxSize(),
                            videoUri = videoUri,
                            videoName = videoName,
                            videoInfo = videoInfo,
                            videoDurationText = videoDurationText,
                            thumbnailBitmap = thumbnailBitmap,
                            onChangeVideo = onChangeVideo,
                            glowAlpha = glowAlpha,
                            glowScale = glowScale,
                            breatheScale = breatheScale
                        )
                    } else {
                        VideoPickerCardEmpty(
                            modifier = Modifier.fillMaxSize(),
                            glowAlpha = glowAlpha,
                            glowScale = glowScale,
                            floatY = floatY,
                            floatRot = floatRot,
                            onPickVideo = onPickVideo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = anim4Alpha
                            translationY = anim4OffsetY.dp.toPx()
                        }
                ) {
                    PrimaryActionButton(
                        isVideoSelected = isVideoSelected,
                        shimmerProgress = shimmerProgress,
                        sparkleRotation = sparkleRotation,
                        sparkleScale = sparkleScale,
                        onClick = {
                            if (isVideoSelected) {
                                onMakeCollage()
                            } else {
                                onPickVideo()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "private · everything happens on your device",
                    style = TextStyle(
                        fontFamily = Jakarta,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        color = IykykTheme.colors.mutedInk
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = anim5Alpha
                            translationY = anim5OffsetY.dp.toPx()
                        }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BrandWordmark(
    pulseAlpha: Float,
    pulseScale: Float
) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = IykykTheme.colors.primaryContainer.copy(alpha = 0.35f),
                spotColor = IykykTheme.colors.primaryContainer.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        IykykTheme.colors.primaryContainer,
                        Color(0xFFFF4B55)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "iykyk",
                style = TextStyle(
                    fontFamily = Jakarta,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = Color.White
                )
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun VideoPickerCardEmpty(
    modifier: Modifier = Modifier,
    glowAlpha: Float,
    glowScale: Float,
    floatY: Float,
    floatRot: Float,
    onPickVideo: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        label = "cardScale"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(glowScale)
                .alpha(glowAlpha)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            IykykTheme.colors.primaryFixed,
                            IykykTheme.colors.secondaryFixed,
                            IykykTheme.colors.tertiaryFixed
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(cardScale)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x1419161D),
                    spotColor = Color(0x1419161D)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(IykykTheme.colors.surface)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onPickVideo() }
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(236.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainerLow)
                    )

                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationY = floatY.dp.toPx()
                            rotationZ = floatRot
                        }
                    ) {
                        VideoIllustration()
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-10).dp, y = (-6).dp)
                            .size(46.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x3319161D),
                                spotColor = Color(0x3319161D)
                            )
                            .clip(CircleShape)
                            .background(IykykTheme.colors.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        MovieIcon()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "portrait video · up to a few minutes",
                    style = TextStyle(
                        fontFamily = Jakarta,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.12.sp,
                        color = IykykTheme.colors.mutedInk
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VideoPickerCardSelected(
    modifier: Modifier = Modifier,
    videoUri: Uri? = null,
    videoName: String,
    videoInfo: String,
    videoDurationText: String,
    thumbnailBitmap: Bitmap? = null,
    onChangeVideo: () -> Unit,
    glowAlpha: Float,
    glowScale: Float,
    breatheScale: Float
) {
    var isPlaying by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        label = "cardScale"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )
    val overlayScale by animateFloatAsState(
        targetValue = if (isPlaying) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "overlayScale"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(glowScale)
                .alpha(glowAlpha)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            IykykTheme.colors.primaryFixed,
                            IykykTheme.colors.secondaryFixed,
                            IykykTheme.colors.tertiaryFixed
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(cardScale)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x1419161D),
                    spotColor = Color(0x1419161D)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(IykykTheme.colors.surface)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1D1A21))
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailBitmap != null && !thumbnailBitmap.isRecycled) {
                        Image(
                            bitmap = thumbnailBitmap.asImageBitmap(),
                            contentDescription = "Selected video thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (videoUri != null) {
                        VideoPlayer(
                            uri = videoUri,
                            isPlaying = isPlaying,
                            onCompletion = { isPlaying = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isPlaying) 1f else 0f
                                }
                        )
                    }

                    if (overlayAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = overlayAlpha
                                }
                                .background(Color.Black.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = overlayScale
                                    scaleY = overlayScale
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                VideoIllustration()

                                Box(
                                    modifier = Modifier
                                        .scale(breatheScale)
                                        .size(56.dp)
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = CircleShape,
                                            ambientColor = Color.Black,
                                            spotColor = Color.Black
                                        )
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.50f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlayArrowIcon()
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ScheduleIcon()
                            Text(
                                text = videoDurationText,
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = videoName,
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.ink
                            )
                        )
                        Text(
                            text = videoInfo,
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainer)
                            .clickable {
                                isPlaying = false
                                onChangeVideo()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SyncIcon()
                            Text(
                                text = "change",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IykykTheme.colors.ink
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(
    uri: Uri,
    isPlaying: Boolean,
    onCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(uri)
                setOnCompletionListener {
                    onCompletion()
                }
            }
        },
        update = { videoView ->
            if (isPlaying) {
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            } else {
                if (videoView.isPlaying) {
                    videoView.pause()
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun PrimaryActionButton(
    isVideoSelected: Boolean,
    shimmerProgress: Float,
    sparkleRotation: Float,
    sparkleScale: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color(0x3319161D),
                spotColor = Color(0x3319161D)
            )
            .clip(CircleShape)
            .background(IykykTheme.colors.ink)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val shimmerX = width * shimmerProgress

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(shimmerX - width * 0.3f, 0f),
                    end = Offset(shimmerX + width * 0.3f, height)
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isVideoSelected) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = sparkleRotation
                        scaleX = sparkleScale
                        scaleY = sparkleScale
                    }
                ) {
                    SparkleIcon()
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "make my collage",
                    style = TextStyle(
                        fontFamily = Jakarta,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.sp,
                        color = IykykTheme.colors.surface
                    )
                )
            } else {
                AddCircleIcon()

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "pick a video",
                    style = TextStyle(
                        fontFamily = Jakarta,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.sp,
                        color = IykykTheme.colors.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun VideoIllustration() {
    val coral = IykykTheme.colors.coral
    val yellow = IykykTheme.colors.yellow
    val purple = IykykTheme.colors.purple
    val ink = IykykTheme.colors.ink
    val surface = IykykTheme.colors.surface

    Box(
        modifier = Modifier.size(184.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(118.dp, 158.dp)
                .shadow(
                    elevation = 7.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0x2219161D),
                    spotColor = Color(0x2219161D)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFFFFBF5))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawRoundRect(
                    color = coral,
                    topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                    size = Size(
                        width = size.width - 24.dp.toPx(),
                        height = size.height - 24.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx())
                )

                drawRoundRect(
                    color = Color(0xFFFFFBF5),
                    topLeft = Offset(17.dp.toPx(), 17.dp.toPx()),
                    size = Size(
                        width = size.width - 34.dp.toPx(),
                        height = size.height - 34.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }

            PersonBubble(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 30.dp),
                color = yellow,
                size = 30.dp
            )

            PersonBubble(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 14.dp, y = 8.dp),
                color = purple,
                size = 40.dp
            )

            PersonBubble(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-14).dp, y = 22.dp),
                color = coral,
                size = 44.dp
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ink),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(15.dp)
                ) {
                    val path = Path().apply {
                        moveTo(2.dp.toPx(), 1.dp.toPx())
                        lineTo(size.width - 1.dp.toPx(), size.height / 2f)
                        lineTo(2.dp.toPx(), size.height - 1.dp.toPx())
                        close()
                    }

                    drawPath(
                        path = path,
                        color = surface
                    )
                }
            }

            Text(
                text = "VIDEO",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 6.dp, y = (-20).dp)
                    .background(
                        color = ink,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                style = TextStyle(
                    fontFamily = Jakarta,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                    color = surface
                )
            )

            Canvas(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 25.dp, y = 24.dp)
                    .size(13.dp)
            ) {
                drawLine(
                    color = coral,
                    start = Offset(0f, size.height),
                    end = Offset(0f, 0f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Square
                )

                drawLine(
                    color = coral,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Square
                )
            }
        }

        DecorativeStar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 24.dp),
            color = yellow
        )

        DecorativeStar(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-28).dp),
            color = yellow
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 26.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(purple)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 18.dp, y = (-28).dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(IykykTheme.colors.teal)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp, y = (-10).dp)
                .size(17.dp)
                .clip(CircleShape)
                .background(coral)
        )
    }
}

@Composable
private fun PersonBubble(
    modifier: Modifier,
    color: Color,
    size: Dp
) {
    val surface = IykykTheme.colors.surface

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size * 0.72f)
        ) {
            val minSize = minOf(this.size.width, this.size.height)

            drawCircle(
                color = surface,
                radius = minSize * 0.18f
            )

            drawArc(
                color = surface,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                style = Stroke(
                    width = minSize * 0.12f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun DecorativeStar(
    modifier: Modifier,
    color: Color
) {
    Canvas(
        modifier = modifier.size(20.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        val radiusOuter = minOf(size.width, size.height) / 2f
        val radiusInner = radiusOuter / 5f

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val radius = if (i % 2 == 0) radiusOuter else radiusInner

            val point = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius
            )

            if (i == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }

        path.close()

        drawPath(
            path = path,
            color = color
        )
    }
}

@Composable
private fun AddCircleIcon() {
    val surface = IykykTheme.colors.surface
    val ink = IykykTheme.colors.ink

    Canvas(
        modifier = Modifier.size(22.dp)
    ) {
        drawCircle(
            color = surface,
            radius = minOf(size.width, size.height) / 2f
        )

        drawLine(
            color = ink,
            start = Offset(size.width * 0.32f, size.height / 2f),
            end = Offset(size.width * 0.68f, size.height / 2f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = ink,
            start = Offset(size.width / 2f, size.height * 0.32f),
            end = Offset(size.width / 2f, size.height * 0.68f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MovieIcon() {
    val surface = IykykTheme.colors.surface
    val purple = IykykTheme.colors.purple

    Canvas(
        modifier = Modifier.size(22.dp)
    ) {
        drawRoundRect(
            color = surface,
            topLeft = Offset(size.width * 0.15f, size.height * 0.2f),
            size = Size(
                width = size.width * 0.7f,
                height = size.height * 0.6f
            ),
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        drawLine(
            color = purple,
            start = Offset(size.width * 0.32f, size.height * 0.2f),
            end = Offset(size.width * 0.32f, size.height * 0.8f),
            strokeWidth = 1.5.dp.toPx()
        )

        drawLine(
            color = purple,
            start = Offset(size.width * 0.68f, size.height * 0.2f),
            end = Offset(size.width * 0.68f, size.height * 0.8f),
            strokeWidth = 1.5.dp.toPx()
        )

        drawLine(
            color = purple,
            start = Offset(size.width * 0.15f, size.height * 0.4f),
            end = Offset(size.width * 0.85f, size.height * 0.4f),
            strokeWidth = 1.5.dp.toPx()
        )

        drawLine(
            color = purple,
            start = Offset(size.width * 0.15f, size.height * 0.6f),
            end = Offset(size.width * 0.85f, size.height * 0.6f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
private fun ScheduleIcon() {
    Canvas(
        modifier = Modifier.size(14.dp)
    ) {
        val minSize = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = Color.White,
            radius = minSize / 2f,
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawLine(
            color = Color.White,
            start = center,
            end = Offset(center.x, center.y - minSize * 0.3f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color.White,
            start = center,
            end = Offset(center.x + minSize * 0.25f, center.y),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PlayArrowIcon() {
    Canvas(
        modifier = Modifier.size(28.dp)
    ) {
        val path = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.25f)
            lineTo(size.width * 0.75f, size.height * 0.50f)
            lineTo(size.width * 0.38f, size.height * 0.75f)
            close()
        }

        drawPath(
            path = path,
            color = Color.White
        )
    }
}

@Composable
private fun SyncIcon() {
    val ink = IykykTheme.colors.ink

    Canvas(
        modifier = Modifier.size(16.dp)
    ) {
        drawArc(
            color = ink,
            startAngle = 30f,
            sweepAngle = 135f,
            useCenter = false,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        drawArc(
            color = ink,
            startAngle = 210f,
            sweepAngle = 135f,
            useCenter = false,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SparkleIcon() {
    val amber = IykykTheme.colors.amberSparkle

    Canvas(
        modifier = Modifier.size(22.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        val radiusOuter = minOf(size.width, size.height) / 2f
        val radiusInner = radiusOuter / 4f

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val radius = if (i % 2 == 0) radiusOuter else radiusInner

            val point = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius
            )

            if (i == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }

        path.close()

        drawPath(
            path = path,
            color = amber
        )
    }
}

@Preview(name = "Empty State - Light", showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenEmptyLightPreview() {
    IykykTheme(darkTheme = false) {
        HomeScreenContent(
            contentPadding = PaddingValues(0.dp),
            isVideoSelected = false,
            videoName = "",
            videoInfo = "",
            onPickVideo = {},
            onChangeVideo = {},
            onMakeCollage = {}
        )
    }
}

@Preview(name = "Empty State - Dark", showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenEmptyDarkPreview() {
    IykykTheme(darkTheme = true) {
        HomeScreenContent(
            contentPadding = PaddingValues(0.dp),
            isVideoSelected = false,
            videoName = "",
            videoInfo = "",
            onPickVideo = {},
            onChangeVideo = {},
            onMakeCollage = {}
        )
    }
}

@Preview(name = "Video Selected - Light", showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenVideoSelectedLightPreview() {
    IykykTheme(darkTheme = false) {
        HomeScreenContent(
            contentPadding = PaddingValues(0.dp),
            isVideoSelected = true,
            videoName = "my_video.mp4",
            videoInfo = "1080 × 1920 · 30 sec",
            onPickVideo = {},
            onChangeVideo = {},
            onMakeCollage = {}
        )
    }
}

@Preview(name = "Video Selected - Dark", showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenVideoSelectedDarkPreview() {
    IykykTheme(darkTheme = true) {
        HomeScreenContent(
            contentPadding = PaddingValues(0.dp),
            isVideoSelected = true,
            videoName = "my_video.mp4",
            videoInfo = "1080 × 1920 · 30 sec",
            onPickVideo = {},
            onChangeVideo = {},
            onMakeCollage = {}
        )
    }
}
