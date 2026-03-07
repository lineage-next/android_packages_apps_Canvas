/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.PointF
import android.graphics.RectF

/**
 * An action to apply to an image.
 */
sealed interface Action {
    /**
     * Resize the image to the given rectangle.
     *
     * @param rect The rectangle to resize to
     */
    data class Resize(val rect: RectF) : Action

    /**
     * Draw a line on the image.
     */
    data object Marker : Action

    /**
     * Add a text label to the image.
     *
     * @param text The text of the overlay
     * @param position The position relative to the image
     * @param textStyle The text style
     */
    data class Text(
        val text: String,
        val position: PointF,
        val textStyle: TextStyle,
    ) : Action

    /**
     * Clear any [Marker] drawing from the image.
     */
    data object Eraser : Action
}
