package com.pratyaksh.iykyk.viewmodel

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pratyaksh.iykyk.video.FaceDetector
import com.pratyaksh.iykyk.video.FaceEmbedder
import com.pratyaksh.iykyk.video.FaceEmbeddingTester
import com.pratyaksh.iykyk.video.VideoProcessor

class HomeViewModelFactory(
    private val contentResolver: ContentResolver,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                HomeViewModel::class.java
            )
        ) {
            val faceDetector =
                FaceDetector()

            val videoProcessor =
                VideoProcessor(
                    contentResolver = contentResolver,
                    faceDetector = faceDetector
                )

            val faceEmbedder =
                FaceEmbedder(context)

            val faceEmbeddingTester =
                FaceEmbeddingTester(
                    videoProcessor = videoProcessor,
                    faceEmbedder = faceEmbedder
                )

            return HomeViewModel(
                videoProcessor = videoProcessor,
                faceEmbeddingTester = faceEmbeddingTester,
                faceEmbedder = faceEmbedder
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}