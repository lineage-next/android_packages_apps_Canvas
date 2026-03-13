/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection

/**
 * An action to apply to an image.
 */
sealed interface Action {
    /**
     * Adjustments applied to the whole source image, not to the [Drawing] actions
     */
    sealed interface Adjustment : Action {
        /**
         * Adjust the brightness of the image.
         *
         * @param value The brightness value
         */
        data class Brightness(val value: Float) : Adjustment

        /**
         * Adjust the contrast of the image.
         *
         * @param value The contrast value
         */
        data class Contrast(val value: Float) : Adjustment
    }

    /**
     * Transformations that will edit the dimensions of the image.
     */
    sealed interface Transformation : Action, Transformable, Processable {
        /**
         * Resize the image to the given rectangle.
         *
         * @param rect The rectangle to resize to, from the top-left corner
         */
        data class Resize(val rect: IntRect) : Transformation {
            override fun DrawScope.applyTransform(block: DrawScope.() -> Unit) {
                withTransform({
                    clipRect(
                        left = rect.left.toFloat(),
                        top = rect.top.toFloat(),
                        right = rect.right.toFloat(),
                        bottom = rect.bottom.toFloat(),
                    )
                }, block)
            }

            override fun process(bitmap: Bitmap): Bitmap = Bitmap.createBitmap(
                bitmap,
                rect.left,
                rect.top,
                rect.width,
                rect.height,
            )
        }

        /**
         * Rotate the image by the given degrees.
         *
         * @param rotation The rotation to apply
         */
        data class Rotation(val rotation: RotationStep) : Transformation {
            override fun DrawScope.applyTransform(block: DrawScope.() -> Unit) {
                withTransform({
                    rotate(
                        degrees = rotation.degrees,
                        pivot = Offset(size.width / 2f, size.height / 2f)
                    )
                    translate(
                        left = (size.width - size.height) / 2f,
                        top = (size.height - size.width) / 2f,
                    )
                }, block)
            }

            override fun process(bitmap: Bitmap): Bitmap {
                val matrix = Matrix().apply {
                    postRotate(rotation.degrees)
                }
                return Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true,
                )
            }
        }
    }

    sealed interface Drawing : Action, Drawable {
        /**
         * Draw a line on the image.
         *
         * @param points The points of the line
         * @param strokeWidth The width of the line
         * @param color The color of the line
         */
        data class Marker(
            val points: List<Offset>,
            val strokeWidth: Float,
            val color: Color,
        ) : Drawing {
            override fun DrawScope.draw(context: DrawingContext) = drawStroke(BlendMode.SrcOver)

            fun DrawScope.drawStroke(blendMode: BlendMode) {
                drawContext.canvas.saveLayer(size.toRect(), Paint())
                for (i in 1 until points.size) {
                    drawLine(
                        color = color,
                        start = points[i - 1],
                        end = points[i],
                        strokeWidth = strokeWidth,
                        blendMode = blendMode,
                    )
                }
                drawContext.canvas.restore()
            }
        }

        /**
         * Add a text label to the image.
         *
         * @param text The text of the overlay
         * @param position The position relative to the image
         * @param style The text style
         */
        data class Text(
            val text: String,
            val position: IntOffset,
            val style: TextStyle,
        ) : Drawing {
            override fun DrawScope.draw(context: DrawingContext) {
                val textMeasurer = TextMeasurer(
                    defaultFontFamilyResolver = context.fontFamilyResolver,
                    defaultDensity = this,
                    defaultLayoutDirection = context.layoutDirection,
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(position.x.toFloat(), position.y.toFloat()),
                    style = style,
                )
            }
        }

        /**
         * Clear a [Marker] drawing from the image.
         *
         * @param marker The marker to clear
         */
        data class Eraser(
            val marker: Marker,
        ) : Drawing {
            override fun DrawScope.draw(context: DrawingContext) {
                with(marker) { drawStroke(BlendMode.Clear) }
            }
        }
    }
}

data class DrawingContext(
    val fontFamilyResolver: FontFamily.Resolver,
    val layoutDirection: LayoutDirection,
)

fun interface Drawable {
    fun DrawScope.draw(context: DrawingContext)
}

fun interface Transformable {
    fun DrawScope.applyTransform(block: DrawScope.() -> Unit)
}

fun interface Processable {
    fun process(bitmap: Bitmap): Bitmap
}

fun List<Action.Transformation>.process(bitmap: Bitmap) = fold(bitmap) { acc, t -> t.process(acc) }

fun DrawScope.applyActionsAndDraw(
    sourceBitmap: ImageBitmap,
    actions: List<Action>,
    context: DrawingContext,
) {
    var block: DrawScope.() -> Unit = {}

    for (action in actions.asReversed()) {
        val innerBlock = block
        block = when (action) {
            is Action.Transformation -> {
                { with(action) { applyTransform { innerBlock() } } }
            }

            is Action.Drawing -> {
                {
                    innerBlock()
                    with(action) { draw(context) }
                }
            }

            else -> {
                innerBlock
            }
        }
    }

    drawImage(sourceBitmap)
    block()
}

/**
 * Image rotation.
 */
enum class RotationStep(val degrees: Float) {
    ROT_0(0f),
    ROT_90(90f),
    ROT_180(180f),
    ROT_270(270f);

    fun clockwise() = when (this) {
        ROT_0 -> ROT_90
        ROT_90 -> ROT_180
        ROT_180 -> ROT_270
        ROT_270 -> ROT_0
    }

    fun counterClockwise() = when (this) {
        ROT_0 -> ROT_270
        ROT_90 -> ROT_0
        ROT_180 -> ROT_90
        ROT_270 -> ROT_180
    }
}
