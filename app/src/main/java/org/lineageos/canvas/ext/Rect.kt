/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ScaleFactor

operator fun Rect.times(scaleFactor: ScaleFactor) = Rect(
    left = left * scaleFactor.scaleX,
    top = top * scaleFactor.scaleY,
    right = right * scaleFactor.scaleX,
    bottom = bottom * scaleFactor.scaleY,
)

operator fun Rect.div(scaleFactor: ScaleFactor) = Rect(
    left = left / scaleFactor.scaleX,
    top = top / scaleFactor.scaleY,
    right = right / scaleFactor.scaleX,
    bottom = bottom / scaleFactor.scaleY,
)
