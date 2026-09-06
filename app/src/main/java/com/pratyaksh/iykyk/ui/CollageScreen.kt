package com.pratyaksh.iykyk.ui

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.core.view.WindowCompat
import com.pratyaksh.iykyk.ui.theme.IykykTheme

private val Jakarta = FontFamily.SansSerif

@Composable
fun CollageScreen(
    contentPadding: PaddingValues,
    collageBitmap: Bitmap? = null,
    representativeBitmaps: List<Bitmap> = emptyList(),
    onShareCollage: () -> Unit = {},
    onSaveCollage: () -> Unit = {},
    onMakeAnother: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    CollageScreenContent(
        contentPadding = contentPadding,
        collageBitmap = collageBitmap,
        representativeBitmaps = representativeBitmaps,
        onShareCollage = onShareCollage,
        onSaveCollage = onSaveCollage,
        onMakeAnother = onMakeAnother,
        onBackClick = onBackClick
    )
}

@Composable
fun CollageScreenContent(
    contentPadding: PaddingValues,
    collageBitmap: Bitmap? = null,
    representativeBitmaps: List<Bitmap> = emptyList(),
    onShareCollage: () -> Unit = {},
    onSaveCollage: () -> Unit = {},
    onMakeAnother: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val isInspection = LocalInspectionMode.current
    var isEntered by remember { mutableStateOf(isInspection) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    val anim1Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(600, delayMillis = 50, easing = FastOutSlowInEasing),
        label = "anim1Alpha"
    )
    val anim1OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else -12f,
        animationSpec = tween(600, delayMillis = 50, easing = FastOutSlowInEasing),
        label = "anim1OffsetY"
    )

    val anim2Alpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "anim2Alpha"
    )
    val anim2OffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(700, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "anim2OffsetY"
    )

    val animCardAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(800, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "animCardAlpha"
    )
    val animCardScale by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0.94f,
        animationSpec = tween(800, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "animCardScale"
    )

    val animButtonsAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(700, delayMillis = 650, easing = FastOutSlowInEasing),
        label = "animButtonsAlpha"
    )
    val animButtonsOffsetY by animateFloatAsState(
        targetValue = if (isEntered) 0f else 18f,
        animationSpec = tween(700, delayMillis = 650, easing = FastOutSlowInEasing),
        label = "animButtonsOffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "collageInfinite")

    val curatedScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "curatedScale"
    )

    val badgePulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgePulseScale"
    )

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
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
            // Brand Header
            item {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                    Row(
                        modifier = Modifier
                            .scale(curatedScale)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CuratedIcon()
                        Text(
                            text = "curated",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main Headline
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = anim2Alpha
                            translationY = anim2OffsetY.dp.toPx()
                        }
                ) {
                    Text(
                        text = "your people.",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.8).sp,
                            color = IykykTheme.colors.ink
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "one best shot of everyone in your video.",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = IykykTheme.colors.mutedInk
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Collage Hero Section (9:16 Card)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animCardAlpha
                            scaleX = animCardScale
                            scaleY = animCardScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CollageHeroCard(
                        collageBitmap = collageBitmap,
                        representativeBitmaps = representativeBitmaps,
                        badgePulseScale = badgePulseScale,
                        onBackClick = onBackClick
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action CTA Buttons Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animButtonsAlpha
                            translationY = animButtonsOffsetY.dp.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Share CTA
                    val shareInteraction = remember { MutableInteractionSource() }
                    val isSharePressed by shareInteraction.collectIsPressedAsState()
                    val shareScale by animateFloatAsState(
                        targetValue = if (isSharePressed) 0.96f else 1.0f,
                        label = "shareScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(shareScale)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x3319161D),
                                spotColor = Color(0x3319161D)
                            )
                            .clip(CircleShape)
                            .background(IykykTheme.colors.ink)
                            .clickable(
                                interactionSource = shareInteraction,
                                indication = null
                            ) { onShareCollage() },
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
                            ShareSparkleIcon()

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "share collage ↗",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.14.sp,
                                    color = IykykTheme.colors.surface
                                )
                            )
                        }
                    }

                    // Secondary Save CTA
                    val saveInteraction = remember { MutableInteractionSource() }
                    val isSavePressed by saveInteraction.collectIsPressedAsState()
                    val saveScale by animateFloatAsState(
                        targetValue = if (isSavePressed) 0.96f else 1.0f,
                        label = "saveScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .scale(saveScale)
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x1419161D),
                                spotColor = Color(0x1419161D)
                            )
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainer)
                            .clickable(
                                interactionSource = saveInteraction,
                                indication = null
                            ) { onSaveCollage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            DownloadIcon()

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "save to gallery",
                                style = TextStyle(
                                    fontFamily = Jakarta,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.14.sp,
                                    color = IykykTheme.colors.ink
                                )
                            )
                        }
                    }

                    val makeAnotherInteraction = remember { MutableInteractionSource() }
                    val isMakeAnotherPressed by makeAnotherInteraction.collectIsPressedAsState()
                    val makeAnotherScale by animateFloatAsState(
                        targetValue = if (isMakeAnotherPressed) 0.96f else 1.0f,
                        label = "makeAnotherScale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(makeAnotherScale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = makeAnotherInteraction,
                                indication = null
                            ) { onMakeAnother() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "make another",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LockIcon()
                        Text(
                            text = "private · everything happens on your device",
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

@Composable
private fun CollageHeroCard(
    collageBitmap: Bitmap?,
    representativeBitmaps: List<Bitmap>,
    badgePulseScale: Float,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color(0x1A19161D),
                spotColor = Color(0x1A19161D)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        IykykTheme.colors.surface,
                        IykykTheme.colors.surfaceContainerLow,
                        IykykTheme.colors.surfaceContainer
                    )
                )
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Mini Story Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "iykyk",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IykykTheme.colors.ink
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.primaryContainer)
                    )
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IykykTheme.colors.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "the crew",
                            style = TextStyle(
                                fontFamily = Jakarta,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IykykTheme.colors.mutedInk
                            )
                        )
                        Text(text = "✨", fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onBackClick() }
                        .padding(4.dp)
                ) {
                    BackArrowIcon()
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Asymmetric Bento Photo Collage Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 13.5f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(IykykTheme.colors.surfaceContainerHigh)
            ) {
                if (collageBitmap != null && !collageBitmap.isRecycled) {
                    Image(
                        bitmap = collageBitmap.asImageBitmap(),
                        contentDescription = "Generated People Collage",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BentoGridPlaceholder(
                        representativeBitmaps = representativeBitmaps,
                        badgePulseScale = badgePulseScale
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Collage Bottom Footer Brand Stamp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "made with iykyk",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IykykTheme.colors.mutedInk
                        )
                    )
                    Text(
                        text = "•",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 10.sp,
                            color = IykykTheme.colors.mutedInk.copy(alpha = 0.4f)
                        )
                    )
                    Text(
                        text = "on-device",
                        style = TextStyle(
                            fontFamily = Jakarta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = IykykTheme.colors.mutedInk.copy(alpha = 0.8f)
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.purple)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.yellow)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IykykTheme.colors.primaryContainer)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoGridPlaceholder(
    representativeBitmaps: List<Bitmap>,
    badgePulseScale: Float
) {
    val coral = IykykTheme.colors.coral
    val purple = IykykTheme.colors.purple
    val yellow = IykykTheme.colors.yellow
    val teal = IykykTheme.colors.teal
    val surfaceHigh = IykykTheme.colors.surfaceContainerHigh
    val surfaceLowest = IykykTheme.colors.surface
    val tertiaryFixed = IykykTheme.colors.tertiaryFixed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Row (7:5 Split)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tile 1
            Box(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                val bitmap1 = representativeBitmaps.getOrNull(0)
                if (bitmap1 != null && !bitmap1.isRecycled) {
                    Image(
                        bitmap = bitmap1.asImageBitmap(),
                        contentDescription = "Person 1",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PersonBubble(
                        modifier = Modifier.size(52.dp),
                        color = coral,
                        size = 52.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .scale(badgePulseScale)
                        .size(22.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(surfaceLowest.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "☀️", fontSize = 10.sp)
                }
            }

            // Tile 2
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                val bitmap2 = representativeBitmaps.getOrNull(1)
                if (bitmap2 != null && !bitmap2.isRecycled) {
                    Image(
                        bitmap = bitmap2.asImageBitmap(),
                        contentDescription = "Person 2",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PersonBubble(
                        modifier = Modifier.size(44.dp),
                        color = purple,
                        size = 44.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .scale(badgePulseScale)
                        .size(22.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(surfaceLowest.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 10.sp)
                }
            }
        }

        // Bottom Row (4:4:4 Split)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(5f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tile 3
            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                val bitmap3 = representativeBitmaps.getOrNull(2)
                if (bitmap3 != null && !bitmap3.isRecycled) {
                    Image(
                        bitmap = bitmap3.asImageBitmap(),
                        contentDescription = "Person 3",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PersonBubble(
                        modifier = Modifier.size(38.dp),
                        color = yellow,
                        size = 38.dp
                    )
                }
            }

            // Tile 4
            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                val bitmap4 = representativeBitmaps.getOrNull(3)
                if (bitmap4 != null && !bitmap4.isRecycled) {
                    Image(
                        bitmap = bitmap4.asImageBitmap(),
                        contentDescription = "Person 4",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PersonBubble(
                        modifier = Modifier.size(38.dp),
                        color = teal,
                        size = 38.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .scale(badgePulseScale)
                        .size(18.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(tertiaryFixed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💛", fontSize = 8.sp)
                }
            }

            // Tile 5
            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                val bitmap5 = representativeBitmaps.getOrNull(4)
                if (bitmap5 != null && !bitmap5.isRecycled) {
                    Image(
                        bitmap = bitmap5.asImageBitmap(),
                        contentDescription = "Person 5",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PersonBubble(
                        modifier = Modifier.size(38.dp),
                        color = coral,
                        size = 38.dp
                    )
                }
            }
        }
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
private fun CuratedIcon() {
    val tertiaryContainer = IykykTheme.colors.yellow

    Canvas(modifier = Modifier.size(14.dp)) {
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
            color = tertiaryContainer
        )
    }
}

@Composable
private fun ShareSparkleIcon() {
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
private fun DownloadIcon() {
    val ink = IykykTheme.colors.ink

    Canvas(modifier = Modifier.size(18.dp)) {
        val width = size.width
        val height = size.height

        drawCircle(
            color = ink,
            radius = minOf(width, height) / 2f,
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawLine(
            color = ink,
            start = Offset(width / 2f, height * 0.28f),
            end = Offset(width / 2f, height * 0.65f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        val path = Path().apply {
            moveTo(width * 0.32f, height * 0.50f)
            lineTo(width / 2f, height * 0.68f)
            lineTo(width * 0.68f, height * 0.50f)
        }

        drawPath(
            path = path,
            color = ink,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun BackArrowIcon() {
    val mutedInk = IykykTheme.colors.mutedInk

    Canvas(modifier = Modifier.size(15.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.20f)
            lineTo(size.width * 0.35f, size.height * 0.50f)
            lineTo(size.width * 0.65f, size.height * 0.80f)
        }

        drawPath(
            path = path,
            color = mutedInk,
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

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

@Preview(name = "Collage Screen - Light", showBackground = true, showSystemUi = true)
@Composable
fun CollageScreenLightPreview() {
    IykykTheme(darkTheme = false) {
        CollageScreenContent(
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(name = "Collage Screen - Dark", showBackground = true, showSystemUi = true)
@Composable
fun CollageScreenDarkPreview() {
    IykykTheme(darkTheme = true) {
        CollageScreenContent(
            contentPadding = PaddingValues(0.dp)
        )
    }
}
