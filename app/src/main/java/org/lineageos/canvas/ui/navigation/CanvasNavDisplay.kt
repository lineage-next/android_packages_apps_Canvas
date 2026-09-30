/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.LocalSharedTransitionScope
import org.lineageos.canvas.ui.screens.HomeScreen
import org.lineageos.canvas.ui.screens.CropScreen
import org.lineageos.canvas.ui.screens.BrightnessScreen
import org.lineageos.canvas.ui.screens.ContrastScreen
import org.lineageos.canvas.ui.screens.HighlighterScreen
import org.lineageos.canvas.ui.screens.MarkerScreen
import org.lineageos.canvas.ui.screens.RotationScreen
import org.lineageos.canvas.ui.screens.TextScreen

@Composable
fun CanvasNavDisplay(
    navigationBackStack: SnapshotStateList<Screen>,
    bitmapWithOverlayActions: ImageBitmap,
    finalResultBitmap: ImageBitmap,
    cropRect: IntRect,
    onAddAction: (Action) -> Unit,
    currentCategory: EditMode.Category?,
    onCategorySelected: (EditMode.Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entryProvider = entryProvider {
        entry<Screen.Home> {
            HomeScreen(
                imageBitmap = finalResultBitmap,
                cropRect = cropRect,
                currentCategory = currentCategory,
                onCategorySelected = onCategorySelected,
            )
        }

        entry<Screen.Edit> {
            when (it.editMode) {
                EditMode.CROP -> CropScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    initialCropRect = cropRect,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.ROTATION -> RotationScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.MARKER -> MarkerScreen(
                    imageBitmap = finalResultBitmap,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.HIGHLIGHTER -> HighlighterScreen(
                    imageBitmap = finalResultBitmap,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.TEXT -> TextScreen(
                    imageBitmap = finalResultBitmap,
                    cropRect = cropRect,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.BRIGHTNESS -> BrightnessScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.CONTRAST -> ContrastScreen(
                    imageBitmap = bitmapWithOverlayActions,
                    cropRect = cropRect,
                    onConfirm = { action ->
                        action?.let(onAddAction)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )
            }
        }
    }

    NavDisplay(
        backStack = navigationBackStack,
        modifier = modifier,
        sharedTransitionScope = LocalSharedTransitionScope.current,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        popTransitionSpec = { fadeIn() togetherWith fadeOut() },
        predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
        entryProvider = entryProvider,
    )
}
