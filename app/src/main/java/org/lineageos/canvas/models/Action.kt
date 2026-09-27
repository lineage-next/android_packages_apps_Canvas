/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect

/**
 * An action to apply to an image. Geometric values use coordinates in the image produced by all
 * preceding actions.
 */
sealed interface Action {
    /**
     * Adjustments applied to the image state at their position in the action history.
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
    sealed interface Transformation : Action {
        /**
         * Crop the current image to a rectangle in its current coordinates.
         */
        data class Crop(val rect: IntRect) : Transformation

        /**
         * Rotate the current image.
         */
        data class Rotation(val rotation: RotationStep) : Transformation
    }

    sealed interface Drawing : Action {
        /**
         * Draw a marker stroke on the current image.
         *
         * @param points The stroke points in current image coordinates
         * @param color The stroke color
         * @param strokeWidth The stroke width in image pixels
         */
        data class Marker(
            val points: List<IntOffset>,
            val color: Color = Color.Red,
            val strokeWidth: Float = 12f,
        ) : Drawing

        /**
         * Draw a translucent highlighter stroke on the current image.
         *
         * @param points The stroke points in current image coordinates
         * @param color The translucent stroke color
         * @param strokeWidth The stroke width in image pixels
         */
        data class Highlighter(
            val points: List<IntOffset>,
            val color: Color = Color.Yellow.copy(alpha = 0.4f),
            val strokeWidth: Float = 32f,
        ) : Drawing

        /**
         * Add a text label to the image.
         *
         * @param text The text of the overlay
         * @param position The position in the image produced by preceding actions
         * @param style The text style, with font size stored in image pixels
         */
        data class Text(
            val text: String,
            val position: IntOffset,
            val style: TextStyle,
        ) : Drawing

        /**
         * Clear a [Marker] drawing from the image.
         *
         * @param marker The marker to clear
         */
        data class Eraser(
            val marker: Marker,
        ) : Drawing
    }
}

/**
 * A supported image rotation.
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
