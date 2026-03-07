/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.PointF
import android.graphics.RectF

sealed interface Action {
    data class Resize(val rect: RectF) : Action

    data object Marker : Action

    data class Text(
        val text: String?,
        val fontStyle: Int,
        val color: Int,
        val size: Float,
        val position: PointF,
    ) : Action

    data object Eraser : Action
}
