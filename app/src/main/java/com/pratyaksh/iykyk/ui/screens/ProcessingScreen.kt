package com.pratyaksh.iykyk.ui.screens

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.pratyaksh.iykyk.ui.theme.IykykTheme

private val Jakarta = FontFamily.SansSerif

data class PersonResultItem(
    val id: Int,
    val name: String,
    val appearancesCount: Int,
    val isStarShot: Boolean = false,
    val thumbnailBitmap: Bitmap? = null
)

@Composable
fun ProcessingScreen(
    contentPadding: PaddingValues,
    isCompleted: Boolean = false,
    videoThumbnail: Bitmap? = null,
    progressFraction: Float = 0.42f,
    stageText: String = "finding faces",
    facesCount: Int = 4,
    totalAppearancesCount: Int = 20,
    estimatedTimeLeft: String = "~4s left",
    personResults: List<PersonResultItem> = emptyList(),
    onSeeCollage: () -> Unit = {},
    onChooseAnotherVideo: () -> Unit = {},
    onPersonClick: (PersonResultItem) -> Unit = {},
    onCancelProcessing: () -> Unit = {}
) {
    if (isCompleted) {
        val isInspection = LocalInspectionMode.current
        val displayPeople = if (personResults.isEmpty() && isInspection) {
            listOf(
                PersonResultItem(id = 1, name = "person 1", appearancesCount = 5, isStarShot = true),
                PersonResultItem(id = 2, name = "person 2", appearancesCount = 4),
                PersonResultItem(id = 3, name = "person 3", appearancesCount = 4),
                PersonResultItem(id = 4, name = "person 4", appearancesCount = 4),
                PersonResultItem(id = 5, name = "person 5", appearancesCount = 3)
            )
        } else {
            personResults
        }

        ResultsStateView(
            contentPadding = contentPadding,
            peopleCount = displayPeople.size,
            totalAppearancesCount = if (totalAppearancesCount > 0) totalAppearancesCount else displayPeople.sumOf { it.appearancesCount },
            personResults = displayPeople,
            onSeeCollage = onSeeCollage,
            onChooseAnotherVideo = onChooseAnotherVideo,
            onPersonClick = onPersonClick
        )
    } else {
        ProcessingStateView(
            contentPadding = contentPadding,
            videoThumbnail = videoThumbnail,
            progressFraction = progressFraction,
            stageText = stageText,
            facesCount = facesCount,
            estimatedTimeLeft = estimatedTimeLeft,
            onCancelProcessing = onCancelProcessing
        )
    }
}

