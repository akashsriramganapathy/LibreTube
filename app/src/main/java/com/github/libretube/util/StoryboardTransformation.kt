package com.github.libretube.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import coil3.size.Size
import coil3.transform.Transformation

class StoryboardTransformation(private val cropRect: Rect) : Transformation() {

    override val cacheKey: String = "StoryboardTransformation-${cropRect.left}-${cropRect.top}-${cropRect.width()}-${cropRect.height()}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = cropRect.width()
        val height = cropRect.height()

        val output = Bitmap.createBitmap(width, height, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val srcRect = cropRect
        val dstRect = Rect(0, 0, width, height)

        canvas.drawBitmap(input, srcRect, dstRect, paint)

        return output
    }
}
