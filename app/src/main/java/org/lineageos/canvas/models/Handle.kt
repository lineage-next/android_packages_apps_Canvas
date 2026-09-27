/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

/**
 * Crop handles.
 *
 * @see [Action.Transformation.Crop]
 */
enum class Handle(
    val movesLeft: Boolean = false,
    val movesTop: Boolean = false,
    val movesRight: Boolean = false,
    val movesBottom: Boolean = false,
) {
    /**
     * Top left corner.
     */
    TOP_LEFT(movesLeft = true, movesTop = true),

    /**
     * Top center corner.
     */
    TOP(movesTop = true),

    /**
     * Top right corner.
     */
    TOP_RIGHT(movesTop = true, movesRight = true),

    /**
     * Left center corner.
     */
    LEFT(movesLeft = true),

    /**
     * Center of the selection.
     */
    CENTER,

    /**
     * Right center corner.
     */
    RIGHT(movesRight = true),

    /**
     * Bottom left corner.
     */
    BOTTOM_LEFT(movesLeft = true, movesBottom = true),

    /**
     * Bottom center corner.
     */
    BOTTOM(movesBottom = true),

    /**
     * Bottom right corner.
     */
    BOTTOM_RIGHT(movesRight = true, movesBottom = true),
}
