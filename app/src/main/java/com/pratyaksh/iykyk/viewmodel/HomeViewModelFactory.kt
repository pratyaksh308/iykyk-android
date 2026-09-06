package com.pratyaksh.iykyk.viewmodel

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pratyaksh.iykyk.video.AppearanceSegmenter
import com.pratyaksh.iykyk.video.CollageGenerator
import com.pratyaksh.iykyk.video.FaceDetector
import com.pratyaksh.iykyk.video.FaceEmbedder
import com.pratyaksh.iykyk.video.FaceEmbeddingTester
import com.pratyaksh.iykyk.video.IdentityGrouper
import com.pratyaksh.iykyk.video.IdentityProfileCache
import com.pratyaksh.iykyk.video.RepresentativeFrameLoader
import com.pratyaksh.iykyk.video.RepresentativeFrameSelector
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

            val appearanceSegmenter =
                AppearanceSegmenter(
                    videoProcessor = videoProcessor,
                    faceEmbedder = faceEmbedder
                )

            val identityProfileCache =
                IdentityProfileCache(
                    context.applicationContext
                )

            val identityGrouper =
                IdentityGrouper(
                    videoProcessor = videoProcessor,
                    faceEmbedder = faceEmbedder,
                    profileCache = identityProfileCache
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
                faceEmbeddingTester = faceEmbeddingTester,
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