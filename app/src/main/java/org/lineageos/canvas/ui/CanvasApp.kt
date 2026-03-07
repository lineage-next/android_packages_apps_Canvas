/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.lineageos.canvas.R
import org.lineageos.canvas.ext.drawCropOverlay
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.Handle
import org.lineageos.canvas.models.Mode
import org.lineageos.canvas.ui.theme.CropOverlayStyle
import org.lineageos.canvas.ui.theme.defaultCropOverlayStyle
import org.lineageos.canvas.viewmodels.EditViewModel
import org.lineageos.canvas.viewmodels.UriViewModel
import kotlin.math.abs
import kotlin.math.min

@Composable
fun CanvasApp(
    uriViewModel: UriViewModel,
    editViewModel: EditViewModel = viewModel(),
    onSave: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    val uri by uriViewModel.uri.collectAsState()
    val isWritable by uriViewModel.isWritable.collectAsState()

    val canUndo by editViewModel.canUndo.collectAsState()
    val canRedo by editViewModel.canRedo.collectAsState()

    val mode by editViewModel.mode.collectAsState()
    val cropRect by editViewModel.cropRect.collectAsState()

    val actionsBitmap by editViewModel.actionsBitmap.collectAsState()

    val showTextEditor by editViewModel.showTextEditor.collectAsState()
    val textEditorPosition by editViewModel.textEditorPosition.collectAsState()
    val textEditorText by editViewModel.textEditorText.collectAsState()

    val currentUri = uri ?: return


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                isWritable = isWritable,
                canUndo = canUndo,
                canRedo = canRedo,
                onSave = { onSave(currentUri) },
                onUndo = { editViewModel.undo() },
                onRedo = { editViewModel.redo() },
                onShare = { onShare(currentUri) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ImageContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    uri = currentUri,
                    mode = mode,
                    cropRect = cropRect,
                    actionsBitmap = actionsBitmap,
                    onBaseRectChange = {
                        editViewModel.setBaseRect(it)
                    },
                    onCropRectChange = {
                        editViewModel.setPendingAction(
                            Action.Resize(it)
                        )
                    },
                    onCropRectCommit = {
                        editViewModel.commitPendingAction()
                    },
                    onImageClick = {
                        if (mode == Mode.TEXT) {
                            editViewModel.showTextEditor(it)
                        }
                    },
                )

                BottomToolbar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                    currentMode = mode,
                    onResize = { editViewModel.setMode(Mode.RESIZE) },
                    onText = {
                        editViewModel.setMode(Mode.TEXT)
                    },
                    onMarker = { editViewModel.setMode(Mode.MARKER) },
                    onEraser = { editViewModel.setMode(Mode.ERASER) },
                )
            }

            if (showTextEditor) {
                TextEditorOverlay(
                    text = textEditorText,
                    onTextChange = { editViewModel.updateTextEditorText(it) },
                    onDismiss = {
                        editViewModel.dismissTextEditor()
                    },
                    onConfirm = { text ->
                        textEditorPosition?.let { textEditorPosition ->
                            editViewModel.addAction(
                                org.lineageos.canvas.models.TextStyle.BLACK.toTextAction(
                                    text = text, position = textEditorPosition
                                )
                            )
                        }
                        editViewModel.dismissTextEditor()
                    },
                )
            }
        }
    }
}

@Composable
fun TextEditorOverlay(
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.8f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("TEST", color = Color.White, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { if (text.isNotBlank()) onConfirm(text) },
                    enabled = text.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (text.isNotBlank()) Color.White else Color.Gray
                    )
                }
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = stringResource(R.string.enter_text),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 32.sp,
                    )
                },
            )
        }
    }
}

@Composable
fun TopBar(
    isWritable: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            if (isWritable) {
                Button(onClick = onSave) {
                    Text(stringResource(R.string.save))
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.undo),
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.redo),
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share),
                )
            }
        }
    }
}

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

