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
        private const val TILE_GAP = 20f
        private const val CORNER_RADIUS = 32f
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
                5 -> drawFiveTiles(this, selected)
                6 -> drawSixTiles(this, selected)
                7 -> drawSevenTiles(this, selected)
                8 -> drawEightTiles(this, selected)
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
                android.graphics.Color.rgb(22, 19, 26),
                android.graphics.Color.rgb(8, 7, 10),
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
            140f,
            OUTPUT_WIDTH - OUTER_MARGIN,
            1780f
        )
        drawCandidate(canvas, candidate, rect, 0)
    }

    private fun drawTwoTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val colWidth = (usableWidth - TILE_GAP) / 2f

        val first = RectF(
            OUTER_MARGIN,
            140f,
            OUTER_MARGIN + colWidth,
            1780f
        )

        val second = RectF(
            OUTER_MARGIN + colWidth + TILE_GAP,
            140f,
            OUTPUT_WIDTH - OUTER_MARGIN,
            1780f
        )

        drawCandidate(canvas, selected[0].second, first, 0)
        drawCandidate(canvas, selected[1].second, second, 1)
    }

    private fun drawThreeTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val topHeight = 980f

        val first = RectF(
            OUTER_MARGIN,
            140f,
            OUTPUT_WIDTH - OUTER_MARGIN,
            140f + topHeight
        )

        val colWidth = (usableWidth - TILE_GAP) / 2f
        val bottomTop = 140f + topHeight + TILE_GAP

        val second = RectF(
            OUTER_MARGIN,
            bottomTop,
            OUTER_MARGIN + colWidth,
            1780f
        )

        val third = RectF(
            OUTER_MARGIN + colWidth + TILE_GAP,
            bottomTop,
            OUTPUT_WIDTH - OUTER_MARGIN,
            1780f
        )

        drawCandidate(canvas, selected[0].second, first, 0)
        drawCandidate(canvas, selected[1].second, second, 1)
        drawCandidate(canvas, selected[2].second, third, 2)
    }

    private fun drawFourTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val tileWidth = (usableWidth - TILE_GAP) / 2f
        val tileHeight = (1640f - TILE_GAP) / 2f

        selected.forEachIndexed { index, item ->
            val row = index / 2
            val col = index % 2
            val left = OUTER_MARGIN + col * (tileWidth + TILE_GAP)
            val top = 140f + row * (tileHeight + TILE_GAP)
            val rect = RectF(left, top, left + tileWidth, top + tileHeight)
            drawCandidate(canvas, item.second, rect, index)
        }
    }

    private fun drawFiveTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val topWidth = (usableWidth - TILE_GAP) / 2f
        val topHeight = 960f

        val first = RectF(
            OUTER_MARGIN,
            140f,
            OUTER_MARGIN + topWidth,
            140f + topHeight
        )

        val second = RectF(
            OUTER_MARGIN + topWidth + TILE_GAP,
            140f,
            OUTPUT_WIDTH - OUTER_MARGIN,
            140f + topHeight
        )

        val bottomWidth = (usableWidth - TILE_GAP * 2f) / 3f
        val bottomTop = 140f + topHeight + TILE_GAP

        val third = RectF(
            OUTER_MARGIN,
            bottomTop,
            OUTER_MARGIN + bottomWidth,
            1780f
        )

        val fourth = RectF(
            OUTER_MARGIN + bottomWidth + TILE_GAP,
            bottomTop,
            OUTER_MARGIN + bottomWidth * 2f + TILE_GAP,
            1780f
        )

        val fifth = RectF(
            OUTER_MARGIN + bottomWidth * 2f + TILE_GAP * 2f,
            bottomTop,
            OUTPUT_WIDTH - OUTER_MARGIN,
            1780f
        )

        drawCandidate(canvas, selected[0].second, first, 0)
        drawCandidate(canvas, selected[1].second, second, 1)
        drawCandidate(canvas, selected[2].second, third, 2)
        drawCandidate(canvas, selected[3].second, fourth, 3)
        drawCandidate(canvas, selected[4].second, fifth, 4)
    }

    private fun drawSixTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val colWidth = (usableWidth - TILE_GAP * 2f) / 3f
        val rowHeight = (1640f - TILE_GAP) / 2f

        selected.forEachIndexed { index, item ->
            val row = index / 3
            val col = index % 3
            val left = OUTER_MARGIN + col * (colWidth + TILE_GAP)
            val top = 140f + row * (rowHeight + TILE_GAP)
            val rect = RectF(left, top, left + colWidth, top + rowHeight)
            drawCandidate(canvas, item.second, rect, index)
        }
    }

    private fun drawSevenTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val topWidth = (usableWidth - TILE_GAP * 2f) / 3f
        val topHeight = 800f

        for (i in 0 until 3) {
            val left = OUTER_MARGIN + i * (topWidth + TILE_GAP)
            val rect = RectF(left, 140f, left + topWidth, 140f + topHeight)
            drawCandidate(canvas, selected[i].second, rect, i)
        }

        val bottomWidth = (usableWidth - TILE_GAP * 3f) / 4f
        val bottomTop = 140f + topHeight + TILE_GAP

        for (i in 3 until 7) {
            val col = i - 3
            val left = OUTER_MARGIN + col * (bottomWidth + TILE_GAP)
            val rect = RectF(left, bottomTop, left + bottomWidth, 1780f)
            drawCandidate(canvas, selected[i].second, rect, i)
        }
    }

    private fun drawEightTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val colWidth = (usableWidth - TILE_GAP * 3f) / 4f
        val rowHeight = (1640f - TILE_GAP) / 2f

        selected.forEachIndexed { index, item ->
            val row = index / 4
            val col = index % 4
            val left = OUTER_MARGIN + col * (colWidth + TILE_GAP)
            val top = 140f + row * (rowHeight + TILE_GAP)
            val rect = RectF(left, top, left + colWidth, top + rowHeight)
            drawCandidate(canvas, item.second, rect, index)
        }
    }

    private fun drawGrid(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val columns = 3
        val rows = (selected.size + columns - 1) / columns
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val tileWidth = (usableWidth - TILE_GAP * (columns - 1)) / columns
        val tileHeight = (1640f - TILE_GAP * (rows - 1)) / rows

        selected.forEachIndexed { index, item ->
            val row = index / columns
            val column = index % columns
            val left = OUTER_MARGIN + column * (tileWidth + TILE_GAP)
            val top = 140f + row * (tileHeight + TILE_GAP)
            val rect = RectF(left, top, left + tileWidth, top + tileHeight)
            drawCandidate(canvas, item.second, rect, index)
        }
    }

    private fun drawCandidate(
        canvas: Canvas,
        candidate: RepresentativeCandidate,
        destination: RectF,
        index: Int
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
            strokeWidth = 2.5f
            color = android.graphics.Color.argb(
                60,
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

        if (index < 3) {
            drawBadge(canvas, destination, index)
        }
    }

    private fun drawBadge(canvas: Canvas, destination: RectF, index: Int) {
        val radius = 24f
        val cx = destination.left + 36f + radius
        val cy = destination.top + 36f + radius

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(220, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 26f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }
        val symbol = when (index % 3) {
            0 -> "☀"
            1 -> "⚡"
            else -> "♥"
        }
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(symbol, cx, cy - yOffset, textPaint)
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

        var cropWidth = face.width * 1.9f
        var cropHeight = face.height * 2.2f

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
