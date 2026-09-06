package com.pratyaksh.iykyk.video

data class FaceObservation(
    val timestampMs: Long,
    val face: DetectedFace
)

data class AppearanceSegment(
    val id: Int,
    val startTimestampMs: Long,
    val endTimestampMs: Long,
    val observations: List<FaceObservation>
) {
    val durationMs: Long
        get() = endTimestampMs - startTimestampMs

    val observationCount: Int
        get() = observations.size
}