@Composable
private fun ProcessingStateView(
    contentPadding: PaddingValues,
    videoThumbnail: Bitmap?,
    progressFraction: Float,
    stageText: String,
    facesCount: Int,
    estimatedTimeLeft: String,
    onCancelProcessing: () -> Unit
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
        animationSpec = tween(750, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "anim2Alpha"
    )
    val anim2OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "anim2OffsetY"
    )

    val anim3Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 250, easing = FastOutSlowInEasing),
        label = "anim3Alpha"
    )
    val anim3OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 250, easing = FastOutSlowInEasing),
        label = "anim3OffsetY"
    )

    val anim5Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(750, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "anim5Alpha"
    )
    val anim5OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(750, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "anim5OffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "processingInfinite")

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

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    val pingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingScale"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingAlpha"
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

    val progressShimmerX by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressShimmerX"
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = anim1Alpha
                            translationY = anim1OffsetY.dp.toPx()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandWordmark(
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale
                    )

                    var showCancelDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainer)
                            .clickable { showCancelDialog = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "cancel",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                    }

                    if (showCancelDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showCancelDialog = false },
                            title = { Text(text = "Cancel processing?") },
                            text = { Text(text = "Are you sure you want to stop processing this video?") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showCancelDialog = false
                                        onCancelProcessing()
                                    }
                                ) {
                                    Text(text = "Yes, cancel", color = IykykTheme.colors.primaryContainer)
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = { showCancelDialog = false }
                                ) {
                                    Text(text = "Continue", color = IykykTheme.colors.ink)
                                }
                            }
                        )
                    }
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
                        text = "making your collage",
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
                        text = "we're finding everyone and picking their best shots.",
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
                    ProcessingCard(
                        modifier = Modifier.fillMaxSize(),
                        videoThumbnail = videoThumbnail,
                        progressFraction = progressFraction,
                        stageText = stageText,
                        facesCount = facesCount,
                        estimatedTimeLeft = estimatedTimeLeft,
                        glowAlpha = glowAlpha,
                        glowScale = glowScale,
                        scanLineY = scanLineY,
                        pingScale = pingScale,
                        pingAlpha = pingAlpha,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        progressShimmerX = progressShimmerX
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
private fun ResultsStateView(
    contentPadding: PaddingValues,
    peopleCount: Int,
    totalAppearancesCount: Int,
    personResults: List<PersonResultItem>,
    onSeeCollage: () -> Unit,
    onChooseAnotherVideo: () -> Unit,
    onPersonClick: (PersonResultItem) -> Unit
) {
    val isInspection = LocalInspectionMode.current
    var isEntered by remember { mutableStateOf(isInspection) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    val animAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "animAlpha"
    )
    val animOffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 14f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "animOffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "resultsInfinite")

    val starScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )

    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animAlpha
                            translationY = animOffsetY.dp.toPx()
                        },
                    verticalAlignment = Alignment.CenterVertically
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
                                    fontSize = 18.sp,
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

                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animAlpha
                            translationY = animOffsetY.dp.toPx()
                        }
                ) {
                    Text(
                        text = "all done.",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 40.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.2).sp,
                            color = IykykTheme.colors.ink
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainerLow)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "we found",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                        Text(
                            text = "$peopleCount people",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = IykykTheme.colors.coral
                            )
                        )
                        Text(
                            text = "•",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.primaryFixed
                            )
                        )
                        Text(
                            text = "$totalAppearancesCount appearances",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "THE PEOPLE IN YOUR VIDEO",
                    style = TextStyle(
                        fontFamily = Jakarta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = IykykTheme.colors.mutedInk
                    ),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .graphicsLayer {
                            alpha = animAlpha
                            translationY = animOffsetY.dp.toPx()
                        }
                )
            }

            items(personResults) { person ->
                PersonCardItem(
                    person = person,
                    starScale = starScale,
                    onClick = { onPersonClick(person) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animAlpha
                            translationY = animOffsetY.dp.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val seeCollageInteraction = remember { MutableInteractionSource() }
                    val isSeeCollagePressed by seeCollageInteraction.collectIsPressedAsState()
                    val seeCollageScale by animateFloatAsState(
                        targetValue = if (isSeeCollagePressed) 0.96f else 1.0f,
                        label = "seeCollageScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(seeCollageScale)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x3319161D),
                                spotColor = Color(0x3319161D)
                            )
                            .clip(CircleShape)
                            .background(IykykTheme.colors.ink)
                            .clickable(
                                interactionSource = seeCollageInteraction,
                                indication = null
                            ) { onSeeCollage() },
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
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = sparkleRotation
                                    scaleX = sparkleScale
                                    scaleY = sparkleScale
                                }
                            ) {
                                SparkleIconYellow()
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "see my collage",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.14.sp,
                                    color = IykykTheme.colors.surface
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            ArrowForwardIcon()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val chooseAnotherInteraction = remember { MutableInteractionSource() }
                    val isChooseAnotherPressed by chooseAnotherInteraction.collectIsPressedAsState()
                    val chooseAnotherScale by animateFloatAsState(
                        targetValue = if (isChooseAnotherPressed) 0.96f else 1.0f,
                        label = "chooseAnotherScale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(chooseAnotherScale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = chooseAnotherInteraction,
                                indication = null
                            ) { onChooseAnotherVideo() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "choose another video",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LockIcon()
                        Text(
                            text = "private · everything happens on your device",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                color = IykykTheme.colors.mutedInk.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCardItem(
    person: PersonResultItem,
    starScale: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x1419161D),
                spotColor = Color(0x1419161D)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(IykykTheme.colors.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(IykykTheme.colors.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    if (person.thumbnailBitmap != null && !person.thumbnailBitmap.isRecycled) {
                        Image(
                            bitmap = person.thumbnailBitmap.asImageBitmap(),
                            contentDescription = person.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PersonAvatarFallback(id = person.id)
                    }

                    if (person.isStarShot) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .scale(starScale)
                                .size(20.dp)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(IykykTheme.colors.yellow),
                            contentAlignment = Alignment.Center
                        ) {
                            StarIconSmall()
                        }
                    }
                }

                    Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = person.name,
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.ink
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (person.isStarShot) {
                            Box(
                                modifier = Modifier
                                    .scale(starScale)
                                    .clip(CircleShape)
                                    .background(IykykTheme.colors.tertiaryFixed)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "STAR SHOT",
                                    style = TextStyle(
                                        fontFamily = Jakarta,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp,
                                        color = Color(0xFF241A00)
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "${person.appearancesCount} appearances",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = IykykTheme.colors.mutedInk
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonAvatarFallback(id: Int) {
    val avatarColor = when (id % 4) {
        1 -> IykykTheme.colors.coral
        2 -> IykykTheme.colors.purple
        3 -> IykykTheme.colors.yellow
        else -> IykykTheme.colors.teal
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(avatarColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        PersonBubble(
            modifier = Modifier.size(36.dp),
            color = avatarColor,
            size = 36.dp
        )
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
private fun ProcessingCard(
    modifier: Modifier = Modifier,
    videoThumbnail: Bitmap?,
    progressFraction: Float,
    stageText: String,
    facesCount: Int,
    estimatedTimeLeft: String,
    glowAlpha: Float,
    glowScale: Float,
    scanLineY: Float,
    pingScale: Float,
    pingAlpha: Float,
    pulseScale: Float,
    pulseAlpha: Float,
    progressShimmerX: Float
) {
    val coral = IykykTheme.colors.coral
    val purpleTarget = IykykTheme.colors.purple
    val yellowTarget = IykykTheme.colors.yellow

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
                        .background(Color(0xFF1D1A21)),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoThumbnail != null && !videoThumbnail.isRecycled) {
                        Image(
                            bitmap = videoThumbnail.asImageBitmap(),
                            contentDescription = "Analyzing video frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        VideoIllustration()
                    }

                                Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                        val lineY = size.height * scanLineY

                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    coral,
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(0f, lineY),
                            size = Size(size.width, 3.dp.toPx())
                        )
                    }

                                Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pingScale)
                                        .alpha(pingAlpha)
                                        .clip(CircleShape)
                                        .background(coral)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(coral)
                                )
                            }

                            Text(
                                text = "analyzing",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }

                                FaceTargetBox(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 30.dp, y = 80.dp)
                            .size(70.dp, 75.dp),
                        dotColor = coral
                    )

                    FaceTargetBox(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = (-10).dp, y = 50.dp)
                            .size(75.dp, 80.dp),
                        dotColor = purpleTarget
                    )

                    FaceTargetBox(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-30).dp, y = 55.dp)
                            .size(68.dp, 72.dp),
                        dotColor = yellowTarget
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                            Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (videoThumbnail != null && !videoThumbnail.isRecycled) {
                                Image(
                                    bitmap = videoThumbnail.asImageBitmap(),
                                    contentDescription = "Processing video thumbnail",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pulseScale)
                                        .alpha(pulseAlpha)
                                        .clip(CircleShape)
                                        .background(IykykTheme.colors.primaryContainer)
                                )
                                Text(
                                    text = "processing video",
                                    style = TextStyle(
                                        fontFamily = Jakarta,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = IykykTheme.colors.mutedInk
                                    )
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IykykTheme.colors.ink
                                )
                            )
                            Text(
                                text = "· $estimatedTimeLeft",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = IykykTheme.colors.mutedInk.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.ink.copy(alpha = 0.10f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            IykykTheme.colors.primaryContainer,
                                            coral
                                        )
                                    )
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val shimmerX = width * progressShimmerX

                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.6f),
                                            Color.Transparent
                                        ),
                                        start = Offset(shimmerX - width * 0.3f, 0f),
                                        end = Offset(shimmerX + width * 0.3f, size.height)
                                    )
                                )
                            }
                        }
                    }

                                Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$stageText...",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.ink
                            ),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LockIcon()
                            Text(
                                text = "this stays on your device",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = IykykTheme.colors.mutedInk
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
private fun FaceTargetBox(
    modifier: Modifier = Modifier,
    dotColor: Color
) {
    Box(
        modifier = modifier
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.80f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(6.dp)
    ) {
        FaceIcon(modifier = Modifier.align(Alignment.TopStart))

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

@Composable
private fun FaceIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val minSize = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = minSize * 0.22f,
            center = Offset(center.x, center.y - minSize * 0.15f)
        )

        drawArc(
            color = Color.White.copy(alpha = 0.9f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(center.x - minSize * 0.35f, center.y + minSize * 0.05f),
            size = Size(minSize * 0.70f, minSize * 0.60f)
        )
    }
}

@Composable
private fun SparkleIconYellow() {
    val tertiaryFixed = IykykTheme.colors.tertiaryFixed

    Canvas(modifier = Modifier.size(18.dp)) {
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
            color = tertiaryFixed
        )
    }
}

