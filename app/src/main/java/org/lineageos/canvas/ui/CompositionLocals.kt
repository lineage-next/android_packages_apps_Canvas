/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
    error("No shared transition scope provided by the parent")
}
