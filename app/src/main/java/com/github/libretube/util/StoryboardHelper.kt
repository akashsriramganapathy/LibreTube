package com.github.libretube.util

import android.graphics.Rect
import com.github.libretube.api.obj.PreviewFrames
import kotlin.math.floor

object StoryboardHelper {

    /**
     * Calculates the URL and crop coordinates for a specific frame at the given timestamp.
     *
     * @param previewFramesList The list of PreviewFrames available for the video.
     * @param timestampSeconds The timestamp in seconds for which to find the frame.
     * @return A pair containing the URL of the sprite sheet and the Rect(x, y, w, h) to crop, or null if not found.
     */
    fun getStoryboardUrlAndCrop(previewFramesList: List<PreviewFrames>?, timestampSeconds: Float): Pair<String, Rect>? {
        if (previewFramesList.isNullOrEmpty()) return null

        // Prefer a medium quality level if available (level 1), otherwise 0 (lowest) or last (highest)
        // Usually level 0 is enough for a tiny placeholder, but level 1 adds bit more detail.
        // Let's pick the one with closest to some reasonable frame width like 160 or 80.
        // Actually, PreviewFrames usually comes sorted. index 0 is typically lowest res.
        val sb = previewFramesList.getOrNull(1) ?: previewFramesList.first()

        if (sb.totalCount == 0 || sb.urls.isEmpty()) return null

        // Calculate total duration covered by this spec
        // NewPipe's PreviewFrames doesn't explicitly have totalDuration, but we can infer or use frame duration.
        // total duration = totalCount * durationPerFrame (in ms)
        val durationPerFrameSeconds = sb.durationPerFrame / 1000f
        if (durationPerFrameSeconds <= 0) return null
        
        // Calculate which absolute frame index corresponds to the timestamp
        // timestamp / durationPerFrame
        val absoluteFrameIndex = floor(timestampSeconds / durationPerFrameSeconds).toInt()
            .coerceIn(0, sb.totalCount - 1)

        // Calculate usage per sheet
        val framesPerSheet = sb.framesPerPageX * sb.framesPerPageY
        if (framesPerSheet <= 0) return null

        // Which sheet index?
        val sheetIndex = absoluteFrameIndex / framesPerSheet
        
        // Which URL?
        val url = sb.urls.getOrNull(sheetIndex) ?: return null

        // Position within the sheet
        val indexInSheet = absoluteFrameIndex % framesPerSheet
        
        // Col and Row (0-indexed)
        val col = indexInSheet % sb.framesPerPageX
        val row = indexInSheet / sb.framesPerPageX

        // Coordinates
        val x = col * sb.frameWidth
        val y = row * sb.frameHeight
        
        return url to Rect(x, y, x + sb.frameWidth, y + sb.frameHeight)
    }
}
