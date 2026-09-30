/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.ext.drawCropOverlay
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.SimpleActionBottomBar
import org.lineageos.canvas.ui.theme.CropOverlayStyle
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle

@Composable
fun CropScreen(
    imageBitmap: ImageBitmap,
    initialCropRect: IntRect?,
    onConfirm: (Action?) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    val imageBounds = IntRect(0, 0, imageBitmap.width, imageBitmap.height)
    var cropRect by remember(imageBitmap) {
        mutableStateOf(initialCropRect ?: imageBounds)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) { imageInformation = it }

            imageInformation?.let { imageInformation ->
                CropOverlay(
                    imageBounds = imageInformation.imageViewRect,
                    cropRect = imageInformation.originalBitmapRectToViewRect(cropRect),
                    onCropRectChange = { rect ->
                        cropRect = imageInformation.viewRectToOriginalBitmap(rect)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SimpleActionBottomBar(
            onConfirm = {
                if (imageInformation != null) {
                    onConfirm(cropRect.takeIf { it != imageBounds }?.let {
                        Action.Transformation.Crop(it)
                    })
                }
            },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun CropOverlay(
    imageBounds: Rect,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    var activeHandle by remember { mutableStateOf<Handle?>(null) }
    val currentCropRect by rememberUpdatedState(cropRect)

    val handleHitRadiusPx = with(LocalDensity.current) {
        maxOf(style.handleRadius + 8.dp, 24.dp).toPx()
    }
    val minimumCropSizePx = with(LocalDensity.current) { 100.dp.toPx() }

    Canvas(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(imageBounds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = getHandleForOffset(
                            offset = offset,
                            rect = currentCropRect,
                            hitRadius = handleHitRadiusPx,
                        )
                    },
                    onDragEnd = {
                        activeHandle = null
                    },
                    onDragCancel = {
                        activeHandle = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val activeHandle = activeHandle ?: return@detectDragGestures

                        if ((change.position.y > imageBounds.bottom && dragAmount.y < 0f)
                            || (change.position.y < imageBounds.top && dragAmount.y > 0f)
                            || (change.position.x > imageBounds.right && dragAmount.x < 0f)
                            || (change.position.x < imageBounds.left && dragAmount.x > 0f)
                        ) {
                            return@detectDragGestures
                        }

                        val newRect = updateRectWithDrag(
                            currentCropRect,
                            activeHandle,
                            dragAmount,
                            imageBounds,
                            minimumCropSizePx,
                        )
                        onCropRectChange(newRect)
                    },
                )
            },
    ) {
        drawCropOverlay(
            rect = currentCropRect,
            showHandles = true,
            style = style,
        )
    }
}

private fun updateRectWithDrag(
    rect: Rect,
    handle: Handle,
    drag: Offset,
    bounds: Rect,
    requestedMinimumSize: Float,
): Rect {
    val minWidth = minOf(requestedMinimumSize, bounds.width)
    val minHeight = minOf(requestedMinimumSize, bounds.height)

    if (handle == Handle.CENTER) {
        val width = rect.width.coerceIn(0f, bounds.width)
        val height = rect.height.coerceIn(0f, bounds.height)
        val maxLeft = maxOf(bounds.left, minOf(bounds.right, bounds.right - width))
        val maxTop = maxOf(bounds.top, minOf(bounds.bottom, bounds.bottom - height))
        val left = (rect.left + drag.x).coerceIn(bounds.left, maxLeft)
        val top = (rect.top + drag.y).coerceIn(bounds.top, maxTop)

        return Rect(
            left = left,
            top = top,
            right = minOf(bounds.right, left + width),
            bottom = minOf(bounds.bottom, top + height),
        )
    }

    val left = if (handle.movesLeft) {
        (rect.left + drag.x).coerceIn(
            bounds.left,
            maxOf(bounds.left, minOf(bounds.right, rect.right - minWidth)),
        )
    } else {
        rect.left
    }
    val right = if (handle.movesRight) {
        (rect.right + drag.x).coerceIn(
            minOf(bounds.right, maxOf(bounds.left, rect.left + minWidth)),
            bounds.right,
        )
    } else {
        rect.right
    }
    val top = if (handle.movesTop) {
        (rect.top + drag.y).coerceIn(
            bounds.top,
            maxOf(bounds.top, minOf(bounds.bottom, rect.bottom - minHeight)),
        )
    } else {
        rect.top
    }
    val bottom = if (handle.movesBottom) {
        (rect.bottom + drag.y).coerceIn(
            minOf(bounds.bottom, maxOf(bounds.top, rect.top + minHeight)),
            bounds.bottom,
        )
    } else {
        rect.bottom
    }

    return Rect(left, top, right, bottom)
}

private fun getHandleForOffset(offset: Offset, rect: Rect, hitRadius: Float): Handle? {
    val handles = listOf(
        Handle.TOP_LEFT to Offset(rect.left, rect.top),
        Handle.TOP to Offset(rect.center.x, rect.top),
        Handle.TOP_RIGHT to Offset(rect.right, rect.top),
        Handle.LEFT to Offset(rect.left, rect.center.y),
        Handle.RIGHT to Offset(rect.right, rect.center.y),
        Handle.BOTTOM_LEFT to Offset(rect.left, rect.bottom),
        Handle.BOTTOM to Offset(rect.center.x, rect.bottom),
        Handle.BOTTOM_RIGHT to Offset(rect.right, rect.bottom),
    )
    val hitRadiusSquared = hitRadius * hitRadius
    val closestHandle = handles.minByOrNull { (_, position) ->
        val dx = offset.x - position.x
        val dy = offset.y - position.y
        dx * dx + dy * dy
    }

    if (closestHandle != null) {
        val (handle, position) = closestHandle
        val dx = offset.x - position.x
        val dy = offset.y - position.y
        if (dx * dx + dy * dy <= hitRadiusSquared) return handle
    }

    return Handle.CENTER.takeIf { rect.contains(offset) }
}
