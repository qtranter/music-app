package com.audiomack.data.search

import com.audiomack.MainApplication
import com.audiomack.SEARCH_PREFERENCES
import com.audiomack.SEARCH_PREFERENCES_RECENT
import com.audiomack.data.search.filters.SearchFilters
import com.audiomack.model.AMResultItem
import com.audiomack.network.API
import com.audiomack.network.APIInterface
import com.audiomack.utils.SecureSharedPreferences
import io.reactivex.Observable
import io.reactivex.Single
import org.json.JSONArray

class SearchRepository(
    private val api: APIInterface.SearchInterface = API.getInstance()
) : SearchDataSource {

    private val preferences = SecureSharedPreferences(MainApplication.context, SEARCH_PREFERENCES)
    private val limit = 20

    override fun getRecentSearches(): List<String> {
        val string = preferences.getString(SEARCH_PREFERENCES_RECENT)
        if (!string.isNullOrEmpty()) {
            val array = JSONArray(string)
            return (0 until array.length()).mapNotNull { array.optString(it, null) }
        }
        return emptyList()
    }

    override fun removeRecentSearch(query: String) {
        val searches = getRecentSearches().filter { it != query }
        setRecentSearches(searches)
    }

    override fun addRecentSearch(query: String) {
        val searches = getRecentSearches().filter { it != query }.toMutableList()
        searches.add(0, query)
        setRecentSearches(searches.take(limit))
    }

    private fun setRecentSearches(strings: List<String>) {
        val array = JSONArray()
        strings.forEach { array.put(it) }
        preferences.put(SEARCH_PREFERENCES_RECENT, array.toString())
    }

    override fun autosuggest(query: String): Observable<List<String>> {
        return api.searchAutoSuggest(query)
    }

    override fun getRecommendations(): Single<List<AMResultItem>> {
        return api.getRecommendations()
    }

    override var verifiedOnly: Boolean
        get() = SearchFilters.verifiedOnly
        set(value) {
            SearchFilters.verifiedOnly = value
        }

    override var categoryCode: String
        get() = SearchFilters.categoryCode
        set(value) {
            SearchFilters.categoryCode = value
        }

    override val categoryName: String = SearchFilters.categoryName()

    override val categoryCodes: List<String> = SearchFilters.categoryCodes

    override var genreCode: String?
        get() = SearchFilters.genreCode
        set(value) {
            SearchFilters.genreCode = value
        }

    override var query: String?
        get() = SearchFilters.query
        set(value) {
            SearchFilters.query = value
        }
}
