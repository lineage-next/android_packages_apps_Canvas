/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
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
import org.lineageos.canvas.ui.screens.ResizeScreen
import org.lineageos.canvas.ui.screens.TextScreen

@Composable
fun CanvasNavDisplay(
    innerPadding: PaddingValues,
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
                innerPadding = innerPadding,
                imageBitmap = finalResultBitmap,
                cropRect = cropRect,
                currentCategory = currentCategory,
                onCategorySelected = onCategorySelected,
            )
        }

        entry<Screen.Edit> {
            when (it.editMode) {
                EditMode.RESIZE -> ResizeScreen(
                    innerPadding = innerPadding,
                    imageBitmap = bitmapWithOverlayActions,
                    initialCropRect = cropRect,
                    onConfirm = { action ->
                        onAddAction(action)
                        navigationBackStack.removeLastOrNull()
                    },
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.ROTATION -> TODO()

                EditMode.MARKER -> TODO()

                EditMode.HIGHLIGHTER -> TODO()

                EditMode.TEXT -> TextScreen(
                    innerPadding = innerPadding,
                    imageBitmap = finalResultBitmap,
                    cropRect = cropRect,
                    onAddAction = onAddAction,
                    onCancel = navigationBackStack::removeLastOrNull,
                )

                EditMode.BRIGHTNESS -> TODO()

                EditMode.CONTRAST -> TODO()
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
