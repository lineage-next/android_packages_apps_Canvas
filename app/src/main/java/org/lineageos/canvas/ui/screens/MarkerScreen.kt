/*
 * SPDX-FileCopyrightText: The LineageOS Project
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.SimpleActionBottomBar

private const val MARKER_STROKE_WIDTH = 12f

/**
 * Add marker strokes to the current image.
 */
@Composable
fun MarkerScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var isDrawing by remember { mutableStateOf(false) }
    var previewPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var imagePoints by remember { mutableStateOf(emptyList<IntOffset>()) }

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
                    .safeContentPadding(),
            ) { imageInformation = it }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageInformation) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val info = imageInformation
                                isDrawing = info != null && info.imageViewRect.contains(offset)
                                if (isDrawing) {
                                    previewPoints = listOf(offset)
                                    imagePoints = listOf(info!!.viewOffsetToOriginalBitmap(offset))
                                }
                            },
                            onDragEnd = {
                                if (isDrawing && imagePoints.isNotEmpty()) {
                                    onAddAction(
                                        Action.Drawing.Marker(
                                            points = imagePoints.toList(),
                                            strokeWidth = MARKER_STROKE_WIDTH,
                                        )
                                    )
                                }
                                isDrawing = false
                                previewPoints = emptyList()
                                imagePoints = emptyList()
                            },
                            onDragCancel = {
                                isDrawing = false
                                previewPoints = emptyList()
                                imagePoints = emptyList()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (!isDrawing) return@detectDragGestures

                                val info = imageInformation ?: return@detectDragGestures
                                previewPoints = previewPoints + change.position
                                imagePoints = imagePoints + info.viewOffsetToOriginalBitmap(
                                    change.position,
                                )
                            },
                        )
                    },
            ) {
                val previewStrokeWidth = imageInformation?.let { info ->
                    MARKER_STROKE_WIDTH *
                        info.imageViewRect.width / info.bitmapSize.width
                } ?: 8.dp.toPx()

                if (previewPoints.size == 1) {
                    drawCircle(
                        color = Color.Red,
                        radius = previewStrokeWidth / 2f,
                        center = previewPoints.first(),
                    )
                } else if (previewPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(previewPoints.first().x, previewPoints.first().y)
                        previewPoints.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Red,
                        style = Stroke(
                            width = previewStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        SimpleActionBottomBar(
            onConfirm = null,
            onCancel = onCancel,
        )
    }
}
