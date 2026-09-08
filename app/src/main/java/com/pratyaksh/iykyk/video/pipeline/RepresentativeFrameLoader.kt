package com.pratyaksh.iykyk.video.pipeline

import android.graphics.Bitmap
import android.net.Uri

class RepresentativeFrameLoader(
    private val videoProcessor: VideoProcessor
) {
    fun loadFrames(
        uri: Uri,
        timestampsMs: List<Long>
    ): Map<Long, Bitmap> {
        return videoProcessor.batchExtractFrames(
            uri = uri,
            timestampsMs = timestampsMs
        )
    }

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
        val timestamps = representativeFrames.map { it.timestampMs }
        val frameMap = videoProcessor.batchExtractFrames(
            uri = uri,
            timestampsMs = timestamps
        )
        return representativeFrames.mapNotNull { frameMap[it.timestampMs] }
    }
}