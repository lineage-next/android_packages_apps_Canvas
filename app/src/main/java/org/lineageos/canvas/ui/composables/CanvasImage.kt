/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.toSize
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import org.lineageos.canvas.ext.size
import org.lineageos.canvas.ext.times
import org.lineageos.canvas.ui.LocalSharedTransitionScope
import kotlin.math.min

/**
 * Image information.
 *
 * @param bitmapSize The size of the displayed bitmap, which may be modified compared to the
 *   original file
 * @param bitmapCropRect The crop rectangle applied to the bitmap
 * @param imageViewRect The rect of the image view
 */
data class ImageInformation(
    val bitmapSize: IntSize,
    val bitmapCropRect: IntRect?,
    val imageViewRect: Rect,
) {
    /**
     * [ScaleFactor] used to convert a point in the bitmap to a point in the image view.
     */
    private val bitmapToViewScaleFactor by lazy {
        ScaleFactor(
            scaleX = imageViewRect.width / bitmapSize.width,
            scaleY = imageViewRect.height / bitmapSize.height,
        )
    }

    /**
     * [ScaleFactor] used to convert a point in the image view to a point in the bitmap.
     */
    private val viewToBitmapScaleFactor by lazy {
        ScaleFactor(
            scaleX = bitmapSize.width / imageViewRect.width,
            scaleY = bitmapSize.height / imageViewRect.height,
        )
    }

    /**
     * Convert a [IntRect] in the original bitmap to a [Rect] in the image view.
     */
    fun originalBitmapRectToViewRect(
        originalBitmapRect: IntRect,
    ): Rect = bitmapRectToView(originalBitmapRect).let { viewRect ->
        bitmapCropRect?.let {
            viewRect.translate(-imageViewRect.topLeft)
        } ?: viewRect
    }

    /**
     * Convert a [Rect] in the image view to a [Rect] in the original bitmap.
     */
    fun viewRectToOriginalBitmap(
        viewRect: Rect,
    ): IntRect = viewRectToBitmap(viewRect).let { bitmapRect ->
        bitmapCropRect?.let { bitmapCropRect ->
            bitmapRect.translate(bitmapCropRect.topLeft)
        } ?: bitmapRect
    }

    /**
     * Convert a point in the image view to a point in the original bitmap.
     */
    fun viewOffsetToOriginalBitmap(
        viewOffset: Offset,
    ): IntOffset = viewOffsetToBitmap(viewOffset).let { bitmapOffset ->
        bitmapCropRect?.let {
            bitmapOffset.plus(it.topLeft)
        } ?: bitmapOffset
    }

    /**
     * Convert the [bitmapCropRect] (measures relative to the bitmap) to a [Rect] with dimensions
     * compatible with the Compose view.
     */
    fun getViewCropRect(): Rect = bitmapCropRect?.let { bitmapCropRect ->
        bitmapRectToView(bitmapCropRect)
    } ?: imageViewRect

    /**
     * Convert an [IntOffset] relative to the bitmap to an [Offset] relative to the view.
     */
    private fun bitmapOffsetToView(offset: IntOffset): Offset = offset
        .toOffset()
        .times(bitmapToViewScaleFactor)
        .plus(imageViewRect.topLeft)

    /**
     * Convert an [Offset] relative to the view to an [IntOffset] relative to the bitmap.
     */
    private fun viewOffsetToBitmap(offset: Offset): IntOffset = offset
        .minus(imageViewRect.topLeft)
        .times(viewToBitmapScaleFactor)
        .round()

    /**
     * Convert a [Size] relative to the view to an [IntSize] relative to the bitmap.
     */
    private fun bitmapSizeToView(size: IntSize): Size = size
        .toSize()
        .times(bitmapToViewScaleFactor)

    /**
     * Convert an [IntSize] relative to the bitmap to a [Size] relative to the view.
     */
    private fun viewSizeToBitmap(size: Size): IntSize = size
        .times(viewToBitmapScaleFactor)
        .roundToIntSize()

    /**
     * Convert an [IntRect] relative to the bitmap to a [Rect] relative to the view.
     */
    private fun bitmapRectToView(rect: IntRect): Rect = rect
        .toRect()
        .times(bitmapToViewScaleFactor)
        .translate(imageViewRect.topLeft)

    /**
     * Convert a [Rect] relative to the view to an [IntRect] relative to the bitmap.
     */
    private fun viewRectToBitmap(rect: Rect): IntRect = rect
        .translate(-imageViewRect.topLeft)
        .times(viewToBitmapScaleFactor)
        .roundToIntRect()
}

/**
 * Custom image composable.
 *
 * @param imageBitmap The image data to display
 * @param cropRect The crop rectangle applied to the given data
 * @param modifier The [Modifier] to apply
 * @param onImageInformation Called when the image information is ready
 */
@Composable
fun CanvasImage(
    imageBitmap: ImageBitmap,
    cropRect: IntRect?,
    modifier: Modifier = Modifier,
    onImageInformation: (ImageInformation) -> Unit = {},
) {
    var positionInParent by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf<IntSize?>(null) }
    var intrinsicSize by remember { mutableStateOf<Size?>(null) }

    val imageViewRect = remember(positionInParent, containerSize, intrinsicSize) {
        val positionInParent = positionInParent ?: return@remember null
        val containerSize = containerSize?.toSize()?.takeIfValid() ?: return@remember null
        val intrinsicSize = intrinsicSize ?: return@remember null

        val scale = min(
            containerSize.width / intrinsicSize.width,
            containerSize.height / intrinsicSize.height,
        )

        val width = intrinsicSize.width * scale
        val height = intrinsicSize.height * scale
        val left = (containerSize.width - width) / 2
        val top = (containerSize.height - height) / 2

        Rect(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height
        ).translate(positionInParent)
    }

    LaunchedEffect(imageBitmap, cropRect, imageViewRect) {
        imageViewRect?.let { imageViewRect ->
            onImageInformation(
                ImageInformation(
                    bitmapSize = imageBitmap.size,
                    bitmapCropRect = cropRect,
                    imageViewRect = imageViewRect,
                )
            )
        }
    }

    val bitmapPainter = remember(imageBitmap) {
        BitmapPainter(imageBitmap).also {
            intrinsicSize = it.intrinsicSize
        }
    }

    with(LocalSharedTransitionScope.current) {
        Image(
            painter = bitmapPainter,
            contentDescription = null,
            modifier = modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState("canvas-image"),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                )
                .onGloballyPositioned {
                    positionInParent = it.positionInParent()
                    containerSize = it.size
                },
            contentScale = ContentScale.Fit,
        )
    }
}

private fun Size.takeIfValid() = takeIf { it != Size.Unspecified && it != Size.Zero }
