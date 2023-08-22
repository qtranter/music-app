package com.audiomack.utils

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat

interface ItemTouchHelperAdapter {
    fun onItemMove(from: Int, to: Int)
    fun onItemDismiss(position: Int)
    fun onMoveComplete(start: Int, end: Int)
}

class ReorderRecyclerViewItemTouchHelper(
    private val mAdapter: ItemTouchHelperAdapter,
    private val deleteEnabled: Boolean = true,
    private val reorderEnabled: Boolean = true,
    private val deleteLastItemEnabled: Boolean = false
) : ItemTouchHelper.Callback() {

    private var moveStartPosition: Int? = null
    private var moveEndPosition: Int? = null

    override fun isLongPressDragEnabled() = reorderEnabled

    override fun isItemViewSwipeEnabled() = deleteEnabled

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val dragFlags = if (reorderEnabled) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
        val swipeFlags = if (deleteEnabled) {
            if (recyclerView.adapter!!.itemCount == 1 && !deleteLastItemEnabled) 0 else ItemTouchHelper.START or ItemTouchHelper.END
        } else 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        if (moveStartPosition == null) moveStartPosition = viewHolder.adapterPosition
        moveEndPosition = target.adapterPosition
        mAdapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        mAdapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            return
        }
        viewHolder.itemView.setBackgroundColor(viewHolder.itemView.context.colorCompat(if (isCurrentlyActive) R.color.queue_reorder_highlighted else R.color.queue_reorder_normal))
        viewHolder.itemView.alpha = 1.0f

        (recyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
            (layoutManager.findFirstVisibleItemPosition() until layoutManager.findLastVisibleItemPosition() + 1)
                .filter { it != viewHolder.adapterPosition }
                .mapNotNull { recyclerView.findViewHolderForLayoutPosition(it) }
                .forEach {
                    it.itemView.setBackgroundColor(viewHolder.itemView.context.colorCompat(R.color.queue_reorder_normal))
                    it.itemView.alpha = if (isCurrentlyActive) 0.3f else 1.0f
                }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        moveStartPosition?.let { start ->
            moveEndPosition?.let { end ->
                moveStartPosition = null
                moveEndPosition = null
                mAdapter.onMoveComplete(start, end)
            }
        }
    }
}
