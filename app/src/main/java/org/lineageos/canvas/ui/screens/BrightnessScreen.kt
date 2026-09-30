/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.brightnessColorFilter
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.AdjustmentControls
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.CanvasBottomBar

@Composable
fun BrightnessScreen(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (Action.Adjustment.Brightness?) -> Unit,
    onCancel: () -> Unit,
) {
    var brightness by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        CanvasImage(
            imageBitmap = imageBitmap,
            cropRect = cropRect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colorFilter = remember(brightness) { brightnessColorFilter(brightness) },
        )

        AdjustmentControls(
            label = R.string.edit_mode_brightness,
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = -1f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        CanvasBottomBar(
            onConfirm = {
                onConfirm(brightness.takeIf { it != 0f }?.let(Action.Adjustment::Brightness))
            },
            onCancel = onCancel,
        )
    }
}
