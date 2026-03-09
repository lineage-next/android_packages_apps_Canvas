/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ext

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

val ImageBitmap.size: IntSize
    get() = IntSize(width, height)
