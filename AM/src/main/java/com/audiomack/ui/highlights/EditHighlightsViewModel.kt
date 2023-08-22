package com.audiomack.ui.highlights

import androidx.lifecycle.MutableLiveData
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.MusicRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent

class EditHighlightsViewModel(
    private val musicDataSource: MusicDataSource = MusicRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val close = SingleLiveEvent<Void>()
    val saveResult = SingleLiveEvent<EditHighlightsStatus>()
    val loadingStatus = MutableLiveData<Boolean>()
    val highlightsReady = MutableLiveData<List<AMResultItem>>()

    fun onCloseTapped() {
        close.call()
    }

    fun onSaveTapped(items: List<AMResultItem>) {
        saveResult.postValue(EditHighlightsStatus.InProgress)
        compositeDisposable.add(
            musicDataSource.reorderHighlights(items)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    saveResult.postValue(EditHighlightsStatus.Succeeded)
                }, {
                    saveResult.postValue(EditHighlightsStatus.Failed)
                })
        )
    }

    fun onHighlightsRequested() {
        loadingStatus.postValue(true)
        compositeDisposable.add(
            musicDataSource.getHighlights(userDataSource.getUserSlug() ?: "", true)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({
                    highlightsReady.postValue(it)
                    loadingStatus.postValue(false)
                }, {
                    highlightsReady.postValue(emptyList())
                    loadingStatus.postValue(false)
                })
        )
    }
}
