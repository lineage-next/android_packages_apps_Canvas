/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.EditMode

/**
 * [EditMode] selector bar. Once an edit mode has been selected, this composable should disappear,
 * to let the mode's UI take over.
 */
@Composable
fun IdleBottomBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    currentCategory: EditMode.Category?,
    onCategorySelected: (EditMode.Category?) -> Unit,
    onEditModeSelected: (EditMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .height(64.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        AnimatedVisibility(
            visible = currentCategory != null,
            enter = slideInHorizontally { it / 2 } + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + fadeOut(),
        ) {
            currentCategory?.let {
                CategoryToolbar(
                    category = it,
                    onEditModeSelected = onEditModeSelected,
                    onBack = { onCategorySelected(null) },
                )
            }
        }

        AnimatedVisibility(
            visible = currentCategory == null,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut(),
        ) {
            RootIdleBottomBar(
                onCategorySelected = onCategorySelected,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RootIdleBottomBar(
    onCategorySelected: (EditMode.Category) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            onDo = onUndo,
            canDo = canUndo,
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = R.string.undo,
        )

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            LazyRow {
                items(EditMode.Category.entries) { category ->
                    CategoryButton(
                        category = category,
                        onCategorySelected = onCategorySelected,
                    )
                }
            }
        }

        ActionButton(
            onDo = onRedo,
            canDo = canRedo,
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = R.string.redo,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryToolbar(
    category: EditMode.Category,
    onEditModeSelected: (EditMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        FloatingActionButton(
            onClick = onBack,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.back),
            )
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            LazyRow {
                items(EditMode.ofCategory(category)) {
                    EditModeButton(
                        editMode = it,
                        onEditModeSelected = onEditModeSelected,
                    )
                }
            }
        }
    }
}

/**
 * Undo/redo button.
 */
@Composable
private fun ActionButton(
    canDo: Boolean,
    onDo: () -> Unit,
    imageVector: ImageVector,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onDo,
        enabled = canDo,
        modifier = modifier,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = stringResource(contentDescription),
        )
    }
}

/**
 * [EditMode.Category] button.
 */
@Composable
private fun CategoryButton(
    category: EditMode.Category,
    onCategorySelected: (EditMode.Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onCategorySelected(category) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = when (category) {
                EditMode.Category.TRANSFORMATION -> Icons.Filled.Transform
                EditMode.Category.DRAWING -> Icons.Filled.Draw
                EditMode.Category.ADJUSTMENTS -> Icons.Filled.Exposure
            },
            contentDescription = category.name,
        )
    }
}

/**
 * [EditMode] button.
 */
@Composable
fun EditModeButton(
    editMode: EditMode,
    onEditModeSelected: (EditMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onEditModeSelected(editMode) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = when (editMode) {
                EditMode.RESIZE -> Icons.Filled.Crop
                EditMode.ROTATION -> Icons.Filled.CropRotate
                EditMode.MARKER -> Icons.Filled.Draw
                EditMode.HIGHLIGHTER -> Icons.Filled.Draw
                EditMode.TEXT -> Icons.Filled.TextFields
                EditMode.BRIGHTNESS -> Icons.Filled.BrightnessMedium
                EditMode.CONTRAST -> Icons.Filled.Contrast
            },
            contentDescription = when (editMode) {
                EditMode.RESIZE -> R.string.edit_mode_resize
                EditMode.ROTATION -> R.string.edit_mode_rotation
                EditMode.MARKER -> R.string.edit_mode_marker
                EditMode.HIGHLIGHTER -> R.string.edit_mode_highlighter
                EditMode.TEXT -> R.string.edit_mode_text
                EditMode.BRIGHTNESS -> R.string.edit_mode_brightness
                EditMode.CONTRAST -> R.string.edit_mode_contrast
            }.let { stringResource(it) },
        )
    }
}
