/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.app.Application
import android.net.Uri
import android.view.View
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import org.lineageos.canvas.models.DrawingContext
import org.lineageos.canvas.models.HistoryList
import org.lineageos.canvas.models.applyActionsAndDraw
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
     * The [DrawingContext] used to draw.
     */
    private val drawingContext by lazy {
        DrawingContext(
            fontFamilyResolver = createFontFamilyResolver(application),
            layoutDirection = getLayoutDirection(),
        )
    }

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
     * Rotated source bitmap composited with the drawing layer.
     * Used by the resize/crop screen.
     */
    val adjustedBitmapWithActions = combine(
        sourceBitmapWithAdjustments,
        actions,
    ) { sourceBitmap, actions ->
        val sourceBitmap = sourceBitmap ?: return@combine null

        val totalRotation = actions
            .filterIsInstance<Action.Transformation.Rotation>()
            .fold(0f) { acc, action -> (acc + action.rotation.degrees) % 360f }

        val isSwapped = (totalRotation / 90f).roundToInt() % 2 != 0

        sourceBitmap.createEmptyBitmap(
            width = if (isSwapped) sourceBitmap.height else sourceBitmap.width,
            height = if (isSwapped) sourceBitmap.width else sourceBitmap.height,
        ).draw {
            applyActionsAndDraw(sourceBitmap, actions, drawingContext)
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
        val bitmap = adjustedBitmapWithActions ?: return@combine null
        val crop = cropRect ?: bitmap.size.toIntRect()

        bitmap.createEmptyBitmap(
            width = crop.width,
            height = crop.height,
        ).draw {
            drawImage(
                image = bitmap,
                srcOffset = crop.topLeft,
                srcSize = crop.size,
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
        density: Density = Density(application.resources.displayMetrics.density),
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
