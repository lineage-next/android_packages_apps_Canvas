/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

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
    }

    sealed interface Drawing : Action {
        /**
         * Draw a line on the image.
         */
        data object Marker : Drawing

        /**
         * Add a text label to the image.
         *
         * @param text The text of the overlay
         * @param position The position in the image produced by preceding actions
         * @param style The text style
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
