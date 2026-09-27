/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.canvas.models.EditStatus
import org.lineageos.canvas.models.ImageFormat
import org.lineageos.canvas.models.Image
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * View model used by the activity to do actions with the final bitmap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultOpsViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Application's [ContentResolver].
     */
    private val contentResolver = application.contentResolver

    /**
     * The source URI of the image.
     */
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri = _uri.asStateFlow()

    /**
     * The status of the save operation.
     */
    private val _editStatus = MutableStateFlow<EditStatus>(EditStatus.Idle)
    val saveStatus = _editStatus.asStateFlow()

    val image = uri
        .filterNotNull()
        .mapLatest {
            // We know this is always non-null because we filter it
            val mimeType = contentResolver.getType(it)!!
            Image(
                uri = it,
                mimeType = mimeType,
                format = ImageFormat.fromMimeType(mimeType),
            )
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    fun suggestedFilename(format: ImageFormat): String {
        val fileName = uri.value?.lastPathSegment ?: "image"
        val baseName = fileName.substringBeforeLast(".")
        return "${baseName}_edit.${format.extension}"
    }

    /**
     * Set the URI of the image.
     *
     * @param uri the URI of the image
     */
    fun setUri(uri: Uri) {
        _uri.value = uri
    }

    fun saveImage(bitmap: ImageBitmap) {
        viewModelScope.launch {
            val image = image.filterNotNull().first()
            saveImageToUri(bitmap, image.uri, image.format)
        }
    }

    fun saveImageToUri(bitmap: ImageBitmap, targetUri: Uri, format: ImageFormat) {
        viewModelScope.launch {
            _editStatus.value = EditStatus.Saving

            _editStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    val outputStream = contentResolver.openOutputStream(targetUri, "wt")
                        ?: throw IOException("Unable to open output stream")

                    outputStream.use {
                        if (!bitmap.asAndroidBitmap().compress(format.compressFormat, 100, it)) {
                            throw IOException("Image compression failed")
                        }
                    }
                }.fold(
                    onSuccess = {
                        EditStatus.Saved
                    },
                    onFailure = {
                        EditStatus.Error("Failed")
                    }
                )
            }
        }
    }

    fun shareImage(bitmap: ImageBitmap) {
        val context = getApplication<Application>()

        viewModelScope.launch {
            _editStatus.value = EditStatus.Sharing

            _editStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    val image = image.filterNotNull().first()

                    val imagesDir = File(context.filesDir, "images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()

                    val file = File(imagesDir, "share_temp.${image.format.extension}")

                    FileOutputStream(file).use { out ->
                        if (!bitmap.asAndroidBitmap()
                                .compress(image.format.compressFormat, 100, out)
                        ) {
                            throw IOException("Image compression failed")
                        }
                    }

                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    ) to image.mimeType
                }.fold(
                    onSuccess = { (uri, mimeType) ->
                        EditStatus.Shared(uri, mimeType)
                    },
                    onFailure = {
                        EditStatus.Error("Failed")
                    }
                )
            }
        }
    }
}
