package com.audiomack.utils.groupie

import com.audiomack.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item

/**
 * A loading item for vertical recyclerView.
 */
class LoadingItem(
    private val loadingType: LoadingItemType = LoadingItemType.LIST,
    private val loadMore: () -> Unit
) : Item() {

    override fun getLayout() =
        if (loadingType == LoadingItemType.LIST) R.layout.row_loadingmore else R.layout.item_account_loading

    override fun bind(viewHolder: GroupieViewHolder, position: Int) = loadMore()

    enum class LoadingItemType {
        LIST, GRID
    }
}
