/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CanvasTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = dynamicDarkColorScheme(context)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
