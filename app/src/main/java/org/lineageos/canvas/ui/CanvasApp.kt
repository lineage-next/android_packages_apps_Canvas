/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.composables.CanvasTopAppBar
import org.lineageos.canvas.ui.navigation.CanvasNavDisplay
import org.lineageos.canvas.ui.navigation.Screen
import org.lineageos.canvas.viewmodels.EditViewModel

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CanvasApp(
    editViewModel: EditViewModel,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onShare: () -> Unit,
) {
    val isWritable by editViewModel.isWritable.collectAsState()

    val canUndo by editViewModel.canUndo.collectAsState()
    val canRedo by editViewModel.canRedo.collectAsState()

    val cropRect by editViewModel.cropRect.collectAsState()

    val adjustedBitmapWithActions by editViewModel.adjustedBitmapWithActions.collectAsState()
    val finalResultBitmap by editViewModel.finalResultBitmap.collectAsState()

    var currentCategory by remember { mutableStateOf<EditMode.Category?>(null) }

    val navigationBackStack = remember { mutableStateListOf<Screen>(Screen.Home) }

    SharedTransitionScope { modifier ->
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    CanvasTopAppBar(
                        canClose = navigationBackStack.size == 1,
                        onClose = onClose,
                        currentScreen = navigationBackStack.last(),
                        isWritable = isWritable,
                        onSave = onSave,
                        onSaveAs = onSaveAs,
                        onShare = onShare,
                    )
                },
            ) { innerPadding ->
                CanvasNavDisplay(
                    navigationBackStack = navigationBackStack,
                    bitmapWithOverlayActions = adjustedBitmapWithActions ?: return@Scaffold,
                    finalResultBitmap = finalResultBitmap ?: return@Scaffold,
                    cropRect = cropRect ?: return@Scaffold,
                    onAddAction = editViewModel::addAction,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = editViewModel::undo,
                    onRedo = editViewModel::redo,
                    currentCategory = currentCategory,
                    onCategorySelected = { currentCategory = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            }
        }
    }
}
