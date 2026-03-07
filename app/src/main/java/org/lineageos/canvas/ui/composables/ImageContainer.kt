/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import org.lineageos.canvas.ext.drawCropOverlay
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.models.Mode
import org.lineageos.canvas.ui.theme.CropOverlayStyle
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle
import kotlin.math.abs
import kotlin.math.min

@Composable
fun ImageContainer(
    modifier: Modifier = Modifier,
    uri: Uri,
    mode: Mode?,
    cropRect: RectF?,
    actionsBitmap: Bitmap?,
    onBaseRectChange: (RectF) -> Unit,
    onCropRectChange: (RectF) -> Unit,
    onCropRectCommit: () -> Unit,
    onImageClick: (PointF) -> Unit = {},
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    val imageBounds = remember(containerSize, imageSize) {
        if (containerSize == Size.Zero || imageSize == Size.Zero) return@remember null

        val scale =
            min(containerSize.width / imageSize.width, containerSize.height / imageSize.height)

        val w = imageSize.width * scale
        val h = imageSize.height * scale
        val dx = (containerSize.width - w) / 2
        val dy = (containerSize.height - h) / 2

        RectF(dx, dy, dx + w, dy + h)
    }

    LaunchedEffect(imageBounds) {
        imageBounds?.let { imageBounds ->
            onBaseRectChange(imageBounds)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                containerSize = it.size.toSize()
            }
            .pointerInput(imageBounds) {
                detectTapGestures { offset ->
                    imageBounds?.let { bounds ->
                        if (bounds.contains(offset.x, offset.y)) {
                            onImageClick(
                                PointF(
                                    offset.x - bounds.left, offset.y - bounds.top
                                )
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = {
                imageSize = it.painter.intrinsicSize
            })

        imageBounds?.let { imageBounds ->
            actionsBitmap?.let {
                Image(
                    modifier = Modifier.size(
                        width = with(LocalDensity.current) { it.width.toDp() },
                        height = with(LocalDensity.current) { it.height.toDp() },
                    ),
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                )
            }

            cropRect?.let { cropRect ->
                if (mode == Mode.RESIZE) {
                    ResizeOverlay(
                        imageBounds = imageBounds,
                        cropRect = cropRect,
                        onCropRectChange = onCropRectChange,
                        onCropRectCommit = onCropRectCommit,
                    )
                } else if (cropRect != imageBounds) {
                    CropOverlay(cropRect = cropRect)
                }
            }
        }
    }
}

@Composable
private fun ResizeOverlay(
    imageBounds: RectF,
    cropRect: RectF,
    onCropRectChange: (RectF) -> Unit,
    onCropRectCommit: () -> Unit,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    var activeHandle by remember { mutableStateOf<Handle?>(null) }
    val currentCropRect by rememberUpdatedState(cropRect)

    val handleThresholdPx = with(LocalDensity.current) { 24.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(imageBounds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle =
                            getHandleForOffset(offset, currentCropRect, handleThresholdPx)
                    },
                    onDragEnd = {
                        activeHandle = null
                        onCropRectCommit()
                    },
                    onDragCancel = {
                        activeHandle = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val activeHandle = activeHandle ?: return@detectDragGestures

                        val newRect = RectF(currentCropRect)
                        updateRectWithDrag(newRect, activeHandle, dragAmount, imageBounds)
                        onCropRectChange(newRect)
                    },
                )
            },
    ) {
        drawCropOverlay(currentCropRect, showHandles = true, style)
    }
}

@Composable
private fun CropOverlay(
    cropRect: RectF,
    modifier: Modifier = Modifier,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawCropOverlay(cropRect, showHandles = false, style)
    }
}

private fun updateRectWithDrag(rect: RectF, handle: Handle, drag: Offset, bounds: RectF) {
    val minSize = 100f
    when (handle) {
        Handle.CENTER -> {
            val width = rect.width()
            val height = rect.height()

            var newLeft = rect.left + drag.x
            var newTop = rect.top + drag.y

            newLeft = newLeft.coerceIn(bounds.left, bounds.right - width)
            newTop = newTop.coerceIn(bounds.top, bounds.bottom - height)

            rect.set(newLeft, newTop, newLeft + width, newTop + height)
        }

        Handle.TOP -> rect.top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize)
        Handle.BOTTOM -> rect.bottom =
            (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom)

        Handle.LEFT -> rect.left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize)
        Handle.RIGHT -> rect.right =
            (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right)

        Handle.TOP_LEFT -> {
            rect.top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize)
            rect.left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize)
        }

        Handle.TOP_RIGHT -> {
            rect.top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize)
            rect.right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right)
        }

        Handle.BOTTOM_LEFT -> {
            rect.bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom)
            rect.left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize)
        }

        Handle.BOTTOM_RIGHT -> {
            rect.bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom)
            rect.right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right)
        }
    }
}

private fun getHandleForOffset(offset: Offset, rect: RectF, threshold: Float): Handle? {
    val x = offset.x
    val y = offset.y

    // Corner check
    val isTop = abs(y - rect.top) < threshold
    val isBottom = abs(y - rect.bottom) < threshold
    val isLeft = abs(x - rect.left) < threshold
    val isRight = abs(x - rect.right) < threshold

    return when {
        isTop && isLeft -> Handle.TOP_LEFT
        isTop && isRight -> Handle.TOP_RIGHT
        isBottom && isLeft -> Handle.BOTTOM_LEFT
        isBottom && isRight -> Handle.BOTTOM_RIGHT

        // Edge check
        isTop && x in (rect.left..rect.right) -> Handle.TOP
        isBottom && x in (rect.left..rect.right) -> Handle.BOTTOM
        isLeft && y in (rect.top..rect.bottom) -> Handle.LEFT
        isRight && y in (rect.top..rect.bottom) -> Handle.RIGHT

        // Rect check
        rect.contains(x, y) -> Handle.CENTER

        else -> null
    }
}
