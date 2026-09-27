/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R

/**
 * Simple action bottom bar.
 */
@Composable
fun SimpleActionBottomBar(
    onConfirm: (() -> Unit)?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CancelActionFloatingButton(onClick = onCancel)

        onConfirm?.let { onConfirm ->
            ConfirmActionFloatingButton(onClick = onConfirm)
        }
    }
}

@Composable
fun CancelActionFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cancel_action),
        )
    }
}

@Composable
fun ConfirmActionFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.then(
            if (enabled) Modifier else Modifier.semantics { disabled() },
        ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
            alpha = if (enabled) 1f else 0.38f,
        ),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
            alpha = if (enabled) 1f else 0.38f,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.confirm_action),
        )
    }
}
