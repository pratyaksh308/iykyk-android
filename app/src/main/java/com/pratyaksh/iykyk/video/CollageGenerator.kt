package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import kotlin.math.max
import kotlin.math.min

class CollageGenerator {

    companion object {
        private const val OUTPUT_WIDTH = 1080
        private const val OUTPUT_HEIGHT = 1920
        private const val OUTER_MARGIN = 48f
        private const val TILE_GAP = 24f
        private const val CORNER_RADIUS = 36f
        private const val TILE_ASPECT_RATIO = 0.82f
        private const val FACE_PADDING_X = 1.9f
        private const val FACE_PADDING_Y = 2.2f
    }

    fun generate(
        identities: List<PersonIdentity>,
        representativeFrames: List<RepresentativeFrame>,
        representativeBitmaps: List<Bitmap>
    ): Bitmap? {
        if (identities.isEmpty()) {
            return null
        }

        val selected = identities.mapNotNull { identity ->
            val candidate = findBestRepresentative(
                identity = identity,
                representativeFrames = representativeFrames,
                representativeBitmaps = representativeBitmaps
            )

            if (candidate == null) {
                null
            } else {
                identity to candidate
            }
        }

        if (selected.isEmpty()) {
            return null
        }

        val output = Bitmap.createBitmap(
            OUTPUT_WIDTH,
            OUTPUT_HEIGHT,
            Bitmap.Config.ARGB_8888
        )

        output.applyCanvas {
            drawBackground(this)

            when (selected.size) {
                1 -> drawSingleTile(this, selected[0].second)
                2 -> drawTwoTiles(this, selected)
                3 -> drawThreeTiles(this, selected)
                4 -> drawFourTiles(this, selected)
                else -> drawGrid(this, selected)
            }
        }

        return output
    }

    private fun findBestRepresentative(
        identity: PersonIdentity,
        representativeFrames: List<RepresentativeFrame>,
        representativeBitmaps: List<Bitmap>
    ): RepresentativeCandidate? {
        val candidates = identity.appearances.mapNotNull { segment ->
            representativeFrames.indices.mapNotNull { index ->
                val frame = representativeFrames[index]
                val bitmap = representativeBitmaps.getOrNull(index)

                if (
                    bitmap != null &&
                    frame.timestampMs >= segment.startTimestampMs &&
                    frame.timestampMs <= segment.endTimestampMs
                ) {
                    RepresentativeCandidate(
                        frame = frame,
                        bitmap = bitmap
                    )
                } else {
                    null
                }
            }.maxByOrNull { it.frame.score }
        }

        return candidates.maxByOrNull { it.frame.score }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(android.graphics.Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f,
                0f,
                OUTPUT_WIDTH.toFloat(),
                OUTPUT_HEIGHT.toFloat(),
                android.graphics.Color.rgb(18, 18, 22),
                android.graphics.Color.rgb(5, 5, 7),
                android.graphics.Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(
            0f,
            0f,
            OUTPUT_WIDTH.toFloat(),
            OUTPUT_HEIGHT.toFloat(),
            paint
        )
    }

    private fun drawSingleTile(
        canvas: Canvas,
        candidate: RepresentativeCandidate
    ) {
        val rect = RectF(
            OUTER_MARGIN,
            180f,
            OUTPUT_WIDTH - OUTER_MARGIN,
            OUTPUT_HEIGHT - 180f
        )

        drawCandidate(
            canvas = canvas,
            candidate = candidate,
            destination = rect
        )
    }

    private fun drawTwoTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val tileHeight =
            (OUTPUT_HEIGHT - OUTER_MARGIN * 2f - TILE_GAP) / 2f

        val first = RectF(
            OUTER_MARGIN,
            OUTER_MARGIN,
            OUTPUT_WIDTH - OUTER_MARGIN,
            OUTER_MARGIN + tileHeight
        )

        val second = RectF(
            OUTER_MARGIN,
            OUTER_MARGIN + tileHeight + TILE_GAP,
            OUTPUT_WIDTH - OUTER_MARGIN,
            OUTPUT_HEIGHT - OUTER_MARGIN
        )

        drawCandidate(canvas, selected[0].second, first)
        drawCandidate(canvas, selected[1].second, second)
    }

