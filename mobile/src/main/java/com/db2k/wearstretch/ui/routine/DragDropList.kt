package com.db2k.wearstretch.ui.routine

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Lightweight drag-and-drop reordering state for a [androidx.compose.foundation.lazy.LazyColumn].
 * Long-press an item to pick it up, drag to reorder; [onMove] is invoked as the dragged item
 * crosses its neighbours.
 */
@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): DragDropListState {
    return remember { DragDropListState(lazyListState, onMove) }
}

class DragDropListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    private var draggedDistance by mutableStateOf(0f)
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    private val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem?.let { index ->
            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }

    /** Vertical pixel offset to apply to the item currently being dragged. */
    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem?.let { index ->
            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }?.let { item ->
            (initiallyDraggedElement?.offset ?: 0).toFloat() + draggedDistance - item.offset
        }

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also {
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
            }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        val initial = initiallyDraggedElement ?: return
        val startOffset = initial.offset + draggedDistance
        val endOffset = initial.offset + initial.size + draggedDistance
        val hovered = currentElement ?: return

        lazyListState.layoutInfo.visibleItemsInfo
            .filterNot { item -> item.offset + item.size < startOffset || item.offset > endOffset || hovered.index == item.index }
            .firstOrNull { item ->
                val delta = startOffset - hovered.offset
                if (delta > 0) endOffset > (item.offset + item.size) else startOffset < item.offset
            }
            ?.also { item ->
                currentIndexOfDraggedItem?.let { current -> onMove(current, item.index) }
                currentIndexOfDraggedItem = item.index
            }
    }

    /** Pixels the list should auto-scroll by when the dragged item is pushed past an edge. */
    fun checkForOverScroll(): Float {
        return initiallyDraggedElement?.let {
            val startOffset = it.offset + draggedDistance
            val endOffset = it.offset + it.size + draggedDistance
            when {
                draggedDistance > 0 -> (endOffset - lazyListState.layoutInfo.viewportEndOffset).takeIf { diff -> diff > 0 }
                draggedDistance < 0 -> (startOffset - lazyListState.layoutInfo.viewportStartOffset).takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }
}
