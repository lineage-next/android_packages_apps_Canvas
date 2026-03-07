/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Text style.
 *
 * @param fontFamily The font family
 * @param fontStyle The font style
 * @param textColor The text color
 * @param textSize The text size
 */
data class TextStyle(
    val fontFamily: FontFamily,
    val fontStyle: FontStyle,
    @ColorInt val textColor: Int,
    val textSize: Float,
) {
    /**
     * Font family.
     */
    enum class FontFamily {
        DEFAULT,
        SANS_SERIF,
        SERIF,
        MONOSPACE,
    }

    /**
     * Font style.
     */
    enum class FontStyle {
        NORMAL,
        BOLD,
        ITALIC,
        BOLD_ITALIC,
    }

    companion object {
        val DEFAULT = TextStyle(
            fontFamily = FontFamily.DEFAULT,
            fontStyle = FontStyle.NORMAL,
            textColor = Color.BLACK,
            textSize = 128f,
        )
    }
}
