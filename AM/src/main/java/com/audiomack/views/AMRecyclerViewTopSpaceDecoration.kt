package com.audiomack.views

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class AMRecyclerViewTopSpaceDecoration(private val size: Int) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: androidx.recyclerview.widget.RecyclerView,
        state: androidx.recyclerview.widget.RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top += size
        }
    }
}
