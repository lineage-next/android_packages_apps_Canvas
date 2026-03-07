/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

/**
 * [Action.Resize] handles.
 *
 * @see [Action.Resize]
 */
enum class Handle {
    /**
     * Top left corner.
     */
    TOP_LEFT,

    /**
     * Top center corner.
     */
    TOP,

    /**
     * Top right corner.
     */
    TOP_RIGHT,

    /**
     * Left center corner.
     */
    LEFT,

    /**
     * Center of the selection.
     */
    CENTER,

    /**
     * Right center corner.
     */
    RIGHT,

    /**
     * Bottom left corner.
     */
    BOTTOM_LEFT,

    /**
     * Bottom center corner.
     */
    BOTTOM,

    /**
     * Bottom right corner.
     */
    BOTTOM_RIGHT,
}
