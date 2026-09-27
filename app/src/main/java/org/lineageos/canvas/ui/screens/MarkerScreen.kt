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
private const val HIGHLIGHTER_STROKE_WIDTH = 32f

/**
 * Add marker strokes to the current image.
 */
@Composable
fun MarkerScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) = StrokeScreen(
    innerPadding = innerPadding,
    imageBitmap = imageBitmap,
    color = Color.Red,
    strokeWidth = MARKER_STROKE_WIDTH,
    createAction = { points ->
        Action.Drawing.Marker(
            points = points,
            strokeWidth = MARKER_STROKE_WIDTH,
        )
    },
    onAddAction = onAddAction,
    onCancel = onCancel,
)

/**
 * Add highlighter strokes to the current image.
 */
@Composable
fun HighlighterScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) = StrokeScreen(
    innerPadding = innerPadding,
    imageBitmap = imageBitmap,
    color = Color.Yellow.copy(alpha = 0.4f),
    strokeWidth = HIGHLIGHTER_STROKE_WIDTH,
    createAction = { points ->
        Action.Drawing.Highlighter(
            points = points,
            strokeWidth = HIGHLIGHTER_STROKE_WIDTH,
        )
    },
    onAddAction = onAddAction,
    onCancel = onCancel,
)

@Composable
private fun StrokeScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    color: Color,
    strokeWidth: Float,
    createAction: (List<IntOffset>) -> Action,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var isDrawing by remember { mutableStateOf(false) }
    var previewPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var imagePoints by remember { mutableStateOf(emptyList<IntOffset>()) }
    var pendingActions by remember { mutableStateOf(emptyList<Action>()) }
    var completedPreviewStrokes by remember { mutableStateOf(emptyList<List<Offset>>()) }

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
                                    pendingActions = pendingActions + createAction(imagePoints.toList())
                                    completedPreviewStrokes = completedPreviewStrokes +
                                        listOf(previewPoints)
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
                    strokeWidth *
                        info.imageViewRect.width / info.bitmapSize.width
                } ?: 8.dp.toPx()

                fun drawStroke(points: List<Offset>) {
                    if (points.size == 1) {
                        drawCircle(
                            color = color,
                            radius = previewStrokeWidth / 2f,
                            center = points.first(),
                        )
                    } else if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = previewStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                completedPreviewStrokes.forEach(::drawStroke)
                drawStroke(previewPoints)
            }
        }

        SimpleActionBottomBar(
            onConfirm = {
                pendingActions.forEach(onAddAction)
                onCancel()
            },
            onCancel = onCancel,
        )
    }
}
