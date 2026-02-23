/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface

sealed interface Action {
    fun drawInto(canvas: Canvas) {}

    data class Resize(val rect: RectF) : Action

    data object Marker : Action

    data class Text(
        val text: String?,
        val fontStyle: Int,
        val color: Int,
        val size: Float,
        val position: PointF,
    ) : Action {
        override fun drawInto(canvas: Canvas) {
            requireNotNull(text)

            val paint = Paint().apply {
                isAntiAlias = true
                color = this@Text.color
                textSize = size
                typeface = Typeface.create(Typeface.DEFAULT, fontStyle)
            }
            canvas.drawText(text, position.x, position.y, paint)
        }
    }

    data object Eraser : Action
}
