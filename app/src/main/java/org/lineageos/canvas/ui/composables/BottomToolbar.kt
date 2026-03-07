/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.Mode

@Composable
fun BottomToolbar(
    modifier: Modifier = Modifier,
    currentMode: Mode?,
    onModeSelected: (Mode) -> Unit,
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(
                mode = Mode.RESIZE,
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                icon = Icons.Default.AspectRatio,
                label = stringResource(R.string.resize),
            )
            ToolButton(
                mode = Mode.TEXT,
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                icon = Icons.Default.Title,
                label = stringResource(R.string.text),
            )
            ToolButton(
                mode = Mode.MARKER,
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                icon = Icons.Default.Edit,
                label = stringResource(R.string.marker),
            )
            ToolButton(
                mode = Mode.ERASER,
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                icon = Icons.Default.AutoFixNormal,
                label = stringResource(R.string.eraser),
            )
        }
    }
}

@Composable
private fun ToolButton(
    mode: Mode,
    currentMode: Mode?,
    onModeSelected: (Mode) -> Unit,
    icon: ImageVector,
    label: String,
) {
    IconButton(
        onClick = { onModeSelected(mode) },
        enabled = currentMode != mode,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(
            icon,
            contentDescription = label,
        )
    }
}
