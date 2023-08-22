package com.audiomack.ui.feed

import com.audiomack.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item

class TimeLinePlaceHolderItem : Item() {

    override fun getLayout() = R.layout.layout_feed_placeholder

    override fun bind(viewHolder: GroupieViewHolder, position: Int) = Unit
}
