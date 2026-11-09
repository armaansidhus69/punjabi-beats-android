package com.punjabibeats.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * Long-press drag-and-drop reordering for a LazyColumn (used by the queue
 * drawer). Attach [dragContainer] to the LazyColumn's modifier and
 * [draggableItem] to every item.
 */
class DragDropState internal constructor(
    private val listState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        internal set

    var draggingItemOffset by mutableFloatStateOf(0f)
        internal set

    internal fun onDragStart(touchY: Float) {
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            touchY >= item.offset && touchY <= item.offset + item.size
        }
        if (target != null) {
            draggingItemIndex = target.index
            draggingItemOffset = 0f
        }
    }

    internal fun onDrag(deltaY: Float) {
        val index = draggingItemIndex ?: return
        draggingItemOffset += deltaY
        val dragged = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index } ?: return
        val draggedCenter = dragged.offset + draggingItemOffset + dragged.size / 2f
        val hovered = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != index && draggedCenter >= item.offset && draggedCenter <= item.offset + item.size
        } ?: return
        onMove(index, hovered.index)
        draggingItemIndex = hovered.index
        draggingItemOffset = 0f
    }

    internal fun onDragEnd() {
        draggingItemIndex = null
        draggingItemOffset = 0f
    }
}

@Composable
fun rememberDragDropState(
    listState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): DragDropState {
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(listState) { DragDropState(listState) { from, to -> currentOnMove(from, to) } }
}

fun Modifier.dragContainer(state: DragDropState): Modifier = pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> state.onDragStart(offset.y) },
        onDrag = { change, dragAmount ->
            change.consume()
            state.onDrag(dragAmount.y)
        },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
    )
}

fun Modifier.draggableItem(state: DragDropState, index: Int): Modifier =
    if (state.draggingItemIndex == index) {
        this
            .zIndex(1f)
            .graphicsLayer { translationY = state.draggingItemOffset }
    } else {
        this
    }
