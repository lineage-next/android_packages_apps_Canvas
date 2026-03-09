/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResizeScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    initialCropRect: IntRect?,
    onConfirm: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }

    var cropRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(initialCropRect, imageInformation) {
        imageInformation?.let { imageInformation ->
            cropRect = initialCropRect?.let {
                imageInformation.originalBitmapRectToViewRect(it)
            }
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(innerPadding),
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .safeContentPadding(),
            ) { imageInformation = it }

            imageInformation?.let { imageInformation ->
                ResizeOverlay(
                    imageBounds = imageInformation.imageViewRect,
                    cropRect = cropRect ?: return@let,
                    onCropRectChange = { rect ->
                        cropRect = rect
                    },
                    onCropRectCommit = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SimpleActionBottomBar(
            onConfirm = {
                cropRect?.let { cropRect ->
                    imageInformation?.let { imageInformation ->
                        val originalImageCropRect = imageInformation.viewRectToOriginalBitmap(
                            cropRect
                        )

                        val action = Action.Resize(
                            rect = originalImageCropRect,
                        )

                        onConfirm(action)
                    }
                }
            },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun ResizeOverlay(
    imageBounds: Rect,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    onCropRectCommit: () -> Unit,
    modifier: Modifier = Modifier,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    var activeHandle by remember { mutableStateOf<Handle?>(null) }
    val currentCropRect by rememberUpdatedState(cropRect)

    val handleThresholdPx = with(LocalDensity.current) { 24.dp.toPx() }

    Canvas(
        modifier = modifier
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

                        val newRect = updateRectWithDrag(
                            currentCropRect,
                            activeHandle,
                            dragAmount,
                            imageBounds,
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

private fun updateRectWithDrag(rect: Rect, handle: Handle, drag: Offset, bounds: Rect): Rect {
    val minSize = 100f

    return when (handle) {
        Handle.CENTER -> {
            val width = rect.width
            val height = rect.height

            var newLeft = rect.left + drag.x
            var newTop = rect.top + drag.y

            newLeft = newLeft.coerceIn(bounds.left, bounds.right - width)
            newTop = newTop.coerceIn(bounds.top, bounds.bottom - height)

            rect.copy(
                left = newLeft,
                top = newTop,
                right = newLeft + width,
                bottom = newTop + height
            )
        }

        Handle.TOP -> rect.copy(
            top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize),
        )

        Handle.BOTTOM -> rect.copy(
            bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom),
        )

        Handle.LEFT -> rect.copy(
            left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize),
        )

        Handle.RIGHT -> rect.copy(
            right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right),
        )

        Handle.TOP_LEFT -> rect.copy(
            top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize),
            left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize),
        )

        Handle.TOP_RIGHT -> rect.copy(
            top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize),
            right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right),
        )

        Handle.BOTTOM_LEFT -> rect.copy(
            bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom),
            left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize),
        )

        Handle.BOTTOM_RIGHT -> rect.copy(
            bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom),
            right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right),
        )
    }
}

private fun getHandleForOffset(offset: Offset, rect: Rect, threshold: Float): Handle? {
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
        rect.contains(offset) -> Handle.CENTER

        else -> null
    }
}
