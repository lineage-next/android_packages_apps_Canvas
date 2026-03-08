/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import androidx.compose.ui.graphics.Color

/**
 * Text style.
 *
 * @param fontFamily The font family
 * @param size The text size
 * @param color The text color
 * @param alignment The text alignment
 * @param bold Whether the text is bold
 * @param italic Whether the text is italic
 * @param underlined Whether the text is underlined
 * @param strikethrough Whether the text is strikethrough
 */
data class TextStyle(
    val fontFamily: FontFamily,
    val size: Float,
    val color: Color,
    val alignment: Alignment = Alignment.LEFT,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underlined: Boolean = false,
    val strikethrough: Boolean = false,
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

    enum class Alignment {
        LEFT,
        CENTER,
        RIGHT,
    }

    companion object {
        val DEFAULT = TextStyle(
            fontFamily = FontFamily.DEFAULT,
            size = 128f,
            color = Color.White,
        )
    }
}
