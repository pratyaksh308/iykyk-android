package com.pratyaksh.iykyk.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.google.mlkit.vision.face.Face

class VideoProcessor(
    private val contentResolver: ContentResolver,
    private val faceDetector: FaceDetector
) {

    fun getVideoName(uri: Uri): String? {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                return cursor.getString(nameIndex)
            }
        }

        return null
    }

    fun extractFrames(
        uri: Uri,
        intervalMs: Long = 200L
    ): List<Bitmap> {

        val retriever = MediaMetadataRetriever()

        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever.setDataSource(pfd.fileDescriptor)
        }

        val duration = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLong() ?: 0L

        val frames = mutableListOf<Bitmap>()

        var timeMs = 0L

        while (timeMs < duration) {
            retriever.getFrameAtTime(
                timeMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )?.let { frame ->
                frames.add(frame)
            }

            timeMs += intervalMs
        }

        retriever.release()

        return frames
    }

    suspend fun detectFacesInFrames(
        frames: List<Bitmap>
    ): List<List<Face>> {
        return frames.map { frame ->
            faceDetector.detectFaces(frame)
        }
    }

    fun close() {
        faceDetector.close()
    }
}