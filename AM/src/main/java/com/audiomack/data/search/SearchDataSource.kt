package com.audiomack.data.search

import com.audiomack.model.AMResultItem
import io.reactivex.Observable
import io.reactivex.Single

interface SearchDataSource {

    fun getRecentSearches(): List<String>

    fun removeRecentSearch(query: String)

    fun addRecentSearch(query: String)

    fun autosuggest(query: String): Observable<List<String>>

    fun getRecommendations(): Single<List<AMResultItem>>

    var verifiedOnly: Boolean

    var categoryCode: String

    val categoryName: String

    val categoryCodes: List<String>

    var genreCode: String?

    var query: String?
}
