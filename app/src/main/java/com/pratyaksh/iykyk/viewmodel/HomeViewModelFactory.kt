package com.pratyaksh.iykyk.viewmodel

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pratyaksh.iykyk.video.FaceDetector
import com.pratyaksh.iykyk.video.VideoProcessor

class HomeViewModelFactory(
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            val faceDetector = FaceDetector()

            val videoProcessor = VideoProcessor(
                contentResolver = contentResolver,
                faceDetector = faceDetector
            )

            return HomeViewModel(videoProcessor) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}