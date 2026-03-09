/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lineageos.canvas.ui.theme.CropOverlayStyle

private fun handleOffsets(rect: Rect) = listOf(
    Offset(rect.left, rect.top),
    Offset(rect.right, rect.top),
    Offset(rect.left, rect.bottom),
    Offset(rect.right, rect.bottom),
    Offset(rect.center.x, rect.top),
    Offset(rect.center.x, rect.bottom),
    Offset(rect.left, rect.center.y),
    Offset(rect.right, rect.center.y),
)

fun DrawScope.drawCropOverlay(rect: Rect, showHandles: Boolean, style: CropOverlayStyle) {
    drawRect(color = style.dimColor)

    drawRect(
        color = Color.Transparent,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        blendMode = BlendMode.Clear,
    )

    drawRect(
        color = if (showHandles) style.handleColor else style.borderColor,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        style = Stroke(width = style.strokeWidth.value),
    )

    if (showHandles) {
        handleOffsets(rect).forEach { offset ->
            drawCircle(style.handleColor, style.handleRadius.value, offset)
        }
    }
}
