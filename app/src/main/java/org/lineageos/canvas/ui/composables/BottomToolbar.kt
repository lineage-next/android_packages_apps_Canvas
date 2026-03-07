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
    onResize: () -> Unit,
    onText: () -> Unit,
    onMarker: () -> Unit,
    onEraser: () -> Unit,
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
                onClick = onResize,
                icon = Icons.Default.AspectRatio,
                label = stringResource(R.string.resize),
                selected = currentMode == Mode.RESIZE
            )
            ToolButton(
                onClick = onText,
                icon = Icons.Default.Title,
                label = stringResource(R.string.text),
                selected = currentMode == Mode.TEXT
            )
            ToolButton(
                onClick = onMarker,
                icon = Icons.Default.Edit,
                label = stringResource(R.string.marker),
                selected = currentMode == Mode.MARKER
            )
            ToolButton(
                onClick = onEraser,
                icon = Icons.Default.AutoFixNormal,
                label = stringResource(R.string.eraser),
                selected = currentMode == Mode.ERASER
            )
        }
    }
}

@Composable
private fun ToolButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    selected: Boolean,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
    ) {
        Icon(
            icon,
            contentDescription = label,
        )
    }
}
