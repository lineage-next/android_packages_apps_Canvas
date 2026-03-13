/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.RotationStep
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.SimpleActionBottomBar

@Composable
fun RotationScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (Action) -> Unit,
    onCancel: () -> Unit,
) {
    var rotation by remember { mutableStateOf(RotationStep.ROT_0) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CanvasImage(
                imageBitmap = imageBitmap,
                cropRect = cropRect,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
                    .graphicsLayer {
                        rotationZ = rotation.degrees
                    },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { rotation = rotation.counterClockwise() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = null,
                )
            }

            IconButton(
                onClick = { rotation = rotation.clockwise() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = null,
                )
            }
        }

        SimpleActionBottomBar(
            onConfirm = {
                onConfirm(Action.Transformation.Rotation(rotation))
            },
            onCancel = onCancel,
        )
    }
}
