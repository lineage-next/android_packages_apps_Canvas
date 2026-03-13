/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas

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

        val isWritable = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

        editViewModel.setUri(uri, isWritable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntentListener.accept(intent)
        addOnNewIntentListener(onNewIntentListener)

        enableEdgeToEdge()
        setContent {
            val saveStatus by resultOpsViewModel.saveStatus.collectAsState()
            val mimeType by resultOpsViewModel.mimeType.collectAsState()

            LaunchedEffect(saveStatus) {
                when (val status = saveStatus) {
                    is EditStatus.Saved -> {
                        setResult(RESULT_OK)
                        finish()
                    }

                    is EditStatus.Shared -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = mimeType ?: "image/png"
                            putExtra(Intent.EXTRA_STREAM, status.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, null))
                    }

                    else -> {}
                }
            }

            val createDocumentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(mimeType ?: "image/png")
            ) { uri: Uri? ->
                uri?.let { uri ->
                    editViewModel.finalResultBitmap.value?.let {
                        resultOpsViewModel.saveImageToUri(it, uri)
                    }
                }
            }

            CanvasTheme {
                CanvasApp(
                    editViewModel = editViewModel,
                    onClose = {
                        setResult(RESULT_CANCELED, null)
                        finish()
                    },
                    onSave = {
                        editViewModel.finalResultBitmap.value?.let {
                            resultOpsViewModel.saveImage(it)
                        }
                    },
                    onSaveAs = {
                        createDocumentLauncher.launch(resultOpsViewModel.suggestedFilename)
                    },
                    onShare = {
                        editViewModel.finalResultBitmap.value?.let {
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
