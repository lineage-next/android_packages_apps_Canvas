/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UriViewModel : ViewModel() {
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri: StateFlow<Uri?> = _uri.asStateFlow()

    private val _isWritable = MutableStateFlow(false)
    val isWritable: StateFlow<Boolean> = _isWritable.asStateFlow()

    fun setUri(uri: Uri?, isWritable: Boolean) {
        _uri.value = uri
        _isWritable.value = isWritable
    }
}
