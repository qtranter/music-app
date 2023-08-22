package com.audiomack.model

import android.net.Uri
import com.audiomack.utils.setQueryParameter

data class NextPageData constructor(
    var url: String,
    var page: Int,
    var mixpanelSource: MixpanelSource = MixpanelSource.empty,
    var offlineScreen: Boolean = false
) {

    init {
        try {
            val urlPage = Uri.parse(url).getQueryParameter("page")
            urlPage?.let { page = it.toInt() }
        } catch (e: Exception) {
            // eat it
        }
    }

    fun getNextPageUrl(): String {
        page = page.inc()
        return Uri.parse(url)
            .setQueryParameter("page", page.toString())
            .toString()
            .also { url = it }
    }
}
