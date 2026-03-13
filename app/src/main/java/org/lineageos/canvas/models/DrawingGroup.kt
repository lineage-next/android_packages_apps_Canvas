/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

/**
 * A run of [Action.Drawing]s that were authored while the image was at [rotationBefore]
 * degrees of cumulative clockwise rotation.
 *
 * @param rotationBefore cumulative rotation in [0, 360) at the time these drawings were added
 * @param widthBefore the width of the image at the time these drawings were added
 * @param heightBefore the height of the image at the time these drawings were added
 * @param drawings the drawings in insertion order
 */
data class DrawingGroup(
    val rotationBefore: Float,
    val widthBefore: Int,
    val heightBefore: Int,
    val drawings: List<Action.Drawing>,
)
