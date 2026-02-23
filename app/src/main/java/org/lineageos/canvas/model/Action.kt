/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.model

import android.graphics.RectF

sealed interface Action {
    data class Resize(val rect: RectF) : Action
    data object Marker : Action
    data object Text : Action
    data object Eraser : Action
}
