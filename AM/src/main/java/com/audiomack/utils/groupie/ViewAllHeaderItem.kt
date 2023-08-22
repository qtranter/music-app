package com.audiomack.utils.groupie

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.audiomack.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_header_view_all.view.downDivider
import kotlinx.android.synthetic.main.item_header_view_all.view.tvTitle
import kotlinx.android.synthetic.main.item_header_view_all.view.tvViewAll
import kotlinx.android.synthetic.main.item_header_view_all.view.upDivider

class ViewAllHeaderItem(
    @StringRes private val title: Int,
    private val onViewAllClick: () -> Unit,
    private val isUpDividerVisible: Boolean = false,
    private val isDownDividerVisible: Boolean = false
) : Item() {

    override fun getLayout() = R.layout.item_header_view_all

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        with(viewHolder.itemView) {
            tvTitle.text = context.getString(title)
            tvViewAll?.setOnClickListener {
                onViewAllClick()
            }
            upDivider.isVisible = isUpDividerVisible
            downDivider.isVisible = isDownDividerVisible
        }
    }
}
