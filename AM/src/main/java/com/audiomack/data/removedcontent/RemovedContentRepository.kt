package com.audiomack.data.removedcontent

import com.audiomack.model.RemovedContentItem

object RemovedContentRepository : RemovedContentDataSource {

    val list: MutableList<RemovedContentItem> = mutableListOf()

    override fun getItems(): List<RemovedContentItem> {
        return list
    }

    override fun addItem(item: RemovedContentItem) {
        list.add(item)
    }

    override fun clearItems() {
        list.clear()
    }
}
