package com.audiomack.data.removedcontent

import com.audiomack.model.RemovedContentItem

interface RemovedContentDataSource {

    fun getItems(): List<RemovedContentItem>

    fun addItem(item: RemovedContentItem)

    fun clearItems()
}
