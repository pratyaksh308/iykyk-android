package com.pratyaksh.iykyk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pratyaksh.iykyk.ui.CollageScreen
import com.pratyaksh.iykyk.ui.HomeScreen
import com.pratyaksh.iykyk.ui.ProcessingScreen
import com.pratyaksh.iykyk.ui.theme.IykykTheme
import com.pratyaksh.iykyk.viewmodel.HomeViewModel
import com.pratyaksh.iykyk.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            IykykTheme {
                val factory =
                    remember {
                        HomeViewModelFactory(
                            contentResolver = contentResolver,
                            context = applicationContext
                        )
                    }

                val homeViewModel: HomeViewModel =
                    viewModel(
                        factory = factory
                    )

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { contentPadding ->

                    IykykApp(
                        contentPadding = contentPadding,
                        viewModel = homeViewModel
                    )
                }
            }
        }
    }
}

private enum class AppScreen {
    HOME,
    PROCESSING,
    COLLAGE
}

@Composable
private fun IykykApp(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current

    var currentScreen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.COLLAGE -> {
                currentScreen = AppScreen.PROCESSING
            }
            AppScreen.PROCESSING -> {
                if (viewModel.isProcessing) {
                    android.widget.Toast.makeText(context, "Processing...", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.resetForNewVideo()
                    currentScreen = AppScreen.HOME
                }
            }
            AppScreen.HOME -> {
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(viewModel.isProcessing) {
        if (viewModel.isProcessing) {
            currentScreen = AppScreen.PROCESSING
        }
    }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                contentPadding = contentPadding
            )
        }

        AppScreen.PROCESSING -> {
            ProcessingScreen(
                contentPadding = contentPadding,
                isCompleted = viewModel.processingCompleted,
                videoThumbnail = viewModel.videoThumbnailBitmap,
                progressFraction = viewModel.processingProgress,
                stageText = viewModel.processingStage,
                facesCount = viewModel.processingFacesCount,
                totalAppearancesCount =
                    viewModel.appearanceSegments.size,
                estimatedTimeLeft =
                    viewModel.processingEstimatedTimeLeft,
                personResults = viewModel.personResults,
                onSeeCollage = {
                    currentScreen = AppScreen.COLLAGE
                },
                onChooseAnotherVideo = {
                    viewModel.resetForNewVideo()
                    currentScreen = AppScreen.HOME
                },
                onCancelProcessing = {
                    viewModel.cancelProcessing()
                    viewModel.resetForNewVideo()
                    currentScreen = AppScreen.HOME
                }
            )
        }

        AppScreen.COLLAGE -> {
            CollageScreen(
                contentPadding = contentPadding,
                collageBitmap = viewModel.collageBitmap,
                representativeBitmaps =
                    viewModel.representativeBitmaps,
                onShareCollage = {
                    android.widget.Toast.makeText(context, "Preparing collage to share...", android.widget.Toast.LENGTH_SHORT).show()
                    shareCollage(context, viewModel)
                },
                onSaveCollage = {
                    android.widget.Toast.makeText(context, "Saving collage to gallery...", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.saveCollage()
                },
                onMakeAnother = {
                    viewModel.resetForNewVideo()
                    currentScreen = AppScreen.HOME
                },
                onBackClick = {
                    currentScreen = AppScreen.PROCESSING
                }
            )
        }
    }
}

private fun shareCollage(
    context: Context,
    viewModel: HomeViewModel
) {
    val uri =
        viewModel.getCollageShareUri()

    if (uri == null) {
        android.widget.Toast.makeText(context, "Saving collage before sharing...", android.widget.Toast.LENGTH_SHORT).show()
        viewModel.saveCollage()
        return
    }

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Share collage"
        )
    )
}