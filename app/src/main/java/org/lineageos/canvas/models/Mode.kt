/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

/**
 * Edit mode.
 */
enum class Mode {
    /**
     * Image resize.
     */
    RESIZE,

    /**
     * Add text overlay.
     */
    TEXT,

    /**
     * Free drawing on the image.
     */
    MARKER,

    /**
     * Erase [MARKER] drawings.
     */
    ERASER,
}
