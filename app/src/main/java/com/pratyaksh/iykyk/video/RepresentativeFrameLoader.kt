package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import android.net.Uri

class RepresentativeFrameLoader(
    private val videoProcessor: VideoProcessor
) {
    fun loadFrame(
        uri: Uri,
        timestampMs: Long
    ): Bitmap? {
        return videoProcessor.extractFrame(
            uri = uri,
            timestampMs = timestampMs
        )
    }

    fun load(
        uri: Uri,
        representativeFrames: List<RepresentativeFrame>
    ): List<Bitmap> {
        return representativeFrames.mapNotNull { representative ->
            videoProcessor.extractFrame(
                uri = uri,
                timestampMs = representative.timestampMs
            )
        }
    }
}