package com.audiomack.utils.groupie

import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomack.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.item_carousel_suggested_accounts.recyclerViewCarousel

/**
 * A horizontally scrolling RecyclerView, for use in a vertically scrolling RecyclerView.
 */
class CarouselItem(
    private val carouselAdapter: GroupAdapter<com.xwray.groupie.GroupieViewHolder>
) : Item() {

    override fun getLayout() = R.layout.item_carousel_suggested_accounts

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.recyclerViewCarousel.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = carouselAdapter
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder) {
        viewHolder.recyclerViewCarousel.adapter = null
        super.unbind(viewHolder)
    }
}
