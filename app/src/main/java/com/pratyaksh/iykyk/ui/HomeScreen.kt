package com.pratyaksh.iykyk.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pratyaksh.iykyk.viewmodel.HomeViewModel
import com.pratyaksh.iykyk.viewmodel.HomeViewModelFactory

@Composable
fun HomeScreen(
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    val factory =
        HomeViewModelFactory(
            contentResolver = context.contentResolver,
            context = context
        )

    val homeViewModel: HomeViewModel =
        viewModel(
            factory = factory
        )

    val videoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            homeViewModel.onVideoSelected(uri)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Text(
            text = "Home Screen"
        )

        Button(
            onClick = {
                videoPickerLauncher.launch("video/*")
            }
        ) {
            Text(
                text = "Select Video"
            )
        }

        if (homeViewModel.selectedVideoUri != null) {
            Text(
                text = "Video Name: ${homeViewModel.selectedVideoName}"
            )

            Button(
                onClick = {
                    homeViewModel.runEmbeddingTest()
                }
            ) {
                Text(
                    text = "Test Face Embedding"
                )
            }

            Button(
                onClick = {
                    homeViewModel.runSegmentationTest()
                }
            ) {
                Text(
                    text = "Process Video"
                )
            }
        }

        if (homeViewModel.representativeFrames.isNotEmpty()) {
            Text(
                text = "Representative Frames: ${homeViewModel.representativeFrames.size}"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(homeViewModel.representativeFrames) { index, representative ->
                    val bitmap =
                        homeViewModel.representativeBitmaps
                            .getOrNull(index)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Appearance ${index + 1}"
                        )

                        Text(
                            text =
                                "Timestamp: ${representative.timestampMs}ms · " +
                                        "Score: ${"%.3f".format(representative.score)}"
                        )

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}