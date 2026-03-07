/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.Color
import android.graphics.PointF
import android.graphics.Typeface

/**
 * Predefined text style.
 *
 * @param fontStyle The font style
 * @param color The text color
 * @param size The text size
 */
enum class TextStyle(
    val fontStyle: Int,
    val color: Int,
    val size: Float,
) {
    BLACK(
        fontStyle = Typeface.NORMAL,
        color = Color.BLACK,
        size = 128f,
    );

    fun toTextAction(text: String, position: PointF) = Action.Text(
        text = text,
        fontStyle = fontStyle,
        color = color,
        size = size,
        position = position,
    )
}
