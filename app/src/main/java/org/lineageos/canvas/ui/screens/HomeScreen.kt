/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.CanvasBottomBar
import org.lineageos.canvas.ui.composables.HomeBottomBarContent

/**
 * Start screen. Here you can select which edit mode you wanna go to, revert or redo changes and
 * save the result.
 */
@Composable
fun HomeScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    currentCategory: EditMode.Category?,
    onCategorySelected: (EditMode.Category?) -> Unit,
    onEditModeSelected: (EditMode) -> Unit,
) {
    BackHandler(enabled = currentCategory != null) {
        onCategorySelected(null)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CanvasImage(
            imageBitmap = imageBitmap,
            cropRect = cropRect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
        )

        CanvasBottomBar {
            HomeBottomBarContent(
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                currentCategory = currentCategory,
                onCategorySelected = onCategorySelected,
                onEditModeSelected = onEditModeSelected,
            )
        }
    }
}
