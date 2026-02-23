/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.lineageos.canvas.models.Action
import org.lineageos.canvas.models.Mode

class EditViewModel : ViewModel() {
    private val _actions = MutableStateFlow<List<Action>>(listOf())

    private val _undoActions = MutableStateFlow<List<Action>>(listOf())

    private val _pendingAction = MutableStateFlow<Action?>(null)

    private val _baseRect = MutableStateFlow<RectF?>(null)

    val canUndo: StateFlow<Boolean> = _actions.map { it.isNotEmpty() }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        false,
    )

    val canRedo: StateFlow<Boolean> = _undoActions.map { it.isNotEmpty() }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        false,
    )

    private val _mode = MutableStateFlow(Mode.RESIZE)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _showTextEditor = MutableStateFlow(false)
    val showTextEditor: StateFlow<Boolean> = _showTextEditor.asStateFlow()

    private val _textEditorPosition = MutableStateFlow<PointF?>(null)
    val textEditorPosition: StateFlow<PointF?> = _textEditorPosition.asStateFlow()

    private val _textEditorText = MutableStateFlow("")
    val textEditorText: StateFlow<String> = _textEditorText.asStateFlow()

    val cropRect: StateFlow<RectF?> = combine(
        _actions,
        _pendingAction,
        _baseRect,
    ) { actions, pending, baseRect ->
        if (pending is Action.Resize) {
            pending.rect
        } else {
            val lastResize = actions.lastOrNull { it is Action.Resize } as? Action.Resize
            lastResize?.rect ?: baseRect
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        null,
    )

    val actionsBitmap: StateFlow<Bitmap?> = combine(
        _actions,
        _baseRect,
    ) { actions, baseRect ->
        if (baseRect == null) return@combine null

        val bitmap = createBitmap(
            baseRect.width().toInt(),
            baseRect.height().toInt(),
            Bitmap.Config.ARGB_8888,
        )

        val canvas = Canvas(bitmap)
        actions.forEach { action ->
            action.drawInto(canvas)
        }

        bitmap
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        null,
    )

    fun setMode(mode: Mode) {
        _mode.value = mode
    }

    fun setBaseRect(rect: RectF) {
        _baseRect.value = rect
    }

    fun addAction(action: Action) {
        _undoActions.value = listOf()
        _actions.value += action
    }

    fun setPendingAction(action: Action?) {
        _pendingAction.value = action
    }

    fun commitPendingAction() {
        val pending = _pendingAction.value ?: return
        addAction(pending)
        _pendingAction.value = null
    }

    fun undo() {
        val lastAction = _actions.value.lastOrNull() ?: return
        _actions.value = _actions.value.dropLast(1)
        _undoActions.value += lastAction
    }

    fun redo() {
        val lastUndoAction = _undoActions.value.lastOrNull() ?: return
        _undoActions.value = _undoActions.value.dropLast(1)
        _actions.value += lastUndoAction
    }

    fun showTextEditor(position: PointF) {
        _textEditorPosition.value = position
        _textEditorText.value = ""
        _showTextEditor.value = true
    }

    fun dismissTextEditor() {
        _showTextEditor.value = false
        _textEditorPosition.value = null
        _textEditorText.value = ""
    }

    fun updateTextEditorText(text: String) {
        _textEditorText.value = text
    }
}
