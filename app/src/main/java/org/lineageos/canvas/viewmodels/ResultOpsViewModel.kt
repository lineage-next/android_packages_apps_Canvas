/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.canvas.models.EditStatus
import java.io.File
import java.io.FileOutputStream

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
    private val uri = MutableStateFlow<Uri?>(null)

    /**
     * The status of the save operation.
     */
    private val _editStatus = MutableStateFlow<EditStatus>(EditStatus.Idle)
    val saveStatus = _editStatus.asStateFlow()

    val suggestedFilename: String
        get() {
            val currentUri = uri.value ?: return "image_edit"
            val fileName = currentUri.lastPathSegment ?: "image"
            val baseName = fileName.substringBeforeLast(".")
            return "${baseName}_edit"
        }

    /**
     * The MIME Type of the image.
     */
    val mimeType = uri
        .mapLatest { currentUri ->
            if (currentUri == null) return@mapLatest null
            contentResolver.getType(currentUri)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    /**
     * Set the URI of the image.
     *
     * @param uri the URI of the image
     */
    fun setUri(uri: Uri) {
        this.uri.value = uri
    }

    fun saveImage(bitmap: ImageBitmap) {
        val uri = uri.value ?: return
        saveImageToUri(bitmap, uri)
    }

    fun saveImageToUri(bitmap: ImageBitmap, targetUri: Uri) {
        val mimeType = mimeType.value ?: return

        viewModelScope.launch {
            _editStatus.value = EditStatus.Saving

            _editStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    val format = when (mimeType) {
                        "image/jpeg" -> Bitmap.CompressFormat.JPEG
                        "image/png" -> Bitmap.CompressFormat.PNG
                        "image/webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
                        else -> Bitmap.CompressFormat.JPEG
                    }

                    contentResolver.openOutputStream(targetUri, "wt")?.use {
                        bitmap.asAndroidBitmap().compress(format, 100, it)
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
                    val imagesDir = File(context.filesDir, "images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()

                    val file = File(imagesDir, "share_temp.png")

                    FileOutputStream(file).use { out ->
                        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }.fold(
                    onSuccess = {
                        EditStatus.Shared(it)
                    },
                    onFailure = {
                        EditStatus.Error("Failed")
                    }
                )
            }
        }
    }
}
