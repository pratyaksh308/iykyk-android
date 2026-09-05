package com.pratyaksh.iykyk.video

data class FrameDetection(
    val timestampMs: Long,
    val faces: List<DetectedFace>
)