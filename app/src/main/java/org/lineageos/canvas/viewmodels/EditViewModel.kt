/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.app.Application
import android.net.Uri
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
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
import org.lineageos.canvas.ext.rotateBy
import org.lineageos.canvas.ext.adjustBrightness
import org.lineageos.canvas.ext.adjustContrast
import org.lineageos.canvas.ext.size
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.HistoryList

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModel(application: Application) : AndroidViewModel(application) {
    private data class RenderCache(
        val sourceBitmap: ImageBitmap,
        val actions: List<Action>,
        val bitmap: ImageBitmap,
    )

    private var renderCache: RenderCache? = null

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
     * Bitmap rendered by replaying all active actions in order. Crop and drawing geometry is
     * relative to the image state at the point where each action appears.
     */
    val adjustedBitmapWithActions = combine(
        sourceBitmap,
        actions,
    ) { sourceBitmap, actions ->
        val sourceBitmap = sourceBitmap ?: return@combine null

        val cached = renderCache
        val reusableCache = cached?.takeIf {
            it.sourceBitmap === sourceBitmap &&
                    it.actions.size <= actions.size &&
                    it.actions.indices.all { index -> it.actions[index] === actions[index] }
        }
        val startingBitmap = reusableCache?.bitmap ?: sourceBitmap
        val firstActionToRender = reusableCache?.actions?.size ?: 0

        val result =
            actions.drop(firstActionToRender).fold(startingBitmap) { currentBitmap, action ->
                currentBitmap.applyAction(action)
            }

        renderCache = RenderCache(sourceBitmap, actions.toList(), result)
        result
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
    val finalResultBitmap = adjustedBitmapWithActions

    /**
     * Bounds of the currently rendered image, used to map view coordinates to image coordinates.
     */
    val cropRect = adjustedBitmapWithActions.mapLatest { bitmap ->
        bitmap?.size?.toIntRect()
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
        if (_uri.value != uri) {
            historyList.clear()
        }
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

    private fun ImageBitmap.applyAction(action: Action): ImageBitmap = when (action) {
        is Action.Adjustment.Brightness -> adjustBrightness(action.value)
        is Action.Adjustment.Contrast -> adjustContrast(action.value)
        is Action.Transformation.Crop -> crop(action.rect)
        is Action.Transformation.Rotation -> rotateBy(action.rotation)
        is Action.Drawing -> createEmptyBitmap(hasAlpha = true).draw {
            drawImage(this@applyAction)
            drawAction(action)
        }
    }

    private fun DrawScope.drawAction(action: Action) {
        when (action) {
            is Action.Adjustment -> when (action) {
                is Action.Adjustment.Brightness -> {
                    // Handled by the action replay pipeline
                }

                is Action.Adjustment.Contrast -> {
                    // Handled by the action replay pipeline
                }
            }

            is Action.Transformation -> when (action) {
                is Action.Transformation.Crop -> {
                    // Handled by the action replay pipeline
                }

                is Action.Transformation.Rotation -> {
                    // Handled by the action replay pipeline
                }
            }

            is Action.Drawing -> when (action) {
                is Action.Drawing.Marker -> drawStroke(
                    points = action.points,
                    color = action.color,
                    strokeWidth = action.strokeWidth,
                )

                is Action.Drawing.Highlighter -> drawStroke(
                    points = action.points,
                    color = action.color,
                    strokeWidth = action.strokeWidth,
                )

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

                is Action.Drawing.Eraser -> drawStroke(
                    points = action.marker.points,
                    color = action.marker.color,
                    strokeWidth = action.marker.strokeWidth,
                    blendMode = BlendMode.Clear,
                )
            }
        }
    }

    private fun DrawScope.drawStroke(
        points: List<androidx.compose.ui.unit.IntOffset>,
        color: androidx.compose.ui.graphics.Color,
        strokeWidth: Float,
        blendMode: BlendMode = BlendMode.SrcOver,
    ) {
        if (points.isEmpty()) return

        val firstPoint = points.first().toOffset()
        if (points.size == 1) {
            drawCircle(
                color = color,
                radius = strokeWidth / 2f,
                center = firstPoint,
                blendMode = blendMode,
            )
            return
        }

        val path = Path().apply {
            moveTo(firstPoint.x, firstPoint.y)
            points.drop(1).forEach { point ->
                lineTo(point.x.toFloat(), point.y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
            blendMode = blendMode,
        )
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

    /**
     * Create a cropped bitmap. The rectangle is clipped to this bitmap's bounds.
     */
    private fun ImageBitmap.crop(rect: androidx.compose.ui.unit.IntRect): ImageBitmap {
        val left = rect.left.coerceIn(0, width - 1)
        val top = rect.top.coerceIn(0, height - 1)
        val right = rect.right.coerceIn(left + 1, width)
        val bottom = rect.bottom.coerceIn(top + 1, height)
        val source = this

        return createEmptyBitmap(
            width = right - left,
            height = bottom - top,
        ).draw {
            drawImage(
                image = source,
                srcOffset = androidx.compose.ui.unit.IntOffset(left, top),
                srcSize = androidx.compose.ui.unit.IntSize(right - left, bottom - top),
            )
        }
    }
}
