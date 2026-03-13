/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import org.lineageos.canvas.R
import org.lineageos.canvas.models.EditMode
import org.lineageos.canvas.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CanvasTopAppBar(
    canGoBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    currentScreen: Screen,
    isWritable: Boolean,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            when (currentScreen) {
                is Screen.Home -> null

                is Screen.Edit -> when (currentScreen.editMode) {
                    EditMode.RESIZE -> R.string.edit_mode_resize
                    EditMode.ROTATION -> R.string.edit_mode_resize
                    EditMode.MARKER -> R.string.edit_mode_resize
                    EditMode.HIGHLIGHTER -> R.string.edit_mode_resize
                    EditMode.TEXT -> R.string.edit_mode_text
                    EditMode.BRIGHTNESS -> R.string.edit_mode_resize
                    EditMode.CONTRAST -> R.string.edit_mode_resize
                }
            }?.let {
                Text(text = stringResource(it))
            }
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = when (canGoBack) {
                    true -> onBack
                    false -> onClose
                },
            ) {
                Icon(
                    imageVector = when (canGoBack) {
                        true -> Icons.AutoMirrored.Filled.ArrowBack
                        false -> Icons.Filled.Close
                    },
                    contentDescription = null,
                )
            }
        },
        actions = {
            when (currentScreen) {
                is Screen.Home -> {
                    Box {
                        var checked by remember { mutableStateOf(false) }

                        SplitButtonLayout(
                            leadingButton = {
                                val description = when (isWritable) {
                                    true -> stringResource(R.string.save)
                                    false -> stringResource(R.string.share)
                                }

                                SplitButtonDefaults.LeadingButton(
                                    onClick = when (isWritable) {
                                        true -> onSave
                                        false -> onShare
                                    },
                                ) {
                                    Icon(
                                        imageVector = when (isWritable) {
                                            true -> Icons.Default.Save
                                            false -> Icons.Default.Share
                                        },
                                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                        contentDescription = description,
                                    )

                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                                    Text(description)
                                }
                            },
                            trailingButton = {
                                val description = stringResource(R.string.more)

                                TooltipBox(
                                    positionProvider =
                                        TooltipDefaults.rememberTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text(description) } },
                                    state = rememberTooltipState(),
                                ) {
                                    SplitButtonDefaults.TrailingButton(
                                        checked = checked,
                                        onCheckedChange = { checked = it },
                                        modifier = Modifier.semantics {
                                            stateDescription = when (checked) {
                                                true -> "Expanded"
                                                false -> "Collapsed"
                                            }
                                            contentDescription = description
                                        },
                                    ) {
                                        val rotation by animateFloatAsState(
                                            targetValue = when (checked) {
                                                true -> 180f
                                                false -> 0f
                                            },
                                            label = "Trailing Icon Rotation",
                                        )

                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            modifier = Modifier
                                                .size(SplitButtonDefaults.TrailingIconSize)
                                                .graphicsLayer { rotationZ = rotation },
                                            contentDescription = "Localized description",
                                        )
                                    }
                                }
                            },
                        )

                        DropdownMenu(
                            expanded = checked,
                            onDismissRequest = { checked = false },
                        ) {
                            if (isWritable) {
                                stringResource(R.string.share).let {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = onShare,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = it,
                                            )
                                        },
                                    )
                                }
                            }

                            stringResource(R.string.save_as).let {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = { onSaveAs() },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.SaveAs,
                                            contentDescription = it,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        },
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}
