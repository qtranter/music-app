package com.audiomack.ui.feed

import androidx.core.view.isVisible
import com.audiomack.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_header_timeline.view.switchReups

class TimeLineHeaderItem(
    private val isLoggedIn: Boolean,
    private val isExcludeReups: Boolean,
    private val onCheckedChangeListener: (Boolean) -> Unit
) : Item() {

    override fun getLayout(): Int {
        return R.layout.item_header_timeline
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        with(viewHolder.itemView.switchReups) {
            isVisible = isLoggedIn
            isChecked = isExcludeReups
            setOnCheckedChangeListener { _, isChecked ->
                onCheckedChangeListener(isChecked)
            }
        }
    }
}
