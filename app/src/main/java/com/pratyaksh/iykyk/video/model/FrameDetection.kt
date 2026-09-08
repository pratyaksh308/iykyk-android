package com.pratyaksh.iykyk.video.model

data class FrameDetection(
    val timestampMs: Long,
    val faces: List<DetectedFace>
)