/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Generic collection for history purpose, supporting undo/redo.
 */
class HistoryList<E>(collection: Collection<E>) {
    constructor() : this(emptyList())

    /**
     * Collection snapshot.
     */
    data class Snapshot<E>(
        val allElements: List<E>,
        val currentIndex: Int,
    ) {
        /**
         * The currently active elements.
         */
        val currentElements = allElements.subList(0, currentIndex + 1)

        /**
         * The reverted elements that can be redone.
         */
        val revertedElements = allElements.subList(currentIndex + 1, allElements.size)

        /**
         * Whether we can undo at least one element.
         */
        val canUndo = currentIndex >= 0

        /**
         * Whether we can go redo at least one element.
         */
        val canRedo = currentIndex < allElements.lastIndex
    }

    /**
     * All the elements.
     */
    private val allElements = collection.toMutableList()

    /**
     * Index of [allElements] pointing to the last active element.
     */
    private val currentIndex = MutableStateFlow(-1)

    /**
     * Transactions mutex.
     */
    private val mutex = Mutex()

    /**
     * Whether we can go back.
     */
    val canUndo: Boolean
        get() = currentIndex.value >= 0

    /**
     * Whether we can go forward.
     */
    val canRedo: Boolean
        get() = currentIndex.value < allElements.lastIndex

    private val _snapshot = MutableStateFlow(takeSnapshot())
    val snapshot = _snapshot.asStateFlow()

    /**
     * Insert the [element] after the current position, throwing away everything after the current
     * position.
     *
     * @param element The element to insert
     */
    fun insert(element: E) = transaction {
        revertedElementsView().clear()

        allElements.add(element)
        currentIndex.value += 1

        true
    }

    /**
     * Move back in the history.
     */
    fun undo() = transaction {
        val canUndo = canUndo
        if (canUndo) {
            currentIndex.value--
        }

        canUndo
    }

    /**
     * Move forward in the history.
     */
    fun redo() = transaction {
        val canRedo = canRedo
        if (canRedo) {
            currentIndex.value++
        }

        canRedo
    }

    private fun takeSnapshot() = Snapshot(
        allElements = allElements.toList(),
        currentIndex = currentIndex.value,
    )

    private fun transaction(block: () -> Boolean) = runBlocking {
        mutex.withLock {
            if (block()) {
                _snapshot.value = takeSnapshot()
            }
        }
    }

    private fun revertedElementsView() = allElements.subList(
        currentIndex.value + 1, allElements.size
    )
}
