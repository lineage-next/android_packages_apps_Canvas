/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

fun brightnessColorFilter(value: Float): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, value * 255f,
            0f, 1f, 0f, 0f, value * 255f,
            0f, 0f, 1f, 0f, value * 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

fun ImageBitmap.adjustBrightness(value: Float): ImageBitmap {
    if (value == 0f) return this

    val output = ImageBitmap(
        width = width,
        height = height,
        config = config,
        hasAlpha = hasAlpha,
        colorSpace = colorSpace,
    )

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(output),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawImage(
            image = this@adjustBrightness,
            colorFilter = brightnessColorFilter(value),
        )
    }

    return output
}
