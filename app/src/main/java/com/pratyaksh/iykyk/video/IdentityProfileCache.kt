package com.pratyaksh.iykyk.video

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class IdentityProfileCache(
    context: Context
) {

    private val cacheFile =
        File(
            context.cacheDir,
            "identity_profiles.cache"
        )

    fun save(
        uri: Uri,
        profiles: List<IdentityProfileData>
    ) {
        DataOutputStream(
            FileOutputStream(cacheFile)
        ).use { output ->

            output.writeInt(VERSION)
            output.writeUTF(buildCacheKey(uri, profiles))
            output.writeInt(profiles.size)

            profiles.forEach { profile ->

                writeSegment(
                    output,
                    profile.segment
                )

                output.writeInt(
                    profile.embeddings.size
                )

                profile.embeddings.forEach { embedding ->

                    output.writeLong(
                        embedding.timestampMs
                    )

                    output.writeInt(
                        embedding.embedding.size
                    )

                    embedding.embedding.forEach {
                        output.writeFloat(it)
                    }
                }

                output.writeInt(
                    profile.clothing.size
                )

                profile.clothing.forEach { clothing ->

                    output.writeLong(
                        clothing.timestampMs
                    )

                    output.writeInt(
                        clothing.descriptor.values.size
                    )

                    clothing.descriptor.values.forEach {
                        output.writeFloat(it)
                    }
                }
            }
        }
    }

    fun load(
        uri: Uri,
        appearanceSegments: List<AppearanceSegment>
    ): List<IdentityProfileData>? {

        if (!cacheFile.exists()) {
            return null
        }

        return runCatching {

            DataInputStream(
                FileInputStream(cacheFile)
            ).use { input ->

                val version =
                    input.readInt()

                if (version != VERSION) {
                    return@use null
                }

                val storedKey =
                    input.readUTF()

                val expectedKey =
                    buildCacheKey(
                        uri,
                        appearanceSegments.map {
                            IdentityProfileData(
                                segment = it,
                                embeddings = emptyList(),
                                clothing = emptyList()
                            )
                        }
                    )

                if (storedKey != expectedKey) {
                    return@use null
                }

                val profileCount =
                    input.readInt()

                val profiles =
                    mutableListOf<IdentityProfileData>()

                repeat(profileCount) {

                    val segment =
                        readSegment(input)

                    val embeddingCount =
                        input.readInt()

                    val embeddings =
                        mutableListOf<IdentityEmbeddingData>()

                    repeat(embeddingCount) {

                        val timestampMs =
                            input.readLong()

                        val size =
                            input.readInt()

                        val embedding =
                            FloatArray(size)

                        for (index in 0 until size) {
                            embedding[index] =
                                input.readFloat()
                        }

                        embeddings +=
                            IdentityEmbeddingData(
                                timestampMs = timestampMs,
                                embedding = embedding
                            )
                    }

                    val clothingCount =
                        input.readInt()

                    val clothing =
                        mutableListOf<IdentityClothingData>()

                    repeat(clothingCount) {

                        val timestampMs =
                            input.readLong()

                        val size =
                            input.readInt()

                        val values =
                            FloatArray(size)

                        for (index in 0 until size) {
                            values[index] =
                                input.readFloat()
                        }

                        clothing +=
                            IdentityClothingData(
                                timestampMs = timestampMs,
                                descriptor =
                                    ClothingDescriptor(
                                        values = values
                                    )
                            )
                    }

                    profiles +=
                        IdentityProfileData(
                            segment = segment,
                            embeddings = embeddings,
                            clothing = clothing
                        )
                }

                profiles
            }
        }.getOrNull()
    }

    fun clear() {
        cacheFile.delete()
    }

    private fun buildCacheKey(
        uri: Uri,
        profiles: List<IdentityProfileData>
    ): String {

        val builder =
            StringBuilder()

        builder.append(uri.toString())

        profiles
            .sortedBy {
                it.segment.id
            }
            .forEach { profile ->

                val segment =
                    profile.segment

                builder.append("|")
                builder.append(segment.id)
                builder.append("|")
                builder.append(
                    segment.startTimestampMs
                )
                builder.append("|")
                builder.append(
                    segment.endTimestampMs
                )

                segment.observations.forEach { observation ->

                    builder.append("|")
                    builder.append(
                        observation.timestampMs
                    )

                    val box =
                        observation.face.boundingBox

                    builder.append("|")
                    builder.append(box.left)
                    builder.append("|")
                    builder.append(box.top)
                    builder.append("|")
                    builder.append(box.right)
                    builder.append("|")
                    builder.append(box.bottom)
                }
            }

        return sha256(
            builder.toString()
        )
    }

    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        return digest
            .digest(
                value.toByteArray()
            )
            .joinToString("") {
                "%02x".format(it)
            }
    }

    private fun writeSegment(
        output: DataOutputStream,
        segment: AppearanceSegment
    ) {

        output.writeInt(
            segment.id
        )

        output.writeLong(
            segment.startTimestampMs
        )

        output.writeLong(
            segment.endTimestampMs
        )

        output.writeInt(
            segment.observations.size
        )

        segment.observations.forEach { observation ->

            output.writeLong(
                observation.timestampMs
            )

            writeFace(
                output,
                observation.face
            )
        }
    }

    private fun readSegment(
        input: DataInputStream
    ): AppearanceSegment {

        val id =
            input.readInt()

        val startTimestampMs =
            input.readLong()

        val endTimestampMs =
            input.readLong()

        val observationCount =
            input.readInt()

        val observations =
            mutableListOf<FaceObservation>()

        repeat(observationCount) {

            val timestampMs =
                input.readLong()

            val face =
                readFace(input)

            observations +=
                FaceObservation(
                    timestampMs = timestampMs,
                    face = face
                )
        }

        return AppearanceSegment(
            id = id,
            startTimestampMs = startTimestampMs,
            endTimestampMs = endTimestampMs,
            observations = observations
        )
    }

    private fun writeFace(
        output: DataOutputStream,
        face: DetectedFace
    ) {

        val box =
            face.boundingBox

        output.writeInt(box.left)
        output.writeInt(box.top)
        output.writeInt(box.right)
        output.writeInt(box.bottom)

        writeNullableFloat(
            output,
            face.headEulerAngleX
        )

        writeNullableFloat(
            output,
            face.headEulerAngleY
        )

        writeNullableFloat(
            output,
            face.headEulerAngleZ
        )

        writeNullableFloat(
            output,
            face.leftEyeOpenProbability
        )

        writeNullableFloat(
            output,
            face.rightEyeOpenProbability
        )

        writeNullableFloat(
            output,
            face.smilingProbability
        )
    }

    private fun readFace(
        input: DataInputStream
    ): DetectedFace {

        val left =
            input.readInt()

        val top =
            input.readInt()

        val right =
            input.readInt()

        val bottom =
            input.readInt()

        return DetectedFace(
            boundingBox =
                Rect(
                    left,
                    top,
                    right,
                    bottom
                ),
            headEulerAngleX =
                readNullableFloat(input),
            headEulerAngleY =
                readNullableFloat(input),
            headEulerAngleZ =
                readNullableFloat(input),
            leftEyeOpenProbability =
                readNullableFloat(input),
            rightEyeOpenProbability =
                readNullableFloat(input),
            smilingProbability =
                readNullableFloat(input)
        )
    }

    private fun writeNullableFloat(
        output: DataOutputStream,
        value: Float?
    ) {

        if (value == null) {
            output.writeBoolean(false)
        } else {
            output.writeBoolean(true)
            output.writeFloat(value)
        }
    }

    private fun readNullableFloat(
        input: DataInputStream
    ): Float? {

        return if (input.readBoolean()) {
            input.readFloat()
        } else {
            null
        }
    }

    companion object {
        private const val VERSION = 2
    }
}

data class IdentityProfileData(
    val segment: AppearanceSegment,
    val embeddings: List<IdentityEmbeddingData>,
    val clothing: List<IdentityClothingData>
)

data class IdentityEmbeddingData(
    val timestampMs: Long,
    val embedding: FloatArray
)

data class IdentityClothingData(
    val timestampMs: Long,
    val descriptor: ClothingDescriptor
)