@Composable
private fun StarIconSmall() {
    Canvas(modifier = Modifier.size(12.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        val radiusOuter = minOf(size.width, size.height) / 2f
        val radiusInner = radiusOuter / 2.2f

        for (i in 0 until 10) {
            val angle = Math.toRadians((i * 36).toDouble())
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
            color = Color.White
        )
    }
}

@Composable
private fun ArrowForwardIcon() {
    val surface = IykykTheme.colors.surface

    Canvas(modifier = Modifier.size(18.dp)) {
        val width = size.width
        val height = size.height

        drawLine(
            color = surface,
            start = Offset(width * 0.15f, height * 0.50f),
            end = Offset(width * 0.85f, height * 0.50f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        val path = Path().apply {
            moveTo(width * 0.55f, height * 0.25f)
            lineTo(width * 0.85f, height * 0.50f)
            lineTo(width * 0.55f, height * 0.75f)
        }

        drawPath(
            path = path,
            color = surface,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun LockIcon() {
    val mutedInk = IykykTheme.colors.mutedInk

    Canvas(modifier = Modifier.size(12.dp)) {
        val width = size.width
        val height = size.height

        drawRoundRect(
            color = mutedInk.copy(alpha = 0.7f),
            topLeft = Offset(width * 0.20f, height * 0.42f),
            size = Size(width * 0.60f, height * 0.50f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        drawArc(
            color = mutedInk.copy(alpha = 0.7f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(width * 0.30f, height * 0.15f),
            size = Size(width * 0.40f, height * 0.50f)
        )
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

@Preview(name = "Processing State - Light", showBackground = true, showSystemUi = true)
@Composable
fun ProcessingScreenLightPreview() {
    IykykTheme(darkTheme = false) {
        ProcessingScreen(
            contentPadding = PaddingValues(0.dp),
            isCompleted = false,
            progressFraction = 0.42f,
            stageText = "finding faces",
            facesCount = 4,
            estimatedTimeLeft = "~4s left"
        )
    }
}

@Preview(name = "Processing State - Dark", showBackground = true, showSystemUi = true)
@Composable
fun ProcessingScreenDarkPreview() {
    IykykTheme(darkTheme = true) {
        ProcessingScreen(
            contentPadding = PaddingValues(0.dp),
            isCompleted = false,
            progressFraction = 0.42f,
            stageText = "finding faces",
            facesCount = 4,
            estimatedTimeLeft = "~4s left"
        )
    }
}

@Preview(name = "Results State - Light", showBackground = true, showSystemUi = true)
@Composable
fun ResultsScreenLightPreview() {
    IykykTheme(darkTheme = false) {
        ProcessingScreen(
            contentPadding = PaddingValues(0.dp),
            isCompleted = true,
            facesCount = 5,
            totalAppearancesCount = 20
        )
    }
}

@Preview(name = "Results State - Dark", showBackground = true, showSystemUi = true)
@Composable
fun ResultsScreenDarkPreview() {
    IykykTheme(darkTheme = true) {
        ProcessingScreen(
            contentPadding = PaddingValues(0.dp),
            isCompleted = true,
            facesCount = 5,
            totalAppearancesCount = 20
        )
    }
}
