package com.pratyaksh.iykyk.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyaksh.iykyk.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoProcessor: VideoProcessor
) : ViewModel() {

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    var selectedVideoName by mutableStateOf<String?>(null)
        private set

    var extractedFrames by mutableStateOf<List<Bitmap>>(emptyList())
        private set

    fun onVideoSelected(uri: Uri?) {
        selectedVideoUri = uri

        if (uri == null) {
            selectedVideoName = null
            extractedFrames = emptyList()
            return
        }

        selectedVideoName = videoProcessor.getVideoName(uri)

        viewModelScope.launch(Dispatchers.IO) {
            extractedFrames = videoProcessor.extractFrames(uri)
        }
    }

    override fun onCleared() {
        videoProcessor.close()
        super.onCleared()
    }
}