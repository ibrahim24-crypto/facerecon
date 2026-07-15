package com.himo.facerecon

import android.graphics.Rect
import androidx.compose.ui.geometry.Rect as ComposeRect
import kotlin.math.min

object FaceCoordinateMapper {

    fun mapBounds(bounds: Rect, analysisWidth: Int, mirrorHorizontally: Boolean): Rect {
        if (!mirrorHorizontally) return bounds
        return Rect(
            analysisWidth - bounds.right,
            bounds.top,
            analysisWidth - bounds.left,
            bounds.bottom
        )
    }

    fun toScreenRect(
        bounds: Rect,
        analysisWidth: Int,
        analysisHeight: Int,
        screenWidthPx: Float,
        screenHeightPx: Float,
        mirrorHorizontally: Boolean = true
    ): ComposeRect {
        val analysisW = analysisWidth.coerceAtLeast(1).toFloat()
        val analysisH = analysisHeight.coerceAtLeast(1).toFloat()
        val scale = min(screenWidthPx / analysisW, screenHeightPx / analysisH)
        val offsetX = (screenWidthPx - analysisW * scale) / 2f
        val offsetY = (screenHeightPx - analysisH * scale) / 2f

        val mapped = mapBounds(bounds, analysisWidth, mirrorHorizontally)
        val left = mapped.left * scale + offsetX
        val top = mapped.top * scale + offsetY
        val right = mapped.right * scale + offsetX
        val bottom = mapped.bottom * scale + offsetY
        return ComposeRect(left, top, right, bottom)
    }
}
