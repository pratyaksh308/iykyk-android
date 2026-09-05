package com.pratyaksh.iykyk.ui

import android.net.Uri
import com.pratyaksh.iykyk.viewmodel.HomeViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    homeViewModel: HomeViewModel = viewModel()
) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        homeViewModel.onVideoSelected(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Text(text = "Home Screen")

        Button(
            onClick = {
                videoPickerLauncher.launch("video/*")
            }
        ) {
            Text(text = "Select Video")
        }

        if (homeViewModel.selectedVideoUri != null) {
            Text(
                text = "Video Name: ${homeViewModel.selectedVideoName}"
            )
        }
    }
}