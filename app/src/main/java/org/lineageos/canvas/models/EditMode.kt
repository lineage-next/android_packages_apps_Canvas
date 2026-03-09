/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

/**
 * UI edit mode.
 */
enum class EditMode(val category: Category) {
    /**
     * Image resize.
     */
    RESIZE(Category.TRANSFORMATION),

    /**
     * Image rotation.
     */
    ROTATION(Category.TRANSFORMATION),

    /**
     * Free drawing on the image.
     */
    MARKER(Category.DRAWING),

    /**
     * Highlight drawing on the image.
     */
    HIGHLIGHTER(Category.DRAWING),

    /**
     * Add text overlay.
     */
    TEXT(Category.DRAWING),

    /**
     * Brightness adjustment.
     */
    BRIGHTNESS(Category.ADJUSTMENTS),

    /**
     * Contrast adjustment.
     */
    CONTRAST(Category.ADJUSTMENTS);

    companion object {
        private val groupedByCategory = entries.groupBy(EditMode::category)

        fun ofCategory(category: Category) = groupedByCategory[category].orEmpty()
    }

    /**
     * Edit mode categories.
     */
    enum class Category {
        /**
         * Transformation of the image.
         */
        TRANSFORMATION,

        /**
         * Adjustments applied to the image.
         */
        ADJUSTMENTS,

        /**
         * Drawing on the image.
         */
        DRAWING,
    }
}
