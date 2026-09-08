package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.applyCanvas
import kotlin.math.max
import kotlin.math.min

class CollageGenerator {

    companion object {
        private const val OUTPUT_WIDTH = 1080
        private const val OUTPUT_HEIGHT = 1920
        private const val OUTER_MARGIN = 48f
        private const val TOP_OFFSET = 260f
        private const val BOTTOM_OFFSET = 1750f
        private const val TILE_GAP = 20f
        private const val CORNER_RADIUS = 32f
        private const val CONTAINER_RADIUS = 36f
    }

    private data class BadgeStyle(
        val label: String,
        val bgStart: Int,
        val bgEnd: Int,
        val textColor: Int
    )

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
            drawHeader(this, selected.size)
            drawCardContainer(this)

            when (selected.size) {
                1 -> drawSingleTile(this, selected[0])
                2 -> drawTwoTiles(this, selected)
                3 -> drawThreeTiles(this, selected)
                4 -> drawFourTiles(this, selected)
                5 -> drawFiveTiles(this, selected)
                6 -> drawSixTiles(this, selected)
                7 -> drawSevenTiles(this, selected)
                8 -> drawEightTiles(this, selected)
                else -> drawGrid(this, selected)
            }

            drawFooter(this)
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
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f,
                OUTPUT_WIDTH.toFloat(), OUTPUT_HEIGHT.toFloat(),
                Color.rgb(19, 15, 28),
                Color.rgb(11, 8, 18),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, OUTPUT_WIDTH.toFloat(), OUTPUT_HEIGHT.toFloat(), bgPaint)

        // Top-left ambient coral glow
        val coralGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                120f, 120f, 650f,
                Color.argb(50, 255, 75, 85),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(120f, 120f, 650f, coralGlow)

        // Top-right electric teal glow
        val tealGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                960f, 160f, 550f,
                Color.argb(35, 6, 182, 212),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(960f, 160f, 550f, tealGlow)

        // Bottom-right ambient purple glow
        val purpleGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                960f, 1800f, 700f,
                Color.argb(45, 139, 92, 246),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(960f, 1800f, 700f, purpleGlow)
    }

    private fun drawHeader(canvas: Canvas, peopleCount: Int) {
        // Pill Badge: iykyk • squad ✨
        val pillRect = RectF(OUTER_MARGIN, 56f, OUTER_MARGIN + 280f, 110f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                pillRect.left, pillRect.top, pillRect.right, pillRect.bottom,
                Color.rgb(255, 75, 85), Color.rgb(255, 107, 139),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(pillRect, 27f, 27f, pillPaint)

        val brandTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        canvas.drawText("iykyk", pillRect.left + 24f, pillRect.top + 36f, brandTextPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(pillRect.left + 115f, pillRect.top + 27f, 4f, dotPaint)

        val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.WHITE
        }
        canvas.drawText("squad ✨", pillRect.left + 132f, pillRect.top + 35f, tagTextPaint)

        // Right Pill: curated
        val curatedRect = RectF(OUTPUT_WIDTH - OUTER_MARGIN - 175f, 60f, OUTPUT_WIDTH - OUTER_MARGIN, 106f)
        val curatedBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 34, 28, 46)
        }
        canvas.drawRoundRect(curatedRect, 23f, 23f, curatedBg)

        val curatedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(175, 168, 186)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦ curated", curatedRect.centerX(), curatedRect.top + 30f, curatedTextPaint)

        // Main Poster Title: the crew.
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        canvas.drawText("the crew.", OUTER_MARGIN, 180f, titlePaint)

        // Subtitle: one best shot of all N people.
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            color = Color.rgb(175, 168, 186)
        }
        val subtitleText = if (peopleCount == 1) {
            "one best shot of 1 person in your video."
        } else {
            "one best shot of all $peopleCount people in your video."
        }
        canvas.drawText(subtitleText, OUTER_MARGIN, 220f, subtitlePaint)
    }

    private fun drawCardContainer(canvas: Canvas) {
        val containerRect = RectF(
            OUTER_MARGIN - 12f,
            TOP_OFFSET - 12f,
            OUTPUT_WIDTH - OUTER_MARGIN + 12f,
            BOTTOM_OFFSET + 12f
        )

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 18, 31)
        }
        canvas.drawRoundRect(containerRect, CONTAINER_RADIUS, CONTAINER_RADIUS, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(containerRect, CONTAINER_RADIUS, CONTAINER_RADIUS, borderPaint)
    }

    private fun drawFooter(canvas: Canvas) {
        val footerY = 1825f

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.rgb(175, 168, 186)
        }
        canvas.drawText("made with iykyk • on-device AI", OUTER_MARGIN, footerY, footerTextPaint)

        // Four colored accent dots on right
        val dotRadius = 8f
        val rightX = OUTPUT_WIDTH - OUTER_MARGIN

        val purplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(139, 92, 246) }
        val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 209, 102) }
        val coralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 75, 85) }
        val tealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(6, 182, 212) }

        canvas.drawCircle(rightX - 66f, footerY - 8f, dotRadius, tealPaint)
        canvas.drawCircle(rightX - 44f, footerY - 8f, dotRadius, purplePaint)
        canvas.drawCircle(rightX - 22f, footerY - 8f, dotRadius, yellowPaint)
        canvas.drawCircle(rightX, footerY - 8f, dotRadius, coralPaint)
    }

    private fun drawSingleTile(
        canvas: Canvas,
        item: Pair<PersonIdentity, RepresentativeCandidate>
    ) {
        val rect = RectF(
            OUTER_MARGIN,
            TOP_OFFSET,
            OUTPUT_WIDTH - OUTER_MARGIN,
            BOTTOM_OFFSET
        )
        drawCandidate(canvas, item.second, rect, 0, item.first.name)
    }

    private fun drawTwoTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val colWidth = (usableWidth - TILE_GAP) / 2f

        val first = RectF(
            OUTER_MARGIN,
            TOP_OFFSET,
            OUTER_MARGIN + colWidth,
            BOTTOM_OFFSET
        )

        val second = RectF(
            OUTER_MARGIN + colWidth + TILE_GAP,
            TOP_OFFSET,
            OUTPUT_WIDTH - OUTER_MARGIN,
            BOTTOM_OFFSET
        )

        drawCandidate(canvas, selected[0].second, first, 0, selected[0].first.name)
        drawCandidate(canvas, selected[1].second, second, 1, selected[1].first.name)
    }

    private fun drawThreeTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val topHeight = 900f

        val first = RectF(
            OUTER_MARGIN,
            TOP_OFFSET,
            OUTPUT_WIDTH - OUTER_MARGIN,
            TOP_OFFSET + topHeight
        )

        val colWidth = (usableWidth - TILE_GAP) / 2f
        val bottomTop = TOP_OFFSET + topHeight + TILE_GAP

        val second = RectF(
            OUTER_MARGIN,
            bottomTop,
            OUTER_MARGIN + colWidth,
            BOTTOM_OFFSET
        )

        val third = RectF(
            OUTER_MARGIN + colWidth + TILE_GAP,
            bottomTop,
            OUTPUT_WIDTH - OUTER_MARGIN,
            BOTTOM_OFFSET
        )

        drawCandidate(canvas, selected[0].second, first, 0, selected[0].first.name)
        drawCandidate(canvas, selected[1].second, second, 1, selected[1].first.name)
        drawCandidate(canvas, selected[2].second, third, 2, selected[2].first.name)
    }

    private fun drawFourTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val tileWidth = (usableWidth - TILE_GAP) / 2f
        val usableHeight = BOTTOM_OFFSET - TOP_OFFSET
        val tileHeight = (usableHeight - TILE_GAP) / 2f

        selected.forEachIndexed { index, item ->
            val row = index / 2
            val col = index % 2
            val left = OUTER_MARGIN + col * (tileWidth + TILE_GAP)
            val top = TOP_OFFSET + row * (tileHeight + TILE_GAP)
            val rect = RectF(left, top, left + tileWidth, top + tileHeight)
            drawCandidate(canvas, item.second, rect, index, item.first.name)
        }
    }

    private fun drawFiveTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val topWidth = (usableWidth - TILE_GAP) / 2f
        val topHeight = 880f

        val first = RectF(
            OUTER_MARGIN,
            TOP_OFFSET,
            OUTER_MARGIN + topWidth,
            TOP_OFFSET + topHeight
        )

        val second = RectF(
            OUTER_MARGIN + topWidth + TILE_GAP,
            TOP_OFFSET,
            OUTPUT_WIDTH - OUTER_MARGIN,
            TOP_OFFSET + topHeight
        )

        val bottomWidth = (usableWidth - TILE_GAP * 2f) / 3f
        val bottomTop = TOP_OFFSET + topHeight + TILE_GAP

        val third = RectF(
            OUTER_MARGIN,
            bottomTop,
            OUTER_MARGIN + bottomWidth,
            BOTTOM_OFFSET
        )

        val fourth = RectF(
            OUTER_MARGIN + bottomWidth + TILE_GAP,
            bottomTop,
            OUTER_MARGIN + bottomWidth * 2f + TILE_GAP,
            BOTTOM_OFFSET
        )

        val fifth = RectF(
            OUTER_MARGIN + bottomWidth * 2f + TILE_GAP * 2f,
            bottomTop,
            OUTPUT_WIDTH - OUTER_MARGIN,
            BOTTOM_OFFSET
        )

        drawCandidate(canvas, selected[0].second, first, 0, selected[0].first.name)
        drawCandidate(canvas, selected[1].second, second, 1, selected[1].first.name)
        drawCandidate(canvas, selected[2].second, third, 2, selected[2].first.name)
        drawCandidate(canvas, selected[3].second, fourth, 3, selected[3].first.name)
        drawCandidate(canvas, selected[4].second, fifth, 4, selected[4].first.name)
    }

    private fun drawSixTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val colWidth = (usableWidth - TILE_GAP * 2f) / 3f
        val usableHeight = BOTTOM_OFFSET - TOP_OFFSET
        val rowHeight = (usableHeight - TILE_GAP) / 2f

        selected.forEachIndexed { index, item ->
            val row = index / 3
            val col = index % 3
            val left = OUTER_MARGIN + col * (colWidth + TILE_GAP)
            val top = TOP_OFFSET + row * (rowHeight + TILE_GAP)
            val rect = RectF(left, top, left + colWidth, top + rowHeight)
            drawCandidate(canvas, item.second, rect, index, item.first.name)
        }
    }

    private fun drawSevenTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val topWidth = (usableWidth - TILE_GAP * 2f) / 3f
        val topHeight = 740f

        for (i in 0 until 3) {
            val left = OUTER_MARGIN + i * (topWidth + TILE_GAP)
            val rect = RectF(left, TOP_OFFSET, left + topWidth, TOP_OFFSET + topHeight)
            drawCandidate(canvas, selected[i].second, rect, i, selected[i].first.name)
        }

        val bottomWidth = (usableWidth - TILE_GAP * 3f) / 4f
        val bottomTop = TOP_OFFSET + topHeight + TILE_GAP

        for (i in 3 until 7) {
            val col = i - 3
            val left = OUTER_MARGIN + col * (bottomWidth + TILE_GAP)
            val rect = RectF(left, bottomTop, left + bottomWidth, BOTTOM_OFFSET)
            drawCandidate(canvas, selected[i].second, rect, i, selected[i].first.name)
        }
    }

    private fun drawEightTiles(
        canvas: Canvas,
        selected: List<Pair<PersonIdentity, RepresentativeCandidate>>
    ) {
        val usableWidth = OUTPUT_WIDTH - OUTER_MARGIN * 2f
        val colWidth = (usableWidth - TILE_GAP * 3f) / 4f
        val usableHeight = BOTTOM_OFFSET - TOP_OFFSET
        val rowHeight = (usableHeight - TILE_GAP) / 2f

        selected.forEachIndexed { index, item ->
            val row = index / 4
            val col = index % 4
            val left = OUTER_MARGIN + col * (colWidth + TILE_GAP)
            val top = TOP_OFFSET + row * (rowHeight + TILE_GAP)
            val rect = RectF(left, top, left + colWidth, top + rowHeight)
            drawCandidate(canvas, item.second, rect, index, item.first.name)
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
        val usableHeight = BOTTOM_OFFSET - TOP_OFFSET
        val tileHeight = (usableHeight - TILE_GAP * (rows - 1)) / rows

        selected.forEachIndexed { index, item ->
            val row = index / columns
            val column = index % columns
            val left = OUTER_MARGIN + column * (tileWidth + TILE_GAP)
            val top = TOP_OFFSET + row * (tileHeight + TILE_GAP)
            val rect = RectF(left, top, left + tileWidth, top + tileHeight)
            drawCandidate(canvas, item.second, rect, index, item.first.name)
        }
    }

    private fun drawCandidate(
        canvas: Canvas,
        candidate: RepresentativeCandidate,
        destination: RectF,
        index: Int,
        personName: String
    ) {
        val bitmap = candidate.bitmap

        val crop = calculateCrop(
            bitmap = bitmap,
            face = candidate.frame.face,
            destination = destination
        )

        val clipPath = Path().apply {
            addRoundRect(
                destination,
                CORNER_RADIUS,
                CORNER_RADIUS,
                Path.Direction.CW
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
            color = Color.argb(70, 255, 255, 255)
        }

        canvas.drawRoundRect(
            destination,
            CORNER_RADIUS,
            CORNER_RADIUS,
            borderPaint
        )

        drawStickerBadge(canvas, destination, index)
        drawNameStamp(canvas, destination, personName)
    }

    private fun drawStickerBadge(canvas: Canvas, destination: RectF, index: Int) {
        val badgeStyle = when (index % 6) {
            0 -> BadgeStyle("⭐ STAR SHOT", Color.rgb(255, 209, 102), Color.rgb(255, 180, 50), Color.rgb(30, 20, 0))
            1 -> BadgeStyle("⚡ THE VIBE", Color.rgb(255, 75, 85), Color.rgb(230, 45, 65), Color.WHITE)
            2 -> BadgeStyle("💖 MAIN CHAR", Color.rgb(255, 107, 139), Color.rgb(235, 75, 110), Color.WHITE)
            3 -> BadgeStyle("🔥 ICONIC", Color.rgb(255, 138, 61), Color.rgb(230, 105, 30), Color.WHITE)
            4 -> BadgeStyle("✨ CHILL", Color.rgb(139, 92, 246), Color.rgb(115, 68, 220), Color.WHITE)
            else -> BadgeStyle("👑 GOAT", Color.rgb(6, 182, 212), Color.rgb(2, 150, 185), Color.WHITE)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(badgeStyle.label)
        val pillWidth = textWidth + 28f
        val pillHeight = 36f

        val left = destination.left + 16f
        val top = destination.top + 16f
        val pillRect = RectF(left, top, left + pillWidth, top + pillHeight)

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
        }
        canvas.drawRoundRect(RectF(left + 2f, top + 3f, left + pillWidth + 2f, top + pillHeight + 3f), 18f, 18f, shadowPaint)

        // Badge pill gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(pillRect.left, pillRect.top, pillRect.right, pillRect.bottom, badgeStyle.bgStart, badgeStyle.bgEnd, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(pillRect, 18f, 18f, bgPaint)

        // Text
        textPaint.color = badgeStyle.textColor
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(badgeStyle.label, pillRect.left + 14f, pillRect.centerY() - yOffset, textPaint)
    }

    private fun drawNameStamp(canvas: Canvas, destination: RectF, name: String) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val textWidth = textPaint.measureText(name)
        val pillWidth = textWidth + 24f
        val pillHeight = 32f

        val left = destination.left + 16f
        val top = destination.bottom - 16f - pillHeight
        val pillRect = RectF(left, top, left + pillWidth, top + pillHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 18, 14, 28)
        }
        canvas.drawRoundRect(pillRect, 16f, 16f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(50, 255, 255, 255)
        }
        canvas.drawRoundRect(pillRect, 16f, 16f, borderPaint)

        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(name, pillRect.left + 12f, pillRect.centerY() - yOffset, textPaint)
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
