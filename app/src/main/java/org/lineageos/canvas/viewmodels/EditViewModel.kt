/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.app.Application
import android.net.Uri
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntRect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.lineageos.canvas.ext.size
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.DrawingGroup
import org.lineageos.canvas.models.HistoryList
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Coil's ImageLoader.
     */
    private val imageLoader = application.imageLoader

    /**
     * The [HistoryList] of actions.
     */
    private val historyList = HistoryList<Action>()

    /**
     * The URI of the image.
     */
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri = _uri.asStateFlow()

    /**
     * Whether the image is writable.
     */
    private val _isWritable = MutableStateFlow(false)
    val isWritable = _isWritable.asStateFlow()

    /**
     * The [FontFamily.Resolver] used to resolve fonts.
     */
    private val fontFamilyResolver = createFontFamilyResolver(application)

    /**
     * The untouched bitmap of the image.
     */
    val sourceBitmap = uri
        .mapLatest { uri ->
            val sharedKey = uri.toString()

            val imageRequest = ImageRequest.Builder(application)
                .data(uri)
                .allowHardware(false)
                .placeholderMemoryCacheKey(sharedKey)
                .memoryCacheKey(sharedKey)
                .build()

            val imageResult = imageLoader.execute(imageRequest)

            imageResult.image?.toBitmap()?.asImageBitmap()
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * The currently active actions applied to the image.
     */
    private val actions = historyList.snapshot
        .mapLatest { it.currentElements }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    /**
     * Whether there is an action that can be undone.
     */
    val canUndo = historyList.snapshot
        .mapLatest { it.canUndo }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /**
     * Whether there is an action that can be redone.
     */
    val canRedo = historyList.snapshot
        .mapLatest { it.canRedo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /**
     * Last applied crop action rectangle.
     */
    val cropRect = combine(
        actions,
        sourceBitmap,
    ) { actions, sourceBitmap ->
        val lastResizeIndex = actions.indexOfLast { it is Action.Transformation.Resize }
        val lastRotationIndex = actions.indexOfLast { it is Action.Transformation.Rotation }

        // Discard the crop if a rotation happened after it
        val validResize = if (lastResizeIndex > lastRotationIndex) {
            actions[lastResizeIndex] as Action.Transformation.Resize
        } else null

        validResize?.rect ?: sourceBitmap?.size?.toIntRect()
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Source bitmap with [Action.Adjustment]s applied.
     */
    val sourceBitmapWithAdjustments = combine(
        sourceBitmap,
        actions,
    ) { sourceBitmap, actions ->
        val sourceBitmap = sourceBitmap ?: return@combine null

        val adjustmentActions = actions.filterIsInstance<Action.Adjustment>().ifEmpty {
            return@combine sourceBitmap
        }

        // TODO: Apply them

        sourceBitmap
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * The clockwise rotation in degrees accumulated from all [Action.Transformation.Rotation] actions.
     */
    private val totalRotation = actions
        .mapLatest {
            it.filterIsInstance<Action.Transformation.Rotation>()
                .fold(0f) { acc, action -> (acc + action.rotation.degrees) % 360f }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 0f,
        )

    /**
     * Partitions the action list into [DrawingGroup]s.
     */
    private fun partitionIntoDrawingGroups(
        actions: List<Action>,
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<DrawingGroup> {
        val groups = mutableListOf<DrawingGroup>()
        var cumulativeAngle = 0f
        var currentWidth = sourceWidth
        var currentHeight = sourceHeight
        var currentDrawings = mutableListOf<Action.Drawing>()

        for (action in actions) {
            when (action) {
                is Action.Transformation.Rotation -> {
                    groups += DrawingGroup(
                        cumulativeAngle,
                        currentWidth,
                        currentHeight,
                        currentDrawings,
                    )
                    cumulativeAngle = (cumulativeAngle + action.rotation.degrees) % 360f
                    if (action.rotation.degrees % 180f != 0f) {
                        val tmp = currentWidth; currentWidth = currentHeight; currentHeight = tmp
                    }
                    currentDrawings = mutableListOf()
                }

                is Action.Drawing -> currentDrawings += action
                else -> {}
            }
        }

        groups += DrawingGroup(
            cumulativeAngle,
            currentWidth,
            currentHeight,
            currentDrawings,
        )
        return groups
    }

    /**
     * Blank bitmap with [Action.Drawing]s applied in their respective rotation spaces.
     *
     * Each [DrawingGroup] is drawn onto the final-orientation canvas with a compensating
     * [withTransform] rotation so that coordinates authored before earlier rotations are
     * placed correctly in the final output space.
     *
     * The bitmap dimensions already reflect the final rotation (width/height swapped for
     * 90°/270°), so this bitmap can be composited directly onto the rotated source.
     */
    val drawingActionsBitmap = combine(
        sourceBitmap,
        actions,
        totalRotation,
    ) { sourceBitmap, actions, totalRotation ->
        val sourceBitmap = sourceBitmap ?: return@combine null

        val isSwapped = (totalRotation / 90f).roundToInt() % 2 != 0
        val targetWidth = if (isSwapped) sourceBitmap.height else sourceBitmap.width
        val targetHeight = if (isSwapped) sourceBitmap.width else sourceBitmap.height

        val erasedMarkers = actions
            .filterIsInstance<Action.Drawing.Eraser>()
            .map { it.marker }
            .toSet()

        val groups = partitionIntoDrawingGroups(
            actions,
            sourceBitmap.width,
            sourceBitmap.height,
        )

        sourceBitmap.createEmptyBitmap(
            width = targetWidth,
            height = targetHeight,
            hasAlpha = true,
        ).draw {
            for (group in groups) {
                val remainingAngle = (totalRotation - group.rotationBefore + 360f) % 360f

                withTransform({
                    rotate(
                        degrees = remainingAngle,
                        pivot = Offset(group.widthBefore / 2f, group.heightBefore / 2f)
                    )
                }) {
                    group.drawings.forEach { drawDrawingAction(it, erasedMarkers) }
                }
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Source bitmap with [Action.Adjustment]s and [Action.Transformation.Rotation]s applied,
     * used for compositing with the drawing layer.
     */
    val rotatedSourceBitmap = combine(
        sourceBitmapWithAdjustments,
        totalRotation,
    ) { sourceBitmapWithAdjustments, totalRotation ->
        val sourceBitmapWithAdjustments = sourceBitmapWithAdjustments ?: return@combine null

        if (totalRotation == 0f) return@combine sourceBitmapWithAdjustments

        val isSwapped = (totalRotation / 90f).roundToInt() % 2 != 0
        val targetWidth =
            if (isSwapped) sourceBitmapWithAdjustments.height else sourceBitmapWithAdjustments.width
        val targetHeight =
            if (isSwapped) sourceBitmapWithAdjustments.width else sourceBitmapWithAdjustments.height

        sourceBitmapWithAdjustments.createEmptyBitmap(
            width = targetWidth,
            height = targetHeight,
        ).draw {
            withTransform({
                rotate(totalRotation, Offset(targetWidth / 2f, targetHeight / 2f))
                translate(
                    left = (targetWidth - sourceBitmapWithAdjustments.width) / 2f,
                    top = (targetHeight - sourceBitmapWithAdjustments.height) / 2f,
                )
            }) {
                drawImage(sourceBitmapWithAdjustments)
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Rotated source bitmap composited with the drawing layer.
     * Used by the resize/crop screen.
     */
    val adjustedBitmapWithActions = combine(
        rotatedSourceBitmap,
        drawingActionsBitmap,
    ) { rotatedSourceBitmap, drawingActionsBitmap ->
        val rotatedSourceBitmap = rotatedSourceBitmap ?: return@combine null
        val drawingActionsBitmap = drawingActionsBitmap ?: return@combine null

        rotatedSourceBitmap.createEmptyBitmap().draw {
            drawImage(rotatedSourceBitmap)
            drawImage(drawingActionsBitmap)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * The final result.
     */
    val finalResultBitmap = combine(
        adjustedBitmapWithActions,
        cropRect,
    ) { adjustedBitmapWithActions, cropRect ->
        val adjustedBitmapWithActions = adjustedBitmapWithActions ?: return@combine null
        val cropRect = cropRect ?: adjustedBitmapWithActions.size.toIntRect()

        adjustedBitmapWithActions.createEmptyBitmap(
            width = cropRect.width,
            height = cropRect.height,
        ).draw {
            drawImage(
                image = adjustedBitmapWithActions,
                srcOffset = cropRect.topLeft,
                srcSize = cropRect.size,
            )
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Set the URI of the image to edit.
     *
     * @param uri the URI of the image to edit
     * @param isWritable whether the image is writable
     */
    fun setUri(uri: Uri, isWritable: Boolean) {
        _uri.value = uri
        _isWritable.value = isWritable
    }

    /**
     * Add an action to the history list.
     *
     * @param action the action to add
     */
    fun addAction(action: Action) {
        historyList.insert(action)
    }

    fun undo() {
        historyList.undo()
    }

    fun redo() {
        historyList.redo()
    }

    /**
     * Draws a single [Action.Drawing] onto the current [DrawScope].
     * Rotation is handled at the call site via [withTransform] — do not apply any coordinate
     * transform here.
     *
     * @param action the drawing action to render
     * @param erasedMarkers the set of [Action.Drawing.Marker]s that have been erased and must
     *   be skipped
     */
    private fun DrawScope.drawDrawingAction(
        action: Action.Drawing,
        erasedMarkers: Set<Action.Drawing.Marker>,
    ) {
        when (action) {
            is Action.Drawing.Text -> {
                val textMeasurer = TextMeasurer(
                    defaultFontFamilyResolver = fontFamilyResolver,
                    defaultDensity = this,
                    defaultLayoutDirection = layoutDirection,
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = action.text,
                    topLeft = Offset(
                        action.position.x.toFloat(),
                        action.position.y.toFloat(),
                    ),
                    style = action.style,
                )
            }

            is Action.Drawing.Marker -> {
                if (action !in erasedMarkers) {
                    // TODO: drawMarker(action)
                }
            }

            is Action.Drawing.Eraser -> {
                // Handled via erasedMarkers pre-pass at the call site.
            }
        }
    }

    /**
     * Get the layout direction.
     */
    private fun getLayoutDirection() = when (application.resources.configuration.layoutDirection) {
        View.LAYOUT_DIRECTION_LTR -> LayoutDirection.Ltr
        View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
        else -> error("Unknown layout direction")
    }

    /**
     * Draw the [DrawScope] into this [ImageBitmap].
     */
    private fun ImageBitmap.draw(
        density: Density = Density(1f), // TODO: Density(context) exists, but this might be ok
        layoutDirection: LayoutDirection = getLayoutDirection(),
        canvas: Canvas = Canvas(this),
        size: Size = Size(width.toFloat(), height.toFloat()),
        block: DrawScope.() -> Unit,
    ) = this.apply {
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = size,
            block = block,
        )
    }

    /**
     * Create a new [ImageBitmap] with the same size and config as this one.
     */
    private fun ImageBitmap.createEmptyBitmap(
        width: Int = this.width,
        height: Int = this.height,
        config: ImageBitmapConfig = this.config,
        hasAlpha: Boolean = this.hasAlpha,
        colorSpace: ColorSpace = this.colorSpace,
    ): ImageBitmap = ImageBitmap(
        width = width,
        height = height,
        config = config,
        hasAlpha = hasAlpha,
        colorSpace = colorSpace,
    )
}
