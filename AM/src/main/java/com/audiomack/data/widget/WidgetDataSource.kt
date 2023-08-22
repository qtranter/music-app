package com.audiomack.data.widget

import com.audiomack.ui.widget.AudiomackWidget

interface WidgetDataSource {
    fun updateRepostStatus(reposted: Boolean)
    fun updateFavoriteStatus(favorited: Boolean)
    fun alertNotLoggedIn(loggedIn: Boolean)
}

class WidgetRepository : WidgetDataSource {

    override fun updateRepostStatus(reposted: Boolean) {
        AudiomackWidget.updateWidgetRepost(reposted)
    }

    override fun updateFavoriteStatus(favorited: Boolean) {
        AudiomackWidget.updateWidgetFavorite(favorited)
    }

    override fun alertNotLoggedIn(loggedIn: Boolean) {
        AudiomackWidget.alertWidgetNotLoggedIn(loggedIn)
    }
}
