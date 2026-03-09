/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor

operator fun Offset.times(scaleFactor: ScaleFactor) = Offset(
    x = x * scaleFactor.scaleX,
    y = y * scaleFactor.scaleY,
)

operator fun Offset.div(scaleFactor: ScaleFactor) = Offset(
    x = x / scaleFactor.scaleX,
    y = y / scaleFactor.scaleY,
)
