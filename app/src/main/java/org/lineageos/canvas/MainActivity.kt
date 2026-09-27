/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.util.Consumer
import org.lineageos.canvas.models.EditStatus
import org.lineageos.canvas.models.ImageFormat
import org.lineageos.canvas.ui.CanvasApp
import org.lineageos.canvas.ui.theme.CanvasTheme
import org.lineageos.canvas.viewmodels.EditViewModel
import org.lineageos.canvas.viewmodels.ResultOpsViewModel

class MainActivity : ComponentActivity() {
    // View models
    private val editViewModel: EditViewModel by viewModels()
    private val resultOpsViewModel: ResultOpsViewModel by viewModels()

    private val onNewIntentListener = Consumer<Intent> { intent ->
        val uri = intent.takeIf { it.action == Intent.ACTION_EDIT }?.data ?: run {
            finish()
            return@Consumer
        }

        val mimeType = contentResolver.getType(uri)
        val supportedMimeTypes = listOf(
            ImageFormat.JPEG.mimeType,
            ImageFormat.PNG.mimeType,
            ImageFormat.WEBP.mimeType,
        )

        if (supportedMimeTypes.none { it == mimeType }) {
            finish()
            return@Consumer
        }

        val isWritable = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

        editViewModel.setUri(uri, isWritable)
        resultOpsViewModel.setUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntentListener.accept(intent)
        addOnNewIntentListener(onNewIntentListener)

        enableEdgeToEdge()
        setContent {
            val saveStatus by resultOpsViewModel.saveStatus.collectAsState()
            val image by resultOpsViewModel.image.collectAsState()
            val finalBitmap by editViewModel.finalResultBitmap.collectAsState()

            LaunchedEffect(saveStatus) {
                when (val status = saveStatus) {
                    is EditStatus.Saved -> {
                        setResult(RESULT_OK)
                        finish()
                    }

                    is EditStatus.Shared -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = status.mimeType
                            putExtra(Intent.EXTRA_STREAM, status.uri)
                            clipData = ClipData.newUri(
                                contentResolver,
                                getString(R.string.app_name),
                                status.uri,
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                    }

                    else -> {}
                }
            }

            val onDocumentCreated: (Uri?, ImageFormat) -> Unit = { uri, format ->
                uri?.let { uri ->
                    editViewModel.finalResultBitmap.value?.let {
                        resultOpsViewModel.saveImageToUri(it, uri, format)
                    }
                }
            }
            val createJpegDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(ImageFormat.JPEG.mimeType),
            ) { uri -> onDocumentCreated(uri, ImageFormat.JPEG) }
            val createPngDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(ImageFormat.PNG.mimeType),
            ) { uri -> onDocumentCreated(uri, ImageFormat.PNG) }
            val createWebpDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(ImageFormat.WEBP.mimeType),
            ) { uri -> onDocumentCreated(uri, ImageFormat.WEBP) }

            CanvasTheme {
                CanvasApp(
                    editViewModel = editViewModel,
                    onClose = {
                        setResult(RESULT_CANCELED, null)
                        finish()
                    },
                    onSave = {
                        finalBitmap?.let {
                            resultOpsViewModel.saveImage(it)
                        }
                    },
                    onSaveAs = {
                        image?.format?.let {
                            val fileName = resultOpsViewModel.suggestedFilename(it)
                            when (it) {
                                ImageFormat.JPEG -> createJpegDocumentLauncher.launch(fileName)
                                ImageFormat.PNG -> createPngDocumentLauncher.launch(fileName)
                                ImageFormat.WEBP -> createWebpDocumentLauncher.launch(fileName)
                            }
                        }
                    },
                    onShare = {
                        finalBitmap?.let {
                            resultOpsViewModel.shareImage(it)
                        }
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        removeOnNewIntentListener(onNewIntentListener)

        super.onDestroy()
    }
}
