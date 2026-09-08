package com.pratyaksh.iykyk.viewmodel

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pratyaksh.iykyk.video.collage.CollageGenerator
import com.pratyaksh.iykyk.video.ml.FaceDetector
import com.pratyaksh.iykyk.video.ml.FaceEmbedder
import com.pratyaksh.iykyk.video.pipeline.AppearanceSegmenter
import com.pratyaksh.iykyk.video.pipeline.IdentityGrouper
import com.pratyaksh.iykyk.video.pipeline.RepresentativeFrameLoader
import com.pratyaksh.iykyk.video.pipeline.RepresentativeFrameSelector
import com.pratyaksh.iykyk.video.pipeline.VideoProcessor

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

            val appearanceSegmenter =
                AppearanceSegmenter(
                    videoProcessor = videoProcessor,
                    faceEmbedder = faceEmbedder
                )

            val identityGrouper =
                IdentityGrouper(
                    videoProcessor = videoProcessor,
                    faceEmbedder = faceEmbedder
                )

            val representativeFrameSelector =
                RepresentativeFrameSelector()

            val representativeFrameLoader =
                RepresentativeFrameLoader(
                    videoProcessor = videoProcessor
                )

            val collageGenerator =
                CollageGenerator()

            return HomeViewModel(
                videoProcessor = videoProcessor,
                faceEmbedder = faceEmbedder,
                appearanceSegmenter = appearanceSegmenter,
                identityGrouper = identityGrouper,
                representativeFrameSelector = representativeFrameSelector,
                representativeFrameLoader = representativeFrameLoader,
                collageGenerator = collageGenerator,
                context = context.applicationContext
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}