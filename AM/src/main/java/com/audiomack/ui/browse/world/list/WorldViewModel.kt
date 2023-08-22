package com.audiomack.ui.browse.world.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.rxjava2.cachedIn
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.reachability.Reachability
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.world.WorldDataSource
import com.audiomack.data.world.WorldRepository
import com.audiomack.model.WorldArticle
import com.audiomack.model.WorldPage
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import com.audiomack.utils.addTo
import io.reactivex.Flowable

class WorldViewModel(
    private val repository: WorldDataSource = WorldRepository(),
    private val reachability: ReachabilityDataSource = Reachability.getInstance(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    sealed class ViewState {
        object LoadingPages : ViewState()
        data class LoadedPages(val filterItems: List<WorldFilterItem>) : ViewState()
        object Error : ViewState()
    }

    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> get() = _viewState

    private val _adsEnabled = MutableLiveData(adsDataSource.adsVisible)
    val adsEnabled: LiveData<Boolean> get() = _adsEnabled

    val openPostDetailEvent = SingleLiveEvent<String>()
    val setupPostsEvent = SingleLiveEvent<PagingData<WorldArticle>>()
    val showOfflineToastEvent = SingleLiveEvent<Void>()

    private var currentArticlesResult: Flowable<PagingData<WorldArticle>>? = null
    private var currentPageLoading: WorldPage? = null
    private var pages: List<WorldPage> = listOf()
    private var selectedPage: WorldPage = WorldPage.all
    private var allPage: WorldPage = WorldPage.all

    private val filterItems: List<WorldFilterItem>
        get() {
            val allPagesFilterItems = pages.map { WorldFilterItem(it, it.slug == selectedPage.slug) }.toMutableList()
            val selectedPageFilterItem = if (!pages.any { it.slug == selectedPage.slug }) WorldFilterItem(selectedPage, true) else null
            selectedPageFilterItem?.let {
                allPagesFilterItems.add(it)
            }
            return allPagesFilterItems.toList()
        }

    fun onPageRequested(page: WorldPage) {
        selectedPage = page

        if (pages.isNotEmpty()) {
            _viewState.postValue(ViewState.LoadedPages(filterItems))
            getPosts(selectedPage)
        } else {
            if (selectedPage != allPage) {
                // Navigated a deeplink with a specific page/tag
                // Show that page first, then load the other pages as well
                _viewState.postValue(ViewState.LoadedPages(filterItems))
                fetchPages()
            } else {
                _viewState.postValue(ViewState.LoadingPages)
                fetchPages()
            }
        }
    }

    fun onSlugRequested(slug: String) {
        openPostDetailEvent.postValue(slug)
    }

    fun handleError() {
        if (!reachability.networkAvailable) {
            showOfflineToastEvent.call()
        }
        _viewState.postValue(ViewState.Error)
    }

    private fun fetchPages() {
        repository.getPages()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({ res ->
                // Always add Home as the first tab, optionally add selectedPage at the end of the list in case it's a new one
                pages = res.toMutableList().apply {
                    add(0, allPage)
                    if (!res.any { it.slug == selectedPage.slug }) {
                        if (selectedPage.slug != allPage.slug) {
                            add(selectedPage)
                        }
                    }
                }
                _viewState.postValue(ViewState.LoadedPages(filterItems))
                getPosts(selectedPage)
            }, {
                _viewState.postValue(ViewState.Error)
            })
            .addTo(compositeDisposable)
    }

    private fun getPosts(page: WorldPage) {
        getWorldPosts(page).subscribe {
            setupPostsEvent.postValue(it)
        }.addTo(compositeDisposable)
    }

    private fun getWorldPosts(page: WorldPage): Flowable<PagingData<WorldArticle>> {
        val lastResult = currentArticlesResult
        if (page == currentPageLoading && lastResult != null) {
            return lastResult
        }
        currentPageLoading = page

        val newResult: Flowable<PagingData<WorldArticle>> =
            repository.getPostsStream(page).cachedIn(viewModelScope)

        currentArticlesResult = newResult
        return newResult
    }
}
