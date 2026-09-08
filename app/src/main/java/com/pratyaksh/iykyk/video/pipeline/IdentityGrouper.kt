package com.pratyaksh.iykyk.video.pipeline

import com.pratyaksh.iykyk.video.ml.FaceEmbedder
import com.pratyaksh.iykyk.video.model.AppearanceSegment
import com.pratyaksh.iykyk.video.model.ClothingDescriptor
import com.pratyaksh.iykyk.video.model.FaceObservation
import com.pratyaksh.iykyk.video.model.PersonIdentity

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.min
import kotlin.math.roundToInt

class IdentityGrouper(
    private val videoProcessor: VideoProcessor,
    private val faceEmbedder: FaceEmbedder
) {
    companion object {
        private const val MAX_OBSERVATIONS_PER_SEGMENT = 5
        private const val EMBEDDING_SIZE = 512
        private const val MIN_FACE_SCORE = 0.45f
        private const val SEED_FACE_THRESHOLD = 0.60f
        private const val GROUP_EXACT_SEARCH_LIMIT = 8
        private const val MERGE_THRESHOLD = 0.55f
    }

    private data class SegmentEmbedding(val timestampMs: Long, val embedding: FloatArray)
    private data class SegmentClothing(val timestampMs: Long, val descriptor: ClothingDescriptor)
    private data class SegmentProfile(val segment: AppearanceSegment, val embeddings: List<SegmentEmbedding>, val clothing: List<SegmentClothing>)
    private data class SegmentMatch(val firstId: Int, val secondId: Int, val score: Float, val faceScore: Float, val clothingScore: Float, val strongestFace: Float, val strongestClothing: Float)
    private data class IdentitySeed(val profile: SegmentProfile)
    private data class Assignment(val segmentId: Int, val seedIndex: Int, val score: Float)

    suspend fun group(uri: Uri, appearanceSegments: List<AppearanceSegment>, onProgress: (Float) -> Unit = {}): List<PersonIdentity> {
        if (appearanceSegments.isEmpty()) return emptyList()

        val profiles = buildAllSegmentProfiles(uri = uri, appearanceSegments = appearanceSegments)
        if (profiles.isEmpty()) return emptyList()
        onProgress(0.5f)

        val seeds = discoverIdentitySeeds(profiles)
        val assignments = assignSegments(profiles = profiles, seeds = seeds)
        onProgress(0.85f)

        val rawIdentities = buildIdentities(profiles = profiles, seeds = seeds, assignments = assignments)

        val validIdentities = rawIdentities.filter { person ->
            person.appearances.size >= 2 ||
                    (person.appearances.first().endTimestampMs - person.appearances.first().startTimestampMs > 1500L)
        }

        val finalIdentities = validIdentities.mapIndexed { index, person ->
            PersonIdentity(
                id = index + 1,
                name = "Person ${index + 1}",
                prototypeEmbedding = person.prototypeEmbedding,
                appearances = person.appearances
            )
        }

        onProgress(1f)
        return finalIdentities
    }

    private suspend fun buildAllSegmentProfiles(uri: Uri, appearanceSegments: List<AppearanceSegment>): List<SegmentProfile> {
        val sortedSegments = appearanceSegments.sortedBy { it.startTimestampMs }
        return coroutineScope {
            sortedSegments.map { segment ->
                async(Dispatchers.Default) {
                    val profile = buildSegmentProfile(uri, segment)
                    profile.takeIf { it.embeddings.isNotEmpty() }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun buildSegmentProfile(uri: Uri, segment: AppearanceSegment): SegmentProfile = coroutineScope {
        val observations = selectObservations(segment)
        val clothing = mutableListOf<SegmentClothing>()

        val embeddings = observations.map { observation ->
            async(Dispatchers.Default) {
                val bitmap = videoProcessor.extractFrame(uri = uri, timestampMs = observation.timestampMs) ?: return@async null
                try {
                    val embedding = runCatching { faceEmbedder.embed(bitmap = bitmap, boundingBox = observation.face.boundingBox) }.getOrNull()
                    if (embedding != null && embedding.size == EMBEDDING_SIZE && embedding.isValidEmbedding()) {
                        SegmentEmbedding(timestampMs = observation.timestampMs, embedding = embedding)
                    } else null
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }.awaitAll().filterNotNull()

        SegmentProfile(segment = segment, embeddings = embeddings, clothing = clothing)
    }

    private fun selectObservations(segment: AppearanceSegment): List<FaceObservation> {
        val observations = segment.observations
        if (observations.size <= MAX_OBSERVATIONS_PER_SEGMENT) return observations
        val result = mutableListOf<FaceObservation>()
        val lastIndex = observations.lastIndex
        for (index in 0 until MAX_OBSERVATIONS_PER_SEGMENT) {
            val position = index.toFloat() / (MAX_OBSERVATIONS_PER_SEGMENT - 1)
            val observationIndex = (position * lastIndex).roundToInt().coerceIn(0, lastIndex)
            result += observations[observationIndex]
        }
        return result.distinctBy { it.timestampMs }
    }

    private fun discoverIdentitySeeds(profiles: List<SegmentProfile>): List<IdentitySeed> {
        val seeds = mutableListOf<IdentitySeed>()
        for (profile in profiles) {
            if (seeds.isEmpty()) {
                seeds += IdentitySeed(profile)
                continue
            }
            val strongestNonOverlappingFace = seeds.filter { seed -> !segmentsOverlap(profile.segment, seed.profile.segment) }
                .maxOfOrNull { seed -> compareSegments(profile, seed.profile)?.strongestFace ?: 0f } ?: 0f

            if (strongestNonOverlappingFace < SEED_FACE_THRESHOLD) seeds += IdentitySeed(profile)
        }
        return seeds
    }

    private fun assignSegments(profiles: List<SegmentProfile>, seeds: List<IdentitySeed>): List<Assignment> {
        val assignments = mutableListOf<Assignment>()
        seeds.forEachIndexed { index, seed -> assignments += Assignment(segmentId = seed.profile.segment.id, seedIndex = index, score = 1f) }
        val remaining = profiles.filter { profile -> seeds.none { it.profile.segment.id == profile.segment.id } }
        val overlapGroups = buildOverlapGroups(remaining)
        val groupedIds = overlapGroups.flatten().map { it.segment.id }.toSet()
        val isolated = remaining.filter { it.segment.id !in groupedIds }

        isolated.forEach { profile ->
            val best = findBestIdentity(profile, seeds, assignments)
            assignments += Assignment(segmentId = profile.segment.id, seedIndex = best.first, score = best.second)
        }

        overlapGroups.forEach { group ->
            val groupAssignments = assignOverlapGroup(group = group, seeds = seeds, assignments = assignments)
            assignments += groupAssignments
        }
        return assignments
    }

    private fun buildOverlapGroups(profiles: List<SegmentProfile>): List<List<SegmentProfile>> {
        val unvisited = profiles.associateBy { it.segment.id }.toMutableMap()
        val groups = mutableListOf<List<SegmentProfile>>()

        while (unvisited.isNotEmpty()) {
            val start = unvisited.values.first()
            val group = mutableListOf<SegmentProfile>()
            val queue = ArrayDeque<SegmentProfile>()
            queue.add(start)
            unvisited.remove(start.segment.id)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group += current
                val connected = unvisited.values.filter { segmentsOverlap(current.segment, it.segment) }.toList()
                connected.forEach {
                    unvisited.remove(it.segment.id)
                    queue.add(it)
                }
            }
            groups += group.sortedBy { it.segment.startTimestampMs }
        }
        return groups
    }

    private fun assignOverlapGroup(group: List<SegmentProfile>, seeds: List<IdentitySeed>, assignments: List<Assignment>): List<Assignment> {
        if (group.size > GROUP_EXACT_SEARCH_LIMIT) return greedyOverlapAssignment(group, seeds, assignments)

        var bestScore = Float.NEGATIVE_INFINITY
        var bestAssignment = emptyList<Pair<Int, Float>>()

        fun search(index: Int, usedSeeds: Set<Int>, current: List<Pair<Int, Float>>, score: Float) {
            if (index == group.size) {
                if (score > bestScore) {
                    bestScore = score
                    bestAssignment = current
                }
                return
            }
            val profile = group[index]
            seeds.indices.forEach { seedIndex ->
                if (seedIndex in usedSeeds) return@forEach
                val identityScore = scoreAgainstIdentity(profile = profile, seedIndex = seedIndex, seeds = seeds, assignments = assignments)
                search(index = index + 1, usedSeeds = usedSeeds + seedIndex, current = current + (seedIndex to identityScore), score = score + identityScore)
            }
        }
        search(index = 0, usedSeeds = emptySet(), current = emptyList(), score = 0f)

        val result = mutableListOf<Assignment>()
        bestAssignment.forEachIndexed { index, pair ->
            result += Assignment(segmentId = group[index].segment.id, seedIndex = pair.first, score = pair.second)
        }
        return result
    }

    private fun greedyOverlapAssignment(group: List<SegmentProfile>, seeds: List<IdentitySeed>, assignments: List<Assignment>): List<Assignment> {
        val usedSeeds = mutableSetOf<Int>()
        return group.map { profile ->
            val selected = seeds.indices.filter { it !in usedSeeds }.map { seedIndex ->
                seedIndex to scoreAgainstIdentity(profile, seedIndex, seeds, assignments)
            }.maxByOrNull { it.second } ?: (0 to scoreAgainstIdentity(profile, 0, seeds, assignments))
            usedSeeds += selected.first
            Assignment(segmentId = profile.segment.id, seedIndex = selected.first, score = selected.second)
        }
    }

    private fun findBestIdentity(profile: SegmentProfile, seeds: List<IdentitySeed>, assignments: List<Assignment>): Pair<Int, Float> {
        return seeds.indices.map { seedIndex ->
            seedIndex to scoreAgainstIdentity(profile, seedIndex, seeds, assignments)
        }.maxByOrNull { it.second } ?: (0 to 0f)
    }

    private fun scoreAgainstIdentity(profile: SegmentProfile, seedIndex: Int, seeds: List<IdentitySeed>, assignments: List<Assignment>): Float {
        val seed = seeds[seedIndex]
        if (segmentsOverlap(profile.segment, seed.profile.segment)) return 0f

        val historicalProfiles = assignments.filter { it.seedIndex == seedIndex }.mapNotNull { findSeedProfile(it.segmentId, seeds) }
        if (historicalProfiles.any { segmentsOverlap(profile.segment, it.segment) }) return 0f

        val seedMatch = compareSegments(profile, seed.profile)
        val seedScore = seedMatch?.score ?: 0f
        val directFace = seedMatch?.faceScore ?: 0f

        if (directFace < MIN_FACE_SCORE) return 0f

        val historicalScores = historicalProfiles.mapNotNull { compareSegments(profile, it)?.score }.sortedDescending()
        val consensus = if (historicalScores.isEmpty()) seedScore else historicalScores.take(min(3, historicalScores.size)).average().toFloat()
        val temporal = temporalConsistency(profile, seedIndex, seeds, assignments)

        val base = 0.60f * seedScore + 0.30f * consensus + 0.10f * temporal
        return base.coerceIn(0f, 1f)
    }

    private fun findSeedProfile(segmentId: Int, seeds: List<IdentitySeed>): SegmentProfile? {
        return seeds.firstOrNull { it.profile.segment.id == segmentId }?.profile
    }

    private fun temporalConsistency(profile: SegmentProfile, seedIndex: Int, seeds: List<IdentitySeed>, assignments: List<Assignment>): Float {
        val previous = assignments.filter { it.seedIndex == seedIndex }.mapNotNull { findSeedProfile(it.segmentId, seeds) }.filter { it.segment.startTimestampMs < profile.segment.startTimestampMs }.maxByOrNull { it.segment.startTimestampMs } ?: return 0f
        val gap = profile.segment.startTimestampMs - previous.segment.endTimestampMs
        return when {
            gap < 0L -> 0f
            gap <= 1500L -> 0f
            gap <= 3500L -> 0.15f
            gap <= 6500L -> 0.75f
            gap <= 10000L -> 1f
            gap <= 15000L -> 0.80f
            else -> 0.55f
        }
    }

    private fun compareSegments(first: SegmentProfile, second: SegmentProfile): SegmentMatch? {
        val faceScores = mutableListOf<Float>()

        first.embeddings.forEach { firstEmbedding ->
            second.embeddings.forEach { secondEmbedding ->
                val similarity = cosineSimilarity(firstEmbedding.embedding, secondEmbedding.embedding)
                if (similarity >= 0f) faceScores += similarity
            }
        }

        if (faceScores.isEmpty()) return null

        val sortedFace = faceScores.sortedDescending()
        val strongestFace = sortedFace.firstOrNull() ?: 0f
        val faceSupport = sortedFace.take(min(3, sortedFace.size)).average().toFloat()
        val faceScore = if (sortedFace.isEmpty()) 0f else 0.80f * strongestFace + 0.20f * faceSupport
        val combinedScore = if (strongestFace < MIN_FACE_SCORE) 0f else faceScore

        return SegmentMatch(
            firstId = first.segment.id,
            secondId = second.segment.id,
            score = combinedScore,
            faceScore = faceScore,
            clothingScore = 0f,
            strongestFace = strongestFace,
            strongestClothing = 0f
        )
    }

    private fun segmentsOverlap(first: AppearanceSegment, second: AppearanceSegment): Boolean {
        val overlapStart = maxOf(first.startTimestampMs, second.startTimestampMs)
        val overlapEnd = minOf(first.endTimestampMs, second.endTimestampMs)
        return (overlapEnd - overlapStart) >= 100L
    }

    private fun buildIdentities(profiles: List<SegmentProfile>, seeds: List<IdentitySeed>, assignments: List<Assignment>): List<PersonIdentity> {
        val initialIdentities = seeds.mapIndexed { seedIndex, _ ->
            val assignedProfiles = assignments.filter { it.seedIndex == seedIndex }.mapNotNull { profiles.firstOrNull { profile -> profile.segment.id == it.segmentId } }.distinctBy { it.segment.id }.sortedBy { it.segment.startTimestampMs }
            val prototype = buildPrototype(assignedProfiles)
            PersonIdentity(
                id = seedIndex + 1,
                name = "Person ${seedIndex + 1}",
                prototypeEmbedding = prototype,
                appearances = assignedProfiles.map { it.segment }.toMutableList()
            )
        }.filter { it.appearances.isNotEmpty() }

        return mergeSimilarIdentities(initialIdentities)
    }

    private fun mergeSimilarIdentities(identities: List<PersonIdentity>): List<PersonIdentity> {
        if (identities.size <= 1) return identities

        val merged = mutableListOf<PersonIdentity>()
        val skip = mutableSetOf<Int>()

        for (i in identities.indices) {
            if (identities[i].id in skip) continue
            var current = identities[i]

            for (j in i + 1 until identities.size) {
                if (identities[j].id in skip) continue
                val other = identities[j]

                val sim = cosineSimilarity(current.prototypeEmbedding, other.prototypeEmbedding)
                val overlaps = haveOverlappingAppearances(current, other)

                if (sim >= MERGE_THRESHOLD && !overlaps) {
                    current.appearances.addAll(other.appearances)
                    skip.add(other.id)
                }
            }
            merged.add(current)
        }

        return merged.mapIndexed { index, person ->
            val sortedAppearances = person.appearances.distinctBy { it.id }.sortedBy { it.startTimestampMs }.toMutableList()
            PersonIdentity(
                id = index + 1,
                name = "Person ${index + 1}",
                prototypeEmbedding = person.prototypeEmbedding,
                appearances = sortedAppearances
            )
        }
    }

    private fun haveOverlappingAppearances(a: PersonIdentity, b: PersonIdentity): Boolean {
        for (segA in a.appearances) {
            for (segB in b.appearances) {
                if (segmentsOverlap(segA, segB)) return true
            }
        }
        return false
    }

    private fun buildPrototype(profiles: List<SegmentProfile>): FloatArray {
        val embeddings = profiles.flatMap { it.embeddings }
        if (embeddings.isEmpty()) return FloatArray(EMBEDDING_SIZE)

        val result = FloatArray(EMBEDDING_SIZE)
        embeddings.forEach { item ->
            if (item.embedding.size == EMBEDDING_SIZE) {
                item.embedding.forEachIndexed { index, value -> result[index] += value }
            }
        }

        val count = embeddings.size.toFloat()
        if (count > 0f) {
            result.indices.forEach { index -> result[index] /= count }
        }
        return normalize(result)
    }

    private fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
        val size = min(first.size, second.size)
        if (size != EMBEDDING_SIZE) return -1f
        var dot = 0f
        var firstMagnitude = 0f
        var secondMagnitude = 0f

        for (index in 0 until size) {
            val firstValue = first[index]
            val secondValue = second[index]
            dot += firstValue * secondValue
            firstMagnitude += firstValue * firstValue
            secondMagnitude += secondValue * secondValue
        }

        if (firstMagnitude == 0f || secondMagnitude == 0f) return -1f
        return dot / (kotlin.math.sqrt(firstMagnitude) * kotlin.math.sqrt(secondMagnitude))
    }

    private fun normalize(embedding: FloatArray): FloatArray {
        var magnitude = 0f
        embedding.forEach { magnitude += it * it }
        magnitude = kotlin.math.sqrt(magnitude)
        if (magnitude == 0f) return embedding
        return FloatArray(embedding.size) { index -> embedding[index] / magnitude }
    }

    private fun FloatArray.isValidEmbedding(): Boolean {
        if (size != EMBEDDING_SIZE) return false
        return any { it.isFinite() && it != 0f }
    }
}