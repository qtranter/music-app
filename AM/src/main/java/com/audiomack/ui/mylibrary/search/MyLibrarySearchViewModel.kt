package com.audiomack.ui.mylibrary.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.database.ArtistDAO
import com.audiomack.data.database.ArtistDAOImpl
import com.audiomack.model.ArtistWithBadge
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class MyLibrarySearchViewModel(
    artistDAO: ArtistDAO = ArtistDAOImpl(),
    schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    private var _artistName = MutableLiveData<ArtistWithBadge>()
    val artistName: LiveData<ArtistWithBadge> get() = _artistName

    private var _clearSearchVisible = MutableLiveData(false)
    val clearSearchVisible: LiveData<Boolean> get() = _clearSearchVisible

    private var _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> get() = _searchQuery

    val closeEvent = SingleLiveEvent<Void>()
    val clearSearchbarEvent = SingleLiveEvent<Void>()
    val showKeyboardEvent = SingleLiveEvent<Void>()
    val hideKeyboardEvent = SingleLiveEvent<Void>()

    var query: String? = null

    init {
        artistDAO.find()
            .subscribeOn(schedulersProvider.io)
            .observeOn(schedulersProvider.main)
            .subscribe({
                _artistName.postValue(ArtistWithBadge(it.name ?: "", it.isVerified, it.isTastemaker, it.isAuthenticated))
            }, {})
            .also { compositeDisposable.add(it) }

        clearSearchbarEvent.call()
    }

    override fun onCleared() {
        hideKeyboardEvent.call()
        super.onCleared()
    }

    fun onBackTapped() {
        closeEvent.call()
    }

    fun onCancelTapped() {
        closeEvent.call()
    }

    fun onClearTapped() {
        clearSearchbarEvent.call()
        showKeyboardEvent.call()
    }

    fun onSearchClicked(text: String) {
        if (text.isNotBlank()) {
            query = text.trim()
            _searchQuery.postValue(query)
        }
        hideKeyboardEvent.call()
    }

    fun onSearchTextChanged(text: String) {
        _clearSearchVisible.value = text.isNotEmpty()
    }
}
