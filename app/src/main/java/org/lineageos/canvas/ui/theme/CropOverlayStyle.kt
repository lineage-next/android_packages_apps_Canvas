/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CropOverlayStyle(
    val dimColor: Color,
    val borderColor: Color,
    val handleColor: Color,
    val strokeWidth: Dp,
    val handleRadius: Dp,
)

@Composable
fun defaultCropOverlayStyle() = CropOverlayStyle(
    dimColor = Color.Black.copy(alpha = 0.5f),
    borderColor = Color.White.copy(alpha = 0.7f),
    handleColor = MaterialTheme.colorScheme.primary,
    strokeWidth = 4.dp,
    handleRadius = 24.dp,
)
