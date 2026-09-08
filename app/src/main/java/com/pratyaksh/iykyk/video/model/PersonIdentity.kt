package com.pratyaksh.iykyk.video.model

data class PersonIdentity(
    val id: Int,
    val name: String,
    val prototypeEmbedding: FloatArray,
    val appearances: MutableList<AppearanceSegment>
)