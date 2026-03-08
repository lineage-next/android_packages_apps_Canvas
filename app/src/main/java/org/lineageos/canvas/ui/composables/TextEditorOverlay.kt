/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.canvas.R
import org.lineageos.canvas.models.TextStyle

@Composable
fun TextEditorOverlay(
    onDismiss: () -> Unit,
    onConfirm: (String, TextStyle) -> Unit,
) {
    val textFieldState = rememberTextFieldState()

    var fontFamily by remember {
        mutableStateOf(TextStyle.FontFamily.DEFAULT)
    }
    var textAlignment by remember { mutableStateOf(TextStyle.Alignment.CENTER) }
    var textColor by remember { mutableStateOf(Color.White) }
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var underlined by remember { mutableStateOf(false) }
    var strikethrough by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
    ) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                )
            }

            IconButton(
                onClick = {
                    val text = textFieldState.text.trim()
                    when (text.isNotBlank()) {
                        true -> onConfirm(
                            text.toString(),
                            TextStyle(
                                fontFamily = fontFamily,
                                size = TextStyle.DEFAULT.size,
                                color = textColor,
                                bold = bold,
                                italic = italic,
                                underlined = underlined,
                                strikethrough = strikethrough,
                            )
                        )

                        false -> {}
                    }
                },
                enabled = textFieldState.text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                )
            }
        }

        TextPropertiesRow {
            TextPropertyButton(
                imageVector = Icons.Default.FormatBold,
                checked = bold,
                contentDescription = R.string.text_decoration_bold,
            ) { bold = it }

            TextPropertyButton(
                imageVector = Icons.Default.FormatItalic,
                checked = italic,
                contentDescription = R.string.text_decoration_italic,
            ) { italic = it }

            TextPropertyButton(
                imageVector = Icons.Default.FormatUnderlined,
                checked = underlined,
                contentDescription = R.string.text_decoration_underlined,
            ) { underlined = it }

            TextPropertyButton(
                imageVector = Icons.Default.FormatStrikethrough,
                checked = strikethrough,
                contentDescription = R.string.text_decoration_strikethrough,
            ) { strikethrough = it }

            TextPropertiesDivider()

            TextAlignmentButton(
                imageVector = Icons.AutoMirrored.Filled.FormatAlignLeft,
                value = when (isRtl) {
                    true -> TextStyle.Alignment.RIGHT
                    false -> TextStyle.Alignment.LEFT
                },
                currentValue = textAlignment,
                contentDescription = R.string.text_alignment_left,
            ) { textAlignment = it }

            TextAlignmentButton(
                imageVector = Icons.Default.FormatAlignCenter,
                value = TextStyle.Alignment.CENTER,
                currentValue = textAlignment,
                contentDescription = R.string.text_alignment_center,
            ) { textAlignment = it }

            TextAlignmentButton(
                imageVector = Icons.AutoMirrored.Filled.FormatAlignRight,
                value = when (isRtl) {
                    true -> TextStyle.Alignment.LEFT
                    false -> TextStyle.Alignment.RIGHT
                },
                currentValue = textAlignment,
                contentDescription = R.string.text_alignment_right,
            ) { textAlignment = it }
        }

        TextPropertiesRow {
            mapOf(
                Color.White to R.string.text_color_white,
                Color.Black to R.string.text_color_black,
                Color.Red to R.string.text_color_red,
                Color.Green to R.string.text_color_green,
                Color.Blue to R.string.text_color_blue,
                Color.Yellow to R.string.text_color_yellow,
                Color.Magenta to R.string.text_color_magenta,
                Color.Cyan to R.string.text_color_cyan,
            ).forEach {
                TextColorButton(
                    color = it.key,
                    currentColor = textColor,
                    contentDescription = it.value,
                ) { color -> textColor = color }
            }
        }

        TextField(
            state = textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = textColor,
                fontSize = 32.sp,
                fontWeight = when (bold) {
                    true -> FontWeight.Bold
                    false -> FontWeight.Normal
                },
                fontStyle = when (italic) {
                    true -> FontStyle.Italic
                    false -> FontStyle.Normal
                },
                fontFamily = fontFamily.toCompose(),
                textDecoration = when {
                    underlined && strikethrough -> TextDecoration.Underline + TextDecoration.LineThrough
                    underlined -> TextDecoration.Underline
                    strikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                textAlign = textAlignment.toCompose(),
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            placeholder = {
                Text(
                    text = stringResource(R.string.enter_text),
                    modifier = Modifier.fillMaxWidth(),
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 32.sp,
                    fontStyle = when (italic) {
                        true -> FontStyle.Italic
                        false -> FontStyle.Normal
                    },
                    fontWeight = when (bold) {
                        true -> FontWeight.Bold
                        false -> FontWeight.Normal
                    },
                    fontFamily = fontFamily.toCompose(),
                    textDecoration = when {
                        strikethrough && underlined -> TextDecoration.LineThrough + TextDecoration.Underline
                        strikethrough -> TextDecoration.LineThrough
                        underlined -> TextDecoration.Underline
                        else -> TextDecoration.None
                    },
                    textAlign = textAlignment.toCompose(),
                )
            },
        )
    }
}

@Composable
private fun TextPropertiesRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        content()

        Spacer(modifier = Modifier.width(16.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TextPropertyButton(
    imageVector: ImageVector,
    checked: Boolean,
    @StringRes contentDescription: Int,
    onCheckedChange: (Boolean) -> Unit,
) {
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = stringResource(contentDescription),
        )
    }
}

@Composable
private fun TextAlignmentButton(
    imageVector: ImageVector,
    value: TextStyle.Alignment,
    currentValue: TextStyle.Alignment,
    @StringRes contentDescription: Int,
    onCheckedChange: (TextStyle.Alignment) -> Unit,
) {
    TextPropertyButton(
        imageVector = imageVector,
        checked = currentValue == value,
        contentDescription = contentDescription,
        onCheckedChange = { onCheckedChange(value) }
    )
}

@Composable
private fun TextPropertiesDivider() {
    VerticalDivider(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .height(32.dp),
        thickness = 2.dp,
    )
}

@Composable
private fun TextColorButton(
    color: Color,
    currentColor: Color,
    @StringRes contentDescription: Int,
    onClick: (color: Color) -> Unit,
) {
    val checked = color == currentColor

    OutlinedIconButton(
        onClick = { onClick(color) },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = color.copy(
                alpha = when (checked) {
                    true -> 0.5f
                    false -> 1f
                }
            ),
        )
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(contentDescription),
            )
        }
    }
}

private fun TextStyle.FontFamily.toCompose() = when (this) {
    TextStyle.FontFamily.DEFAULT -> FontFamily.Default
    TextStyle.FontFamily.SANS_SERIF -> FontFamily.SansSerif
    TextStyle.FontFamily.SERIF -> FontFamily.Serif
    TextStyle.FontFamily.MONOSPACE -> FontFamily.Monospace
}

private fun TextStyle.Alignment.toCompose() = when (this) {
    TextStyle.Alignment.LEFT -> TextAlign.Left
    TextStyle.Alignment.CENTER -> TextAlign.Center
    TextStyle.Alignment.RIGHT -> TextAlign.Right
}
