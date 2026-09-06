package com.pratyaksh.iykyk.video

import android.net.Uri
import android.util.Log
import kotlin.math.min
import kotlin.math.roundToInt

class IdentityGrouper(
    private val videoProcessor: VideoProcessor,
    private val faceEmbedder: FaceEmbedder,
    private val profileCache: IdentityProfileCache
) {

    companion object {
        private const val TAG = "IdentityGrouper"
        private const val MAX_OBSERVATIONS_PER_SEGMENT = 5
        private const val STRONG_FACE_SCORE = 0.58f
        private const val MIN_FACE_SCORE = 0.35f
        private const val STRONG_FACE_WEIGHT = 0.85f
        private const val STRONG_CLOTHING_WEIGHT = 0.15f
        private const val NORMAL_FACE_WEIGHT = 0.70f
        private const val NORMAL_CLOTHING_WEIGHT = 0.30f
        private const val WEAK_FACE_WEIGHT = 0.70f
        private const val WEAK_CLOTHING_WEIGHT = 0.30f
        private const val SEED_FACE_THRESHOLD = 0.60f
        private const val GROUP_EXACT_SEARCH_LIMIT = 8
    }

    private data class SegmentEmbedding(
        val timestampMs: Long,
        val embedding: FloatArray
    )

    private data class SegmentClothing(
        val timestampMs: Long,
        val descriptor: ClothingDescriptor
    )

    private data class SegmentProfile(
        val segment: AppearanceSegment,
        val embeddings: List<SegmentEmbedding>,
        val clothing: List<SegmentClothing>
    )

    private data class SegmentMatch(
        val firstId: Int,
        val secondId: Int,
        val score: Float,
        val faceScore: Float,
        val clothingScore: Float,
        val strongestFace: Float,
        val strongestClothing: Float
    )

    private data class IdentitySeed(
        val profile: SegmentProfile
    )

    private data class Assignment(
        val segmentId: Int,
        val seedIndex: Int,
        val score: Float
    )

    private val clothingExtractor =
        ClothingDescriptorExtractor()

    suspend fun group(
        uri: Uri,
        appearanceSegments: List<AppearanceSegment>,
        onProgress: (Float) -> Unit = {}
    ): List<PersonIdentity> {

        Log.d(
            TAG,
            "=================== IDENTITY GROUPING START ==================="
        )

        Log.d(
            TAG,
            "Input appearance segments=${appearanceSegments.size}"
        )

        if (appearanceSegments.isEmpty()) {
            return emptyList()
        }

        val profiles =
            loadOrBuildProfiles(
                uri = uri,
                appearanceSegments = appearanceSegments
            )

        if (profiles.isEmpty()) {
            return emptyList()
        }

        onProgress(0.3f)

        Log.d(
            TAG,
            "Profiles ready=${profiles.size}"
        )

        val matches =
            buildAllMatches(profiles)

        onProgress(0.6f)

        logBestMatches(
            profiles,
            matches
        )

        val seeds =
            discoverIdentitySeeds(
                profiles,
                matches
            )

        Log.d(
            TAG,
            "=================== IDENTITY SEEDS ==================="
        )

        seeds.forEachIndexed { index, seed ->
            Log.d(
                TAG,
                "Seed ${index + 1}: Segment #${seed.profile.segment.id}"
            )
        }

        val assignments =
            assignSegments(
                profiles = profiles,
                seeds = seeds,
                matches = matches
            )

        onProgress(0.85f)

        Log.d(
            TAG,
            "=================== FINAL SEGMENT ASSIGNMENTS ==================="
        )

        assignments
            .sortedBy {
                it.segmentId
            }
            .forEach { assignment ->

                Log.d(
                    TAG,
                    "Segment #${assignment.segmentId} -> " +
                            "Person ${assignment.seedIndex + 1}, " +
                            "score=${"%.4f".format(assignment.score)}"
                )
            }

        val identities =
            buildIdentities(
                profiles = profiles,
                seeds = seeds,
                assignments = assignments
            )

        Log.d(
            TAG,
            "=================== IDENTITY GROUPING RESULTS ==================="
        )

        Log.d(
            TAG,
            "Total input appearance segments=${appearanceSegments.size}"
        )

        Log.d(
            TAG,
            "Final number of unique identities=${identities.size}"
        )

        identities.forEach { identity ->

            Log.d(
                TAG,
                "${identity.name}: " +
                        "${identity.appearances.size} appearances -> " +
                        "Segments=${identity.appearances.map { it.id }}"
            )
        }

        Log.d(
            TAG,
            "=================================================================="
        )

        onProgress(1f)
        return identities
    }

    private suspend fun loadOrBuildProfiles(
        uri: Uri,
        appearanceSegments: List<AppearanceSegment>
    ): List<SegmentProfile> {

        val cached =
            profileCache.load(
                uri = uri,
                appearanceSegments = appearanceSegments
            )

        if (cached != null) {

            Log.d(
                TAG,
                "IDENTITY PROFILE CACHE HIT"
            )

            Log.d(
                TAG,
                "Loading ${cached.size} cached profiles"
            )

            return cached.map {
                fromCacheData(it)
            }
        }

        Log.d(
            TAG,
            "IDENTITY PROFILE CACHE MISS"
        )

        Log.d(
            TAG,
            "Building identity profiles from video"
        )

        val profiles =
            appearanceSegments
                .sortedBy {
                    it.startTimestampMs
                }
                .mapNotNull { segment ->

                    val profile =
                        buildSegmentProfile(
                            uri,
                            segment
                        )

                    Log.d(
                        TAG,
                        "Segment #${segment.id}: " +
                                "observations=${segment.observationCount}, " +
                                "faceEmbeddings=${profile.embeddings.size}, " +
                                "clothingDescriptors=${profile.clothing.size}"
                    )

                    profile.takeIf {
                        it.embeddings.isNotEmpty() ||
                                it.clothing.isNotEmpty()
                    }
                }

        profileCache.save(
            uri = uri,
            profiles =
                profiles.map {
                    toCacheData(it)
                }
        )

        Log.d(
            TAG,
            "IDENTITY PROFILE CACHE SAVED"
        )

        return profiles
    }

    private suspend fun buildSegmentProfile(
        uri: Uri,
        segment: AppearanceSegment
    ): SegmentProfile {

        val observations =
            selectObservations(segment)

        val embeddings =
            mutableListOf<SegmentEmbedding>()

        val clothing =
            mutableListOf<SegmentClothing>()

        observations.forEach { observation ->

            val bitmap =
                videoProcessor.extractFrame(
                    uri = uri,
                    timestampMs = observation.timestampMs
                )
                    ?: return@forEach

            try {

                val embedding =
                    runCatching {

                        faceEmbedder.embed(
                            bitmap = bitmap,
                            boundingBox =
                                observation.face.boundingBox
                        )
                    }.getOrNull()

                if (
                    embedding != null &&
                    embedding.isValidEmbedding()
                ) {

                    embeddings +=
                        SegmentEmbedding(
                            timestampMs =
                                observation.timestampMs,
                            embedding =
                                embedding
                        )
                }

                val clothingDescriptor =
                    runCatching {

                        clothingExtractor.extract(
                            bitmap = bitmap,
                            faceBounds =
                                observation.face.boundingBox
                        )
                    }.getOrNull()

                if (clothingDescriptor != null) {

                    clothing +=
                        SegmentClothing(
                            timestampMs =
                                observation.timestampMs,
                            descriptor =
                                clothingDescriptor
                        )
                }
            } finally {

                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }

        return SegmentProfile(
            segment = segment,
            embeddings = embeddings,
            clothing = clothing
        )
    }

    private fun toCacheData(
        profile: SegmentProfile
    ): IdentityProfileData {

        return IdentityProfileData(
            segment = profile.segment,
            embeddings =
                profile.embeddings.map {
                    IdentityEmbeddingData(
                        timestampMs =
                            it.timestampMs,
                        embedding =
                            it.embedding
                    )
                },
            clothing =
                profile.clothing.map {
                    IdentityClothingData(
                        timestampMs =
                            it.timestampMs,
                        descriptor =
                            it.descriptor
                    )
                }
        )
    }

    private fun fromCacheData(
        profile: IdentityProfileData
    ): SegmentProfile {

        return SegmentProfile(
            segment = profile.segment,
            embeddings =
                profile.embeddings.map {
                    SegmentEmbedding(
                        timestampMs =
                            it.timestampMs,
                        embedding =
                            it.embedding
                    )
                },
            clothing =
                profile.clothing.map {
                    SegmentClothing(
                        timestampMs =
                            it.timestampMs,
                        descriptor =
                            it.descriptor
                    )
                }
        )
    }

    private fun selectObservations(
        segment: AppearanceSegment
    ): List<FaceObservation> {

        val observations =
            segment.observations

        if (
            observations.size <=
            MAX_OBSERVATIONS_PER_SEGMENT
        ) {
            return observations
        }

        val result =
            mutableListOf<FaceObservation>()

        val lastIndex =
            observations.lastIndex

        for (
        index in 0 until
                MAX_OBSERVATIONS_PER_SEGMENT
        ) {

            val position =
                index.toFloat() /
                        (
                                MAX_OBSERVATIONS_PER_SEGMENT -
                                        1
                                )

            val observationIndex =
                (
                        position *
                                lastIndex
                        )
                    .roundToInt()
                    .coerceIn(
                        0,
                        lastIndex
                    )

            result +=
                observations[
                    observationIndex
                ]
        }

        return result.distinctBy {
            it.timestampMs
        }
    }

    private fun buildAllMatches(
        profiles: List<SegmentProfile>
    ): List<SegmentMatch> {

        val matches =
            mutableListOf<SegmentMatch>()

        for (
        firstIndex in profiles.indices
        ) {

            for (
            secondIndex in
            firstIndex + 1 until profiles.size
            ) {

                val first =
                    profiles[firstIndex]

                val second =
                    profiles[secondIndex]

                if (
                    segmentsOverlap(
                        first.segment,
                        second.segment
                    )
                ) {
                    continue
                }

                compareSegments(
                    first,
                    second
                )?.let {
                    matches += it
                }
            }
        }

        return matches
    }

    private fun logBestMatches(
        profiles: List<SegmentProfile>,
        matches: List<SegmentMatch>
    ) {

        Log.d(
            TAG,
            "=================== BEST MATCHES WITH CLOTHING ==================="
        )

        profiles.forEach { profile ->

            matches
                .filter {
                    it.firstId ==
                            profile.segment.id ||
                            it.secondId ==
                            profile.segment.id
                }
                .sortedByDescending {
                    it.score
                }
                .take(5)
                .forEachIndexed { index, match ->

                    val otherId =
                        if (
                            match.firstId ==
                            profile.segment.id
                        ) {
                            match.secondId
                        } else {
                            match.firstId
                        }

                    Log.d(
                        TAG,
                        "Segment #${profile.segment.id} " +
                                "best[$index] -> " +
                                "Segment #$otherId, " +
                                "combined=${"%.4f".format(match.score)}, " +
                                "face=${"%.4f".format(match.faceScore)}, " +
                                "clothing=${"%.4f".format(match.clothingScore)}, " +
                                "strongestFace=${"%.4f".format(match.strongestFace)}, " +
                                "strongestClothing=${"%.4f".format(match.strongestClothing)}"
                    )
                }
        }
    }

    private fun discoverIdentitySeeds(
        profiles: List<SegmentProfile>,
        matches: List<SegmentMatch>
    ): List<IdentitySeed> {

        val seeds =
            mutableListOf<IdentitySeed>()

        for (profile in profiles) {

            if (seeds.isEmpty()) {
                seeds +=
                    IdentitySeed(profile)
                continue
            }

            val strongestNonOverlappingFace =
                seeds
                    .filter { seed ->
                        !segmentsOverlap(profile.segment, seed.profile.segment)
                    }
                    .maxOfOrNull { seed ->
                        compareSegments(
                            profile,
                            seed.profile
                        )?.strongestFace ?: 0f
                    } ?: 0f

            if (
                strongestNonOverlappingFace <
                SEED_FACE_THRESHOLD
            ) {
                seeds +=
                    IdentitySeed(profile)
            }
        }

        return seeds
    }

    private fun assignSegments(
        profiles: List<SegmentProfile>,
        seeds: List<IdentitySeed>,
        matches: List<SegmentMatch>
    ): List<Assignment> {

        val assignments =
            mutableListOf<Assignment>()

        seeds.forEachIndexed { index, seed ->

            assignments +=
                Assignment(
                    segmentId =
                        seed.profile.segment.id,
                    seedIndex = index,
                    score = 1f
                )
        }

        val remaining =
            profiles.filter { profile ->

                seeds.none {
                    it.profile.segment.id ==
                            profile.segment.id
                }
            }

        val overlapGroups =
            buildOverlapGroups(
                remaining
            )

        val groupedIds =
            overlapGroups
                .flatten()
                .map {
                    it.segment.id
                }
                .toSet()

        val isolated =
            remaining.filter {
                it.segment.id !in groupedIds
            }

        isolated.forEach { profile ->

            val best =
                findBestIdentity(
                    profile,
                    seeds,
                    assignments
                )

            assignments +=
                Assignment(
                    segmentId =
                        profile.segment.id,
                    seedIndex =
                        best.first,
                    score =
                        best.second
                )
        }

        overlapGroups.forEach { group ->

            Log.d(
                TAG,
                "Simultaneous appearance group: " +
                        group.map {
                            it.segment.id
                        }
            )

            val groupAssignments =
                assignOverlapGroup(
                    group = group,
                    seeds = seeds,
                    assignments = assignments
                )

            assignments +=
                groupAssignments
        }

        return assignments
    }

    private fun buildOverlapGroups(
        profiles: List<SegmentProfile>
    ): List<List<SegmentProfile>> {

        val unvisited =
            profiles
                .associateBy {
                    it.segment.id
                }
                .toMutableMap()

        val groups =
            mutableListOf<List<SegmentProfile>>()

        while (unvisited.isNotEmpty()) {

            val start =
                unvisited.values.first()

            val group =
                mutableListOf<SegmentProfile>()

            val queue =
                ArrayDeque<SegmentProfile>()

            queue.add(start)

            unvisited.remove(
                start.segment.id
            )

            while (queue.isNotEmpty()) {

                val current =
                    queue.removeFirst()

                group += current

                val connected =
                    unvisited.values
                        .filter {
                            segmentsOverlap(
                                current.segment,
                                it.segment
                            )
                        }
                        .toList()

                connected.forEach {
                    unvisited.remove(
                        it.segment.id
                    )

                    queue.add(it)
                }
            }

            groups +=
                group.sortedBy {
                    it.segment.startTimestampMs
                }
        }

        return groups
    }

    private fun assignOverlapGroup(
        group: List<SegmentProfile>,
        seeds: List<IdentitySeed>,
        assignments: List<Assignment>
    ): List<Assignment> {

        if (
            group.size >
            GROUP_EXACT_SEARCH_LIMIT
        ) {
            return greedyOverlapAssignment(
                group,
                seeds,
                assignments
            )
        }

        var bestScore =
            Float.NEGATIVE_INFINITY

        var bestAssignment =
            emptyList<Pair<Int, Float>>()

        fun search(
            index: Int,
            usedSeeds: Set<Int>,
            current: List<Pair<Int, Float>>,
            score: Float
        ) {

            if (
                index ==
                group.size
            ) {

                if (
                    score >
                    bestScore
                ) {

                    bestScore =
                        score

                    bestAssignment =
                        current
                }

                return
            }

            val profile =
                group[index]

            seeds.indices.forEach { seedIndex ->

                if (
                    seedIndex in
                    usedSeeds
                ) {
                    return@forEach
                }

                val identityScore =
                    scoreAgainstIdentity(
                        profile =
                            profile,
                        seedIndex =
                            seedIndex,
                        seeds =
                            seeds,
                        assignments =
                            assignments
                    )

                search(
                    index =
                        index + 1,
                    usedSeeds =
                        usedSeeds +
                                seedIndex,
                    current =
                        current +
                                (
                                        seedIndex to
                                                identityScore
                                        ),
                    score =
                        score +
                                identityScore
                )
            }
        }

        search(
            index = 0,
            usedSeeds = emptySet(),
            current = emptyList(),
            score = 0f
        )

        val result =
            mutableListOf<Assignment>()

        bestAssignment.forEachIndexed {
                index,
                pair ->

            result +=
                Assignment(
                    segmentId =
                        group[index]
                            .segment.id,
                    seedIndex =
                        pair.first,
                    score =
                        pair.second
                )
        }

        Log.d(
            TAG,
            "Joint simultaneous assignment: " +
                    result.joinToString {
                        "#${it.segmentId}->" +
                                "Person ${it.seedIndex + 1}"
                    }
        )

        return result
    }

    private fun greedyOverlapAssignment(
        group: List<SegmentProfile>,
        seeds: List<IdentitySeed>,
        assignments: List<Assignment>
    ): List<Assignment> {

        val usedSeeds =
            mutableSetOf<Int>()

        return group.map { profile ->

            val selected =
                seeds.indices
                    .filter {
                        it !in usedSeeds
                    }
                    .map { seedIndex ->

                        seedIndex to
                                scoreAgainstIdentity(
                                    profile,
                                    seedIndex,
                                    seeds,
                                    assignments
                                )
                    }
                    .maxByOrNull {
                        it.second
                    }
                    ?: (
                            0 to
                                    scoreAgainstIdentity(
                                        profile,
                                        0,
                                        seeds,
                                        assignments
                                    )
                            )

            usedSeeds +=
                selected.first

            Assignment(
                segmentId =
                    profile.segment.id,
                seedIndex =
                    selected.first,
                score =
                    selected.second
            )
        }
    }

    private fun findBestIdentity(
        profile: SegmentProfile,
        seeds: List<IdentitySeed>,
        assignments: List<Assignment>
    ): Pair<Int, Float> {

        return seeds.indices
            .map { seedIndex ->

                seedIndex to
                        scoreAgainstIdentity(
                            profile,
                            seedIndex,
                            seeds,
                            assignments
                        )
            }
            .maxByOrNull {
                it.second
            }
            ?: (0 to 0f)
    }

    private fun scoreAgainstIdentity(
        profile: SegmentProfile,
        seedIndex: Int,
        seeds: List<IdentitySeed>,
        assignments: List<Assignment>
    ): Float {

        val seed =
            seeds[seedIndex]

        if (segmentsOverlap(profile.segment, seed.profile.segment)) {
            return 0f
        }

        val historicalProfiles =
            assignments
                .filter {
                    it.seedIndex ==
                            seedIndex
                }
                .mapNotNull {
                    findSeedProfile(
                        it.segmentId,
                        seeds
                    )
                }

        if (historicalProfiles.any { segmentsOverlap(profile.segment, it.segment) }) {
            return 0f
        }

        val seedMatch =
            compareSegments(
                profile,
                seed.profile
            )

        val seedScore =
            seedMatch?.score ?: 0f

        val directFace =
            seedMatch?.faceScore ?: 0f

        if (directFace < 0.35f) {
            return 0f
        }

        val historicalScores =
            historicalProfiles
                .mapNotNull {
                    compareSegments(
                        profile,
                        it
                    )?.score
                }
                .sortedDescending()

        val consensus =
            if (
                historicalScores.isEmpty()
            ) {
                seedScore
            } else {
                historicalScores
                    .take(
                        min(
                            3,
                            historicalScores.size
                        )
                    )
                    .average()
                    .toFloat()
            }

        val temporal =
            temporalConsistency(
                profile,
                seedIndex,
                seeds,
                assignments
            )

        val directClothing =
            seedMatch?.clothingScore ?: 0f

        val base =
            0.56f * seedScore +
                    0.36f * consensus +
                    0.08f * temporal

        return when {

            directFace >=
                    STRONG_FACE_SCORE ->

                0.92f * base +
                        0.08f * directFace

            directClothing >=
                    0.90f &&
                    directFace >=
                    MIN_FACE_SCORE ->

                0.95f * base +
                        0.05f * directClothing

            else ->
                base
        }.coerceIn(
            0f,
            1f
        )
    }

    private fun findSeedProfile(
        segmentId: Int,
        seeds: List<IdentitySeed>
    ): SegmentProfile? {

        return seeds
            .firstOrNull {
                it.profile.segment.id ==
                        segmentId
            }
            ?.profile
    }

    private fun temporalConsistency(
        profile: SegmentProfile,
        seedIndex: Int,
        seeds: List<IdentitySeed>,
        assignments: List<Assignment>
    ): Float {

        val previous =
            assignments
                .filter {
                    it.seedIndex ==
                            seedIndex
                }
                .mapNotNull {
                    findSeedProfile(
                        it.segmentId,
                        seeds
                    )
                }
                .filter {
                    it.segment.startTimestampMs <
                            profile.segment.startTimestampMs
                }
                .maxByOrNull {
                    it.segment.startTimestampMs
                }
                ?: return 0f

        val gap =
            profile.segment.startTimestampMs -
                    previous.segment.endTimestampMs

        return when {

            gap < 0L ->
                0f

            gap <= 1500L ->
                0f

            gap <= 3500L ->
                0.35f

            gap <= 6500L ->
                0.75f

            gap <= 10000L ->
                1f

            gap <= 15000L ->
                0.80f

            else ->
                0.55f
        }
    }

    private fun compareSegments(
        first: SegmentProfile,
        second: SegmentProfile
    ): SegmentMatch? {

        val faceScores =
            mutableListOf<Float>()

        val clothingScores =
            mutableListOf<Float>()

        first.embeddings.forEach { firstEmbedding ->

            second.embeddings.forEach { secondEmbedding ->

                val similarity =
                    cosineSimilarity(
                        firstEmbedding.embedding,
                        secondEmbedding.embedding
                    )

                if (similarity >= 0f) {
                    faceScores +=
                        similarity
                }
            }
        }

        first.clothing.forEach { firstClothing ->

            second.clothing.forEach { secondClothing ->

                clothingScores +=
                    histogramSimilarity(
                        firstClothing.descriptor,
                        secondClothing.descriptor
                    )
            }
        }

        if (
            faceScores.isEmpty() &&
            clothingScores.isEmpty()
        ) {
            return null
        }

        val sortedFace =
            faceScores.sortedDescending()

        val sortedClothing =
            clothingScores.sortedDescending()

        val strongestFace =
            sortedFace.firstOrNull() ?: 0f

        val strongestClothing =
            sortedClothing.firstOrNull() ?: 0f

        val faceSupport =
            sortedFace
                .take(
                    min(
                        3,
                        sortedFace.size
                    )
                )
                .average()
                .toFloat()

        val clothingSupport =
            sortedClothing
                .take(
                    min(
                        3,
                        sortedClothing.size
                    )
                )
                .average()
                .toFloat()

        val faceScore =
            if (sortedFace.isEmpty()) {
                0f
            } else {
                0.65f * strongestFace +
                        0.35f * faceSupport
            }

        val clothingScore =
            if (
                sortedClothing.isEmpty()
            ) {
                0f
            } else {
                0.65f * strongestClothing +
                        0.35f * clothingSupport
            }

        val combinedScore =
            when {

                strongestFace < 0.35f ->
                    0f

                strongestFace >= 0.65f ->
                    0.85f * faceScore + 0.15f * clothingScore

                else ->
                    0.70f * faceScore + 0.30f * clothingScore
            }

        return SegmentMatch(
            firstId =
                first.segment.id,
            secondId =
                second.segment.id,
            score =
                combinedScore,
            faceScore =
                faceScore,
            clothingScore =
                clothingScore,
            strongestFace =
                strongestFace,
            strongestClothing =
                strongestClothing
        )
    }

    private fun segmentsOverlap(
        first: AppearanceSegment,
        second: AppearanceSegment
    ): Boolean {

        return first.startTimestampMs <=
                second.endTimestampMs &&
                second.startTimestampMs <=
                first.endTimestampMs
    }

    private fun histogramSimilarity(
        first: ClothingDescriptor,
        second: ClothingDescriptor
    ): Float {

        val size =
            min(
                first.values.size,
                second.values.size
            )

        var dot = 0f
        var firstMagnitude = 0f
        var secondMagnitude = 0f

        for (index in 0 until size) {

            val firstValue =
                first.values[index]

            val secondValue =
                second.values[index]

            dot +=
                firstValue *
                        secondValue

            firstMagnitude +=
                firstValue *
                        firstValue

            secondMagnitude +=
                secondValue *
                        secondValue
        }

        if (
            firstMagnitude <= 0f ||
            secondMagnitude <= 0f
        ) {
            return 0f
        }

        return (
                dot /
                        (
                                kotlin.math.sqrt(
                                    firstMagnitude
                                ) *
                                        kotlin.math.sqrt(
                                            secondMagnitude
                                        )
                                )
                ).coerceIn(
                0f,
                1f
            )
    }

    private fun buildIdentities(
        profiles: List<SegmentProfile>,
        seeds: List<IdentitySeed>,
        assignments: List<Assignment>
    ): List<PersonIdentity> {

        val initialIdentities = seeds.mapIndexed { seedIndex, seed ->

            val assignedProfiles =
                assignments
                    .filter {
                        it.seedIndex ==
                                seedIndex
                    }
                    .mapNotNull {
                        profiles.firstOrNull { profile ->
                            profile.segment.id ==
                                    it.segmentId
                        }
                    }
                    .distinctBy {
                        it.segment.id
                    }
                    .sortedBy {
                        it.segment.startTimestampMs
                    }

            val prototype =
                buildPrototype(
                    assignedProfiles
                )

            PersonIdentity(
                id =
                    seedIndex + 1,
                name =
                    "Person ${seedIndex + 1}",
                prototypeEmbedding =
                    prototype,
                appearances =
                    assignedProfiles
                        .map {
                            it.segment
                        }
                        .toMutableList()
            )
        }.filter { it.appearances.isNotEmpty() }

        return mergeSimilarIdentities(initialIdentities)
    }

    private fun mergeSimilarIdentities(
        identities: List<PersonIdentity>
    ): List<PersonIdentity> {
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
                if (sim >= 0.60f && !haveOverlappingAppearances(current, other)) {
                    current.appearances.addAll(other.appearances)
                    skip.add(other.id)
                }
            }

            merged.add(current)
        }

        return merged.mapIndexed { index, person ->
            val sortedAppearances = person.appearances
                .distinctBy { it.id }
                .sortedBy { it.startTimestampMs }
                .toMutableList()

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
                if (segmentsOverlap(segA, segB)) {
                    return true
                }
            }
        }
        return false
    }

    private fun buildPrototype(
        profiles: List<SegmentProfile>
    ): FloatArray {

        val embeddings =
            profiles.flatMap {
                it.embeddings
            }

        if (embeddings.isEmpty()) {
            return FloatArray(192)
        }

        val size =
            embeddings.first()
                .embedding
                .size

        val result =
            FloatArray(size)

        embeddings.forEach { item ->

            item.embedding
                .forEachIndexed {
                        index,
                        value ->

                    result[index] +=
                        value
                }
        }

        val count =
            embeddings.size.toFloat()

        result.forEachIndexed {
                index,
                value ->

            result[index] =
                value / count
        }

        return normalize(
            result
        )
    }

    private fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {

        val size =
            min(
                first.size,
                second.size
            )

        var dot = 0f
        var firstMagnitude = 0f
        var secondMagnitude = 0f

        for (index in 0 until size) {

            val firstValue =
                first[index]

            val secondValue =
                second[index]

            dot +=
                firstValue *
                        secondValue

            firstMagnitude +=
                firstValue *
                        firstValue

            secondMagnitude +=
                secondValue *
                        secondValue
        }

        if (
            firstMagnitude == 0f ||
            secondMagnitude == 0f
        ) {
            return -1f
        }

        return dot /
                (
                        kotlin.math.sqrt(
                            firstMagnitude
                        ) *
                                kotlin.math.sqrt(
                                    secondMagnitude
                                )
                        )
    }

    private fun normalize(
        embedding: FloatArray
    ): FloatArray {

        var magnitude = 0f

        embedding.forEach {
            magnitude +=
                it * it
        }

        magnitude =
            kotlin.math.sqrt(
                magnitude
            )

        if (magnitude == 0f) {
            return embedding
        }

        return FloatArray(
            embedding.size
        ) { index ->
            embedding[index] /
                    magnitude
        }
    }

    private fun FloatArray.isValidEmbedding(): Boolean {

        if (isEmpty()) {
            return false
        }

        return any {
            it.isFinite() &&
                    it != 0f
        }
    }
}