@Composable
fun ImageContainer(
    modifier: Modifier = Modifier,
    uri: Uri,
    mode: Mode?,
    cropRect: RectF?,
    actionsBitmap: Bitmap?,
    onBaseRectChange: (RectF) -> Unit,
    onCropRectChange: (RectF) -> Unit,
    onCropRectCommit: () -> Unit,
    onImageClick: (PointF) -> Unit = {},
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    val imageBounds = remember(containerSize, imageSize) {
        if (containerSize == Size.Zero || imageSize == Size.Zero) return@remember null

        val scale =
            min(containerSize.width / imageSize.width, containerSize.height / imageSize.height)

        val w = imageSize.width * scale
        val h = imageSize.height * scale
        val dx = (containerSize.width - w) / 2
        val dy = (containerSize.height - h) / 2

        RectF(dx, dy, dx + w, dy + h)
    }

    LaunchedEffect(imageBounds) {
        imageBounds?.let { imageBounds ->
            onBaseRectChange(imageBounds)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                containerSize = it.size.toSize()
            }
            .pointerInput(imageBounds) {
                detectTapGestures { offset ->
                    imageBounds?.let { bounds ->
                        if (bounds.contains(offset.x, offset.y)) {
                            onImageClick(
                                PointF(
                                    offset.x - bounds.left, offset.y - bounds.top
                                )
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = {
                imageSize = it.painter.intrinsicSize
            })

        imageBounds?.let { imageBounds ->
            actionsBitmap?.let {
                Image(
                    modifier = Modifier.size(
                        width = with(LocalDensity.current) { it.width.toDp() },
                        height = with(LocalDensity.current) { it.height.toDp() },
                    ),
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                )
            }

            cropRect?.let { cropRect ->
                if (mode == Mode.RESIZE) {
                    ResizeOverlay(
                        imageBounds = imageBounds,
                        cropRect = cropRect,
                        onCropRectChange = onCropRectChange,
                        onCropRectCommit = onCropRectCommit,
                    )
                } else if (cropRect != imageBounds) {
                    CropOverlay(cropRect = cropRect)
                }
            }
        }
    }
}

@Composable
private fun ResizeOverlay(
    imageBounds: RectF,
    cropRect: RectF,
    onCropRectChange: (RectF) -> Unit,
    onCropRectCommit: () -> Unit,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    var activeHandle by remember { mutableStateOf<Handle?>(null) }
    val currentCropRect by rememberUpdatedState(cropRect)

    val handleThresholdPx = with(LocalDensity.current) { 24.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(imageBounds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle =
                            getHandleForOffset(offset, currentCropRect, handleThresholdPx)
                    },
                    onDragEnd = {
                        activeHandle = null
                        onCropRectCommit()
                    },
                    onDragCancel = {
                        activeHandle = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val activeHandle = activeHandle ?: return@detectDragGestures

                        val newRect = RectF(currentCropRect)
                        updateRectWithDrag(newRect, activeHandle, dragAmount, imageBounds)
                        onCropRectChange(newRect)
                    },
                )
            },
    ) {
        drawCropOverlay(currentCropRect, showHandles = true, style)
    }
}

@Composable
fun CropOverlay(
    cropRect: RectF,
    modifier: Modifier = Modifier,
    style: CropOverlayStyle = defaultCropOverlayStyle(),
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawCropOverlay(cropRect, showHandles = false, style)
    }
}

private fun updateRectWithDrag(rect: RectF, handle: Handle, drag: Offset, bounds: RectF) {
    val minSize = 100f
    when (handle) {
        Handle.CENTER -> {
            val width = rect.width()
            val height = rect.height()

            var newLeft = rect.left + drag.x
            var newTop = rect.top + drag.y

            newLeft = newLeft.coerceIn(bounds.left, bounds.right - width)
            newTop = newTop.coerceIn(bounds.top, bounds.bottom - height)

            rect.set(newLeft, newTop, newLeft + width, newTop + height)
        }

        Handle.TOP -> rect.top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize)
        Handle.BOTTOM -> rect.bottom =
            (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom)

        Handle.LEFT -> rect.left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize)
        Handle.RIGHT -> rect.right =
            (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right)

        Handle.TOP_LEFT -> {
            rect.top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize)
            rect.left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize)
        }

        Handle.TOP_RIGHT -> {
            rect.top = (rect.top + drag.y).coerceIn(bounds.top, rect.bottom - minSize)
            rect.right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right)
        }

        Handle.BOTTOM_LEFT -> {
            rect.bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom)
            rect.left = (rect.left + drag.x).coerceIn(bounds.left, rect.right - minSize)
        }

        Handle.BOTTOM_RIGHT -> {
            rect.bottom = (rect.bottom + drag.y).coerceIn(rect.top + minSize, bounds.bottom)
            rect.right = (rect.right + drag.x).coerceIn(rect.left + minSize, bounds.right)
        }
    }
}

private fun getHandleForOffset(offset: Offset, rect: RectF, threshold: Float): Handle? {
    val x = offset.x
    val y = offset.y

    // Corner check
    val isTop = abs(y - rect.top) < threshold
    val isBottom = abs(y - rect.bottom) < threshold
    val isLeft = abs(x - rect.left) < threshold
    val isRight = abs(x - rect.right) < threshold

    return when {
        isTop && isLeft -> Handle.TOP_LEFT
        isTop && isRight -> Handle.TOP_RIGHT
        isBottom && isLeft -> Handle.BOTTOM_LEFT
        isBottom && isRight -> Handle.BOTTOM_RIGHT

        // Edge check
        isTop && x in (rect.left..rect.right) -> Handle.TOP
        isBottom && x in (rect.left..rect.right) -> Handle.BOTTOM
        isLeft && y in (rect.top..rect.bottom) -> Handle.LEFT
        isRight && y in (rect.top..rect.bottom) -> Handle.RIGHT

        // Rect check
        rect.contains(x, y) -> Handle.CENTER

        else -> null
    }
}
