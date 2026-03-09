/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.navigation

import org.lineageos.canvas.models.EditMode

/**
 * App screen.
 */
sealed interface Screen {
    /**
     * Home screen.
     */
    data object Home : Screen

    /**
     * Edit screen.
     */
    data class Edit(val editMode: EditMode) : Screen
}
