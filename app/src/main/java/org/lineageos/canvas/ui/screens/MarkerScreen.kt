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
import org.lineageos.canvas.ui.composables.StrokeColorPalette

private const val MARKER_STROKE_WIDTH = 24f
private const val HIGHLIGHTER_STROKE_WIDTH = 32f

/**
 * Add marker strokes to the current image.
 */
@Composable
fun MarkerScreen(
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) = StrokeScreen(
    imageBitmap = imageBitmap,
    initialColor = Color.Red,
    strokeWidth = MARKER_STROKE_WIDTH,
    createAction = { points, color ->
        Action.Drawing.Marker(
            points = points,
            color = color,
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
    imageBitmap: ImageBitmap,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) = StrokeScreen(
    imageBitmap = imageBitmap,
    initialColor = Color.Yellow,
    colorTransform = { it.copy(alpha = 0.4f) },
    strokeWidth = HIGHLIGHTER_STROKE_WIDTH,
    createAction = { points, color ->
        Action.Drawing.Highlighter(
            points = points,
            color = color,
            strokeWidth = HIGHLIGHTER_STROKE_WIDTH,
        )
    },
    onAddAction = onAddAction,
    onCancel = onCancel,
)

@Composable
private fun StrokeScreen(
    imageBitmap: ImageBitmap,
    initialColor: Color,
    colorTransform: (Color) -> Color = { it },
    strokeWidth: Float,
    createAction: (List<IntOffset>, Color) -> Action,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }
    var isDrawing by remember { mutableStateOf(false) }
    var previewPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var imagePoints by remember { mutableStateOf(emptyList<IntOffset>()) }
    var color by remember { mutableStateOf(initialColor) }
    var activeStrokeColor by remember { mutableStateOf(initialColor) }
    var pendingActions by remember { mutableStateOf(emptyList<Action>()) }
    var completedPreviewStrokes by remember {
        mutableStateOf(emptyList<Pair<List<Offset>, Color>>())
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
                    .fillMaxSize(),
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
                                    activeStrokeColor = colorTransform(color)
                                }
                            },
                            onDragEnd = {
                                if (isDrawing && imagePoints.isNotEmpty()) {
                                    pendingActions = pendingActions + createAction(
                                        imagePoints.toList(),
                                        activeStrokeColor,
                                    )
                                    completedPreviewStrokes = completedPreviewStrokes +
                                        listOf(previewPoints to activeStrokeColor)
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

                fun drawStroke(points: List<Offset>, strokeColor: Color) {
                    if (points.size == 1) {
                        drawCircle(
                            color = strokeColor,
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
                            color = strokeColor,
                            style = Stroke(
                                width = previewStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                completedPreviewStrokes.forEach { (points, strokeColor) ->
                    drawStroke(points, strokeColor)
                }
                drawStroke(previewPoints, activeStrokeColor)
            }
        }

        StrokeColorPalette(
            selectedColor = color,
            onColorSelected = { color = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
        )

        SimpleActionBottomBar(
            onConfirm = {
                pendingActions.forEach(onAddAction)
                onCancel()
            },
            onCancel = onCancel,
        )
    }
}
