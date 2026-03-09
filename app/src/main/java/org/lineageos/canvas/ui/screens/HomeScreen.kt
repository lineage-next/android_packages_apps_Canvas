/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.composables.CanvasImage

/**
 * Start screen. Here you can select which edit mode you wanna go to, revert or redo changes and
 * save the result.
 */
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    currentCategory: EditMode.Category?,
    onCategorySelected: (EditMode.Category?) -> Unit,
) {
    BackHandler(enabled = currentCategory != null) {
        onCategorySelected(null)
    }

    CanvasImage(
        imageBitmap = imageBitmap,
        cropRect = cropRect,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    )
}
