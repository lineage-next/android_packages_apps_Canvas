/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.ImageInformation
import org.lineageos.canvas.ui.composables.SimpleActionBottomBar
import org.lineageos.canvas.ui.composables.TextEditorOverlay

/**
 * Add a label on top of the image.
 */
@Composable
fun TextScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onAddAction: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var textEditorPosition by remember { mutableStateOf<IntOffset?>(null) }

    var imageInformation by remember { mutableStateOf<ImageInformation?>(null) }

    BackHandler(enabled = textEditorPosition != null) {
        textEditorPosition = null
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(innerPadding)
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = cropRect,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageInformation) {
                        detectTapGestures { offset ->
                            imageInformation?.let { imageInformation ->
                                if (imageInformation.imageViewRect.contains(offset)) {
                                    textEditorPosition =
                                        imageInformation.viewOffsetToOriginalBitmap(
                                            offset
                                        )
                                }
                            }
                        }
                    },
            ) { imageInformation = it }

            textEditorPosition?.let {
                TextEditorOverlay(
                    onDismiss = {
                        textEditorPosition = null
                    },
                    onConfirm = { text, textStyle ->
                        val action = Action.Drawing.Text(
                            text = text,
                            position = it,
                            style = textStyle,
                        )

                        onAddAction(action)

                        textEditorPosition = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SimpleActionBottomBar(
            onConfirm = null,
            onCancel = onCancel,
        )
    }
}
