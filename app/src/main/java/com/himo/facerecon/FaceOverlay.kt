package com.himo.facerecon

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val UnknownColor = Color(0xFFFF1744)
private val KnownColor = Color(0xFF00E676)
private val RegisterColor = Color(0xFFFF9100)

@Composable
fun FaceOverlay(
    faces: List<FaceData>,
    analysisWidth: Int,
    analysisHeight: Int,
    mirrorHorizontally: Boolean = true,
    onUnknownFaceTap: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isRegisterMode: Boolean = false,
    showFaceIndex: Boolean = false
) {
    val hasRecognizedFace = faces.any { it.name.isNotEmpty() && it.name != "Unknown" }
    val infiniteTransition = rememberInfiniteTransition(label = "facePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasRecognizedFace) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val labelBackground = Color.Black.copy(alpha = 0.55f)
    val cornerRadius = 12.dp
    val baseStroke = if (faces.size <= 2) 5f else if (faces.size <= 5) 3f else 2f

    var revealedFaceId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revealedFaceId) {
        if (revealedFaceId != null) {
            delay(2000)
            revealedFaceId = null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(faces) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var isDrag = false

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            if (!isDrag) {
                                change.consume()
                                val tap = downPos
                                for (face in faces) {
                                    val isUnknown = face.name == "Unknown" || face.name.isEmpty()
                                    val rect = FaceCoordinateMapper.toScreenRect(
                                        bounds = face.bounds,
                                        analysisWidth = analysisWidth,
                                        analysisHeight = analysisHeight,
                                        screenWidthPx = size.width.toFloat(),
                                        screenHeightPx = size.height.toFloat(),
                                        mirrorHorizontally = mirrorHorizontally
                                    )
                                    if (tap.x in rect.left..rect.right && tap.y in rect.top..rect.bottom) {
                                        if (isUnknown) {
                                            onUnknownFaceTap(face.id)
                                        } else {
                                            revealedFaceId = face.id
                                        }
                                        break
                                    }
                                }
                            }
                            break
                        }
                        val dragDist = change.position - downPos
                        if (dragDist.getDistance() > viewConfiguration.touchSlop) {
                            isDrag = true
                        }
                    } while (true)
                }
            }
    ) {
        val screenWidthPx = size.width
        val screenHeightPx = size.height
        val cornerPx = cornerRadius.toPx()

        for (face in faces) {
            val isUnknown = face.name == "Unknown" || face.name.isEmpty()
            val color = if (isRegisterMode) RegisterColor else if (isUnknown) UnknownColor else KnownColor

            val screenRect = FaceCoordinateMapper.toScreenRect(
                bounds = face.bounds,
                analysisWidth = analysisWidth,
                analysisHeight = analysisHeight,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                mirrorHorizontally = mirrorHorizontally
            )
            val width = screenRect.width * pulseScale
            val height = screenRect.height * pulseScale
            val centerX = screenRect.center.x
            val centerY = screenRect.center.y
            val drawLeft = centerX - width / 2f
            val drawTop = centerY - height / 2f

            val minDim = minOf(width, height)
            val scaledCorner = minOf(cornerPx, minDim / 4f)
            val scaledStroke = if (minDim < 80f) (baseStroke * minDim / 80f).coerceAtLeast(1f) else baseStroke
            val finalStroke = scaledStroke * pulseScale

            drawRoundRect(
                color = color,
                topLeft = Offset(drawLeft, drawTop),
                size = Size(width, height),
                cornerRadius = CornerRadius(scaledCorner, scaledCorner),
                style = Stroke(width = finalStroke)
            )

            if (isRegisterMode || isUnknown) {
                drawRoundRect(
                    color = color.copy(alpha = 0.18f),
                    topLeft = Offset(drawLeft, drawTop),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(scaledCorner, scaledCorner)
                )
            }

            if (!isUnknown && face.id == revealedFaceId) {
                val paint = Paint().also { p ->
                    p.color = color.toArgb()
                    p.textSize = 36f
                    p.isAntiAlias = true
                }
                val textBounds = Rect()
                paint.getTextBounds(face.name, 0, face.name.length, textBounds)
                val textWidth = paint.measureText(face.name)
                val padH = 16f
                val padV = 10f
                val bgWidth = textWidth + padH * 2
                val bgHeight = textBounds.height() + padV * 2
                val textX = drawLeft + padH
                val bgTop = (drawTop - bgHeight - 4f).coerceAtLeast(0f)

                drawRoundRect(
                    color = labelBackground,
                    topLeft = Offset(drawLeft, bgTop),
                    size = Size(bgWidth, bgHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                val baseline = bgTop + padV + paint.fontMetrics.run { descent - top } * 0.75f
                drawContext.canvas.nativeCanvas.drawText(face.name, textX, baseline, paint)

                // Confidence score below name
                if (face.confidence > 0f) {
                    val scorePaint = Paint().also { p ->
                        p.color = color.copy(alpha = 0.7f).toArgb()
                        p.textSize = 24f
                        p.isAntiAlias = true
                    }
                    val scoreText = String.format("%.1f", face.confidence)
                    val scoreBaseline = baseline + paint.fontMetrics.run { descent - top } * 0.6f
                    drawContext.canvas.nativeCanvas.drawText(scoreText, textX, scoreBaseline, scorePaint)
                }
            }

            // small index badge when requested (developer/UX toggle)
            if (showFaceIndex) {
                val idxPaint = Paint().also { p ->
                    p.color = Color.Black.toArgb()
                    p.textSize = 28f
                    p.isAntiAlias = true
                }
                val badgeSize = 36f
                val badgeLeft = drawLeft
                val badgeTop = drawTop - badgeSize - 4f
                // draw background
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.6f),
                    topLeft = Offset(badgeLeft, badgeTop.coerceAtLeast(0f)),
                    size = Size(badgeSize, badgeSize),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                val indexText = face.id.takeLast(2) // short id hint
                val tx = badgeLeft + 8f
                val ty = (badgeTop.coerceAtLeast(0f)) + badgeSize - 10f
                drawContext.canvas.nativeCanvas.drawText(indexText, tx, ty, idxPaint)
            }
        }
    }
}
