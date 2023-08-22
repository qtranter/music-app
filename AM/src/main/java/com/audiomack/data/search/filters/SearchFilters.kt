package com.audiomack.data.search.filters

import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelFilterVerified_All
import com.audiomack.data.tracking.mixpanel.MixpanelFilterVerified_Verified
import com.audiomack.model.AMGenre

object SearchFilters {

    private val POPULAR = "popular"
    private val RECENT = "recent"
    private val RELEVANT = "relevance"

    val categoryCodes: List<String> = listOf(
        POPULAR,
        RECENT,
        RELEVANT
    )

    private val categoryNames: List<String> = listOf(
        MainApplication.context!!.getString(R.string.search_filter_popular),
        MainApplication.context!!.getString(R.string.search_filter_recent),
        MainApplication.context!!.getString(R.string.search_filter_relevant)
    )

    var verifiedOnly = false

    var categoryCode = POPULAR

    var genreCode: String? = null

    var query: String? = null
    set(value) {
        val oldValue = field
        field = value
        if (value != oldValue) {
            categoryCode = POPULAR
            verifiedOnly = false
            genreCode = null
        }
    }

    fun categoryName(): String {
        val index = categoryCodes.indexOfFirst { it == categoryCode }
        if (index != -1) {
            return categoryNames[index].toUpperCase()
        }
        return ""
    }

    fun humanDescription(): String = "${categoryName()}${if (genreName().isNullOrEmpty()) "" else " - ${genreName()}"}"

    private fun genreName(): String? {
        val genre = AMGenre.fromApiValue(genreCode)
        return if (genre == AMGenre.Other || genre == AMGenre.All) {
            null
        } else {
            genre.humanValue(MainApplication.context!!).toUpperCase()
        }
    }

    fun mixpanelGenreName(): String {
        val genre = AMGenre.fromApiValue(genreCode)
        return if (genre == AMGenre.Other || genre == AMGenre.All) {
            "all"
        } else {
            genre.apiValue()
        }
    }

    fun mixpanelSortName(): String {
        return when (categoryCode) {
            POPULAR -> "Most Popular"
            RECENT -> "Most Recent"
            else -> "Most Relevant"
        }
    }

    fun mixpanelVerifiedName(): String {
        return if (verifiedOnly) MixpanelFilterVerified_Verified else MixpanelFilterVerified_All
    }
}