    private fun drawThreeTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val tileWidth =
            (OUTPUT_WIDTH - OUTER_MARGIN * 2f - TILE_GAP) / 2f

        val topHeight = 900f

        val first = RectF(
            OUTER_MARGIN,
            90f,
            OUTPUT_WIDTH - OUTER_MARGIN,
            90f + topHeight
        )

        val second = RectF(
            OUTER_MARGIN,
            90f + topHeight + TILE_GAP,
            OUTER_MARGIN + tileWidth,
            OUTPUT_HEIGHT - 90f
        )

        val third = RectF(
            OUTER_MARGIN + tileWidth + TILE_GAP,
            90f + topHeight + TILE_GAP,
            OUTPUT_WIDTH - OUTER_MARGIN,
            OUTPUT_HEIGHT - 90f
        )

        drawCandidate(canvas, selected[0].second, first)
        drawCandidate(canvas, selected[1].second, second)
        drawCandidate(canvas, selected[2].second, third)
    }

    private fun drawFourTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        drawGrid(canvas, selected)
    }

    private fun drawGrid(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val columns = 2
        val rows = (selected.size + columns - 1) / columns

        val availableWidth =
            OUTPUT_WIDTH - OUTER_MARGIN * 2f - TILE_GAP

        val tileWidth = availableWidth / columns

        val availableHeight =
            OUTPUT_HEIGHT - OUTER_MARGIN * 2f -
                    TILE_GAP * (rows - 1)

        val tileHeight = availableHeight / rows

        selected.forEachIndexed { index, item ->
            val row = index / columns
            val column = index % columns

            val left =
                OUTER_MARGIN + column * (tileWidth + TILE_GAP)

            val top =
                OUTER_MARGIN + row * (tileHeight + TILE_GAP)

            val rect = RectF(
                left,
                top,
                left + tileWidth,
                top + tileHeight
            )

            drawCandidate(
                canvas = canvas,
                candidate = item.second,
                destination = rect
            )
        }
    }

    private fun drawCandidate(
        canvas: Canvas,
        candidate: RepresentativeCandidate,
        destination: RectF
    ) {
        val bitmap = candidate.bitmap

        val crop = calculateCrop(
            bitmap = bitmap,
            face = candidate.frame.face,
            destination = destination
        )

        val clipPath = android.graphics.Path().apply {
            addRoundRect(
                destination,
                CORNER_RADIUS,
                CORNER_RADIUS,
                android.graphics.Path.Direction.CW
            )
        }

        canvas.save()
        canvas.clipPath(clipPath)

        val source = Rect(
            crop.left,
            crop.top,
            crop.right,
            crop.bottom
        )

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
        )

        canvas.drawBitmap(
            bitmap,
            source,
            destination,
            paint
        )

        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = android.graphics.Color.argb(
                80,
                255,
                255,
                255
            )
        }

        canvas.drawRoundRect(
            destination,
            CORNER_RADIUS,
            CORNER_RADIUS,
            borderPaint
        )
    }

    private fun calculateCrop(
        bitmap: Bitmap,
        face: DetectedFace,
        destination: RectF
    ): Rect {
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        val destinationAspect =
            destination.width() / destination.height()

        var cropWidth = face.width * FACE_PADDING_X
        var cropHeight = face.height * FACE_PADDING_Y

        if (cropWidth / cropHeight > destinationAspect) {
            cropHeight = cropWidth / destinationAspect
        } else {
            cropWidth = cropHeight * destinationAspect
        }

        cropWidth = min(cropWidth, imageWidth)
        cropHeight = min(cropHeight, imageHeight)

        val centerX = face.centerX
        val centerY = face.centerY

        var left = centerX - cropWidth / 2f
        var top = centerY - cropHeight * 0.42f

        left = left.coerceIn(
            0f,
            max(0f, imageWidth - cropWidth)
        )

        top = top.coerceIn(
            0f,
            max(0f, imageHeight - cropHeight)
        )

        return Rect(
            left.toInt(),
            top.toInt(),
            (left + cropWidth).toInt(),
            (top + cropHeight).toInt()
        )
    }

    private data class RepresentativeCandidate(
        val frame: RepresentativeFrame,
        val bitmap: Bitmap
    )
}