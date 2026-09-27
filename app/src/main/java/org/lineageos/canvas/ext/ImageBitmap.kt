/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.lineageos.canvas.models.RotationStep

val ImageBitmap.size: IntSize
    get() = IntSize(width, height)

/**
 * Rotate this bitmap clockwise using a supported rotation step.
 */
fun ImageBitmap.rotateBy(rotationStep: RotationStep): ImageBitmap {
    val degrees = rotationStep.degrees
    if (degrees == 0f) return this

    val isSwapped = degrees % 180f != 0f
    val outputWidth = if (isSwapped) height else width
    val outputHeight = if (isSwapped) width else height
    val result = ImageBitmap(
        width = outputWidth,
        height = outputHeight,
        config = config,
        hasAlpha = hasAlpha,
        colorSpace = colorSpace,
    )

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(result),
        size = Size(outputWidth.toFloat(), outputHeight.toFloat()),
    ) {
        rotate(
            degrees = degrees,
            pivot = Offset(outputWidth / 2f, outputHeight / 2f),
        ) {
            drawImage(
                image = this@rotateBy,
                topLeft = Offset(
                    x = (outputWidth - width) / 2f,
                    y = (outputHeight - height) / 2f,
                ),
            )
        }
    }

    return result
}
