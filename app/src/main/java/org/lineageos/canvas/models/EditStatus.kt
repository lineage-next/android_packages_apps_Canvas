/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.net.Uri

sealed class EditStatus {
    // No operation in progress
    object Idle : EditStatus()

    // Operation in progress
    object Saving : EditStatus()
    object Sharing : EditStatus()

    // Operation completed
    object Saved : EditStatus()
    data class Shared(val uri: Uri) : EditStatus()
    data class Error(val message: String) : EditStatus()
}
