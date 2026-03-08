/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui

import android.graphics.PointF
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.Mode
import org.lineageos.canvas.ui.composables.BottomToolbar
import org.lineageos.canvas.ui.composables.ImageContainer
import org.lineageos.canvas.ui.composables.TextEditorOverlay
import org.lineageos.canvas.ui.composables.TopBar
import org.lineageos.canvas.viewmodels.EditViewModel
import org.lineageos.canvas.viewmodels.UriViewModel

@Composable
fun CanvasApp(
    uriViewModel: UriViewModel,
    editViewModel: EditViewModel = viewModel(),
    onSave: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    val uri by uriViewModel.uri.collectAsState()
    val isWritable by uriViewModel.isWritable.collectAsState()

    val canUndo by editViewModel.canUndo.collectAsState()
    val canRedo by editViewModel.canRedo.collectAsState()

    val mode by editViewModel.mode.collectAsState()
    val cropRect by editViewModel.cropRect.collectAsState()

    val actionsBitmap by editViewModel.actionsBitmap.collectAsState()

    val currentUri = uri ?: return

    var textEditorPosition by remember {
        mutableStateOf<PointF?>(null)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                isWritable = isWritable,
                canUndo = canUndo,
                canRedo = canRedo,
                onSave = { onSave(currentUri) },
                onUndo = { editViewModel.undo() },
                onRedo = { editViewModel.redo() },
                onShare = { onShare(currentUri) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ImageContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    uri = currentUri,
                    mode = mode,
                    cropRect = cropRect,
                    actionsBitmap = actionsBitmap,
                    onBaseRectChange = {
                        editViewModel.setBaseRect(it)
                    },
                    onCropRectChange = {
                        editViewModel.setPendingAction(
                            Action.Resize(it)
                        )
                    },
                    onCropRectCommit = {
                        editViewModel.commitPendingAction()
                    },
                    onImageClick = {
                        when (mode) {
                            Mode.TEXT -> textEditorPosition = it
                            else -> {}
                        }
                    },
                )

                BottomToolbar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                    currentMode = mode,
                    onModeSelected = editViewModel::setMode,
                )
            }

            textEditorPosition?.let {
                TextEditorOverlay(
                    onDismiss = {
                        textEditorPosition = null
                    },
                    onConfirm = { text, textStyle ->
                        editViewModel.addAction(
                            Action.Text(
                                text = text,
                                position = it,
                                textStyle = textStyle,
                            )
                        )

                        textEditorPosition = null
                    },
                )
            }
        }
    }
}
