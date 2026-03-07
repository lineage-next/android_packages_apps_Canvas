/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.util.Consumer
import org.lineageos.canvas.ui.CanvasApp
import org.lineageos.canvas.ui.theme.CanvasTheme
import org.lineageos.canvas.viewmodels.UriViewModel

class MainActivity : ComponentActivity() {
    private val uriViewModel: UriViewModel by viewModels()

    private val onNewIntentListener = Consumer<Intent> { intent ->
        val uri = intent.takeIf { it.action == Intent.ACTION_EDIT }?.data ?: run {
            finish()
            return@Consumer
        }

        val isWritable = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

        uriViewModel.setUri(uri, isWritable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onNewIntentListener.accept(intent)
        addOnNewIntentListener(onNewIntentListener)

        enableEdgeToEdge()
        setContent {
            CanvasTheme {
                CanvasApp(
                    uriViewModel = uriViewModel,
                    onSave = {
                        val result = Intent().apply {
                            data = it
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, it)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        startActivity(Intent.createChooser(intent, null))
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
