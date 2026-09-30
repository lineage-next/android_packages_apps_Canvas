/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RotationScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (Action?) -> Unit,
    onCancel: () -> Unit,
) {
    var quarterTurns by remember { mutableIntStateOf(0) }
    val rotation = RotationStep.entries[quarterTurns.mod(RotationStep.entries.size)]

    val degrees by animateFloatAsState(quarterTurns * 90f)

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap.width > 0 && imageBitmap.height > 0 &&
                constraints.maxWidth > 0 && constraints.maxHeight > 0
            ) {
                val fitScale = min(
                    maxWidth.value / imageBitmap.width,
                    maxHeight.value / imageBitmap.height,
                )
                val fittedWidth = (imageBitmap.width * fitScale).dp
                val fittedHeight = (imageBitmap.height * fitScale).dp

                CanvasImage(
                    imageBitmap = imageBitmap,
                    cropRect = cropRect,
                    modifier = Modifier
                        .size(fittedWidth, fittedHeight)
                        .graphicsLayer {
                            val radians = degrees * PI.toFloat() / 180f
                            val cosine = abs(cos(radians))
                            val sine = abs(sin(radians))
                            val boundsWidth = size.width * cosine + size.height * sine
                            val boundsHeight = size.width * sine + size.height * cosine
                            val scale = min(
                                constraints.maxWidth / boundsWidth,
                                constraints.maxHeight / boundsHeight,
                            ).coerceAtMost(1f)

                            rotationZ = degrees
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { quarterTurns-- }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = null,
                )
            }

            IconButton(onClick = { quarterTurns++ }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = null,
                )
            }
        }

        SimpleActionBottomBar(
            onConfirm = {
                val action = rotation.takeIf { it != RotationStep.ROT_0 }?.let {
                    Action.Transformation.Rotation(it)
                }
                onConfirm(action)
            },
            onCancel = onCancel,
        )
    }
}
