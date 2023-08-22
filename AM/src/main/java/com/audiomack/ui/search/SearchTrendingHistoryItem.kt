package com.audiomack.ui.search

import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem

sealed class SearchTrendingHistoryItem {
    object RecommendationsHeader : SearchTrendingHistoryItem()
    object RecentHeader : SearchTrendingHistoryItem()
    data class TrendingMusic(val music: AMResultItem) : SearchTrendingHistoryItem()
    data class TrendingArtist(val artist: AMArtist) : SearchTrendingHistoryItem()
    data class RecentSearch(val text: String) : SearchTrendingHistoryItem()
}
