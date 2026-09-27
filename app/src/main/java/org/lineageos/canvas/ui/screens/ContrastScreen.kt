/*
 * SPDX-FileCopyrightText: The LineageOS Project
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.contrastColorFilter
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.ui.composables.CanvasImage
import org.lineageos.canvas.ui.composables.SimpleActionBottomBar

@Composable
fun ContrastScreen(
    innerPadding: PaddingValues,
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    onConfirm: (Action.Adjustment.Contrast?) -> Unit,
    onCancel: () -> Unit,
) {
    var contrast by remember { mutableFloatStateOf(1f) }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    .padding(48.dp),
                colorFilter = remember(contrast) { contrastColorFilter(contrast) },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.edit_mode_contrast))
                Text(stringResource(R.string.adjustment_percent, contrast * 100))
            }

            Slider(
                value = contrast,
                onValueChange = { contrast = it },
                valueRange = 0f..2f,
            )
        }

        SimpleActionBottomBar(
            onConfirm = {
                onConfirm(contrast.takeIf { it != 1f }?.let(Action.Adjustment::Contrast))
            },
            onCancel = onCancel,
        )
    }